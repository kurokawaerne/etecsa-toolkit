package dev.kurokawa.etecsa.toolkit.ui

import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.outlined.*
import com.composables.icons.materialsymbols.outlinedfilled.*

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.kurokawa.etecsa.toolkit.core.*
import dev.kurokawa.etecsa.toolkit.i18n.*
import dev.kurokawa.etecsa.toolkit.model.*
import dev.kurokawa.etecsa.toolkit.theme.EtecsaToolkitTheme
import kotlinx.coroutines.delay

/** App-wide UI state shared by screens. */
class AppState(
    val bridge: DeviceBridge, val store: LocalStore,
    var lang: String, var slot: Int,
) {
    var strings by mutableStateOf(stringsFor(lang))
    var screen by mutableStateOf("home")
    var ussdResult by mutableStateOf<String?>(null)
    var ussdBusy by mutableStateOf(false)
    var balance by mutableStateOf<ParsedResponse?>(null)

    fun applyLang(l: String) {
        lang = l; store.put("lang", l); strings = stringsFor(l)
    }
    fun runUssd(code: String, title: String = code) {
        ussdBusy = true
        bridge.runUssd(code, slot) { reply ->
            ussdBusy = false; ussdResult = reply
            store.addHistory("ussd", title, reply, slot)
            val parsed = ResponseParser.parse(reply)
            if (parsed.kind == ParsedResponse.Kind.BALANCE) {
                balance = parsed
                store.put("last_consult_ms", nowMillis().toString())
            }
            // Feed the home-screen widgets (multi-line widget reads these)
            parsed.balanceCup?.let { store.put("widget_saldo", "$it CUP") }
            parsed.dataBytes?.let { store.put("widget_datos", ResponseParser.formatData(it)) }
            parsed.voiceMinutes?.let { store.put("widget_voz", "$it min") }
            parsed.smsCount?.let { store.put("widget_sms", "$it SMS") }
            // Plan/balance expiry reminder: arm a day-before notification
            parsed.expires?.let { exp ->
                ResponseParser.expiryReminderMillis(exp, nowMillis())?.let { at ->
                    bridge.scheduleReminder("Vencimiento cercano",
                        "Tu saldo o plan vence el $exp", at)
                }
            }
        }
    }
    fun back() { screen = "home" }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ToolkitApp() {
    val bridge = remember { createBridge() }
    val store = remember { createStore() }
    val savedLang = remember { store.get("lang") ?: "system" }
    val state = remember {
        AppState(bridge, store,
            lang = if (savedLang == "system") "es" else savedLang,
            slot = store.get("sim_slot")?.toIntOrNull() ?: 0)
    }
    var dark by remember { mutableStateOf(store.get("dark") == "1") }
    var splashDone by remember { mutableStateOf(false) }

    EtecsaToolkitTheme(darkMode = dark) {
        Surface(Modifier.fillMaxSize()) {
            if (!splashDone) {
                Splash { splashDone = true }
            } else {
                MainShell(state, dark) { dark = it; store.put("dark", if (it) "1" else "0") }
            }
        }
        // USSD result dialog (operator reply), shared by every screen
        state.ussdResult?.let { reply ->
            AlertDialog(
                onDismissRequest = { state.ussdResult = null },
                title = { Text(state.strings.result) },
                text = {
                    Column {
                        Text(reply)
                        val p = remember(reply) { ResponseParser.parse(reply) }
                        if (p.kind == ParsedResponse.Kind.BALANCE) {
                            Spacer(Modifier.height(12.dp))
                            p.balanceCup?.let { Text("${state.strings.balance}: $it CUP", fontWeight = FontWeight.Bold) }
                            p.dataBytes?.let { Text("Datos: ${ResponseParser.formatData(it)}") }
                            p.voiceMinutes?.let { Text("Voz: $it min") }
                            p.smsCount?.let { Text("SMS: $it") }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { state.ussdResult = null }) { Text(state.strings.close) } },
            )
        }
        if (state.ussdBusy) {
            AlertDialog(onDismissRequest = {}, title = { Text(state.strings.consulting) },
                text = { Row(verticalAlignment = Alignment.CenterVertically) { LoadingIndicator(); Spacer(Modifier.width(16.dp)); Text(state.ussdResult ?: "") } },
                confirmButton = {})
        }
    }
}

/** Animated splash: the gradient app mark scales/springs in, wordmark fades,
 *  then we hand over to the shell (single splash — no double splash). */
@Composable
private fun Splash(onDone: () -> Unit) {
    val scale = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, tween(700))
        delay(650); onDone()
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            EtkMark(Modifier.scale(scale.value).size(110.dp), big = true)
            Spacer(Modifier.height(20.dp))
            Text("ETECSA Toolkit", style = MaterialTheme.typography.headlineMedium)
            Text("Material 3 Expressive", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MainShell(state: AppState, dark: Boolean, onDark: (Boolean) -> Unit) {
    val s = state.strings
    // The original's bottom navigation (RoCla MainActivity, tag
    // rocla_bottom_navigation_static_rc6): Inicio · Llamadas · Mensajes ·
    // Planes. Sub-screens highlight the tab they belong to; everything
    // reached from the Home service list stays on Inicio.
    val tab = when (state.screen) {
        "calls", "call", "callblocker" -> 1
        "messages", "smsc" -> 2
        "plans" -> 3
        else -> 0
    }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            // v1.2 shell: NO global header. Each tab is its own page, like
            // the original's activities — Home carries its own top row,
            // Calls/Messages their own, feature screens their title bar.
            Box(Modifier.weight(1f)) {
                when (state.screen) {
                    "home" -> HomeScreen(state)
                    "messages" -> MessagesScreen(state)
                    "calls" -> CallsScreen(state)
                    "network" -> NetworkScreen(state)
                    "settings" -> SettingsScreen(state, dark, onDark)
                    "history" -> HistoryScreen(state)
                    "search" -> SearchScreen(state)
                    "plans" -> PlansScreen(state)
                    "transfer" -> TransferScreen(state)
                    "friends" -> FriendsScreen(state)
                    "advance" -> AdvanceScreen(state)
                    "cards" -> CardsScreen(state)
                    "smsc" -> SmscScreen(state)
                    "call" -> CallScreen(state)
                    "callblocker" -> CallBlockerScreen(state)
                    "nauta" -> NautaScreen(state)
                    "nautamail" -> NautaMailScreen(state)
                    "online" -> OnlineScreen(state)
                    "useful" -> UsefulScreen(state)
                    "speed" -> SpeedScreen(state)
                    "firewall" -> FirewallScreen(state)
                    "forcelte" -> ForceLteScreen(state)
                    "antifraud" -> AntifraudScreen(state)
                    "backup" -> BackupScreen(state)
                    else -> HomeScreen(state)
                }
            }
        }
        // ── Floating nav: the OFFICIAL MD3 Expressive
        // HorizontalFloatingToolbar, Google Photos style — the same
        // improvised navbar LSEx/UDYAT use (Ernest's design). Items are
        // spring-animated label pills; the selected pill grows to reveal
        // its filled icon. (The ShortNavigationBar is gone: it rendered
        // flat and generic, and its alpha05 measure policies fought the
        // bar width — see AGENTS.md.)
        Box(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            EtkFloatingNavBar(tab, s.home, s.calls, s.messages, s.plans) { t ->
                state.screen = when (t) {
                    0 -> "home"; 1 -> "calls"; 2 -> "messages"; else -> "plans"
                }
            }
        }

    }
}


/** The improvised navbar (LSEx/UDYAT pattern, Ernest's design):
 *  the OFFICIAL MD3 Expressive HorizontalFloatingToolbar, Google
 *  Photos style. The 4dp end spacers keep the selected pill's
 *  breathing room at the bar's ends equal to its y-axis gaps. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EtkFloatingNavBar(
    tab: Int, home: String, calls: String, messages: String, plans: String,
    onSelect: (Int) -> Unit,
) {
    HorizontalFloatingToolbar(
        expanded = true,
        colors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
    ) {
        Spacer(Modifier.width(4.dp))
        EtkNavToolbarItem(tab == 0, MaterialSymbols.OutlinedFilled.Home, home) { onSelect(0) }
        EtkNavToolbarItem(tab == 1, MaterialSymbols.OutlinedFilled.Call, calls) { onSelect(1) }
        EtkNavToolbarItem(tab == 2, MaterialSymbols.OutlinedFilled.Sms, messages) { onSelect(2) }
        EtkNavToolbarItem(tab == 3, MaterialSymbols.OutlinedFilled.Shopping_bag, plans) { onSelect(3) }
        Spacer(Modifier.width(4.dp))
    }
}

/** One nav pill: a spring-animated label whose selected state grows
 *  to reveal its filled icon (LSEx's LsexNavToolbarItem). */
@Composable
private fun EtkNavToolbarItem(
    selected: Boolean, selectedIcon: ImageVector, label: String, onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val fraction by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
        label = "navItemSelectedFraction",
    )
    val f = fraction.coerceIn(0f, 1f)
    Box(
        Modifier.clip(RoundedCornerShape(50))
            .background(scheme.secondaryContainer.copy(alpha = f), RoundedCornerShape(50))
            .clickable(onClick = onClick),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (f >= 0.5f) FontWeight.Medium else FontWeight.Normal,
            color = androidx.compose.ui.graphics.lerp(scheme.onSurface, scheme.onSecondaryContainer, f),
            maxLines = 1,
            modifier = Modifier.padding(
                start = androidx.compose.ui.unit.lerp(12.dp, 44.dp, f),
                end = 12.dp, top = 10.dp, bottom = 10.dp,
            ),
        )
        if (f > 0.01f) {
            Box(Modifier.align(Alignment.CenterStart).padding(start = 16.dp)) {
                Icon(selectedIcon, contentDescription = null,
                    tint = scheme.onSecondaryContainer.copy(alpha = f),
                    modifier = Modifier.size(20.dp))
            }
        }
    }
}

/** Connected ToggleButton group — the SIM slot selector. ALWAYS shows both
 *  slots, even with a single/undetected SIM (Ernest). Slot-based: choosing a
 *  slot re-targets every query/transfer at that slot. This is the OFFICIAL
 *  MD3E ButtonGroup + ToggleButton — the custom white-pill variant is gone
 *  (Ernest, 2026-11-17: old-style pills). */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SimSelector(state: AppState) {
    val s = state.strings
    val sims = remember { state.bridge.simSlots() }
    Column {
        Text(s.simForQueries, style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            listOf(s.sim1 to 0, s.sim2 to 1).forEachIndexed { i, (label, slot) ->
                val selected = state.slot == slot
                val info = sims.firstOrNull { it.slot == slot }
                val text = if (info?.detected == true) "$label · ${info.carrier}" else "$label · —"
                val shapes = when (i) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    else -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                }
                ToggleButton(
                    checked = selected,
                    onCheckedChange = { if (it) { state.slot = slot; state.store.put("sim_slot", slot.toString()) } },
                    shapes = shapes,
                    modifier = Modifier.weight(1f).padding(horizontal = 1.dp),
                ) {
                    Text(text, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))
}

@Composable
fun ScreenScaffold(state: AppState, title: String, content: @Composable ColumnScope.() -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 110.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { state.back() }) {
                    Icon(MaterialSymbols.Outlined.Arrow_back, contentDescription = null)
                }
                Text(title, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { state.screen = "history" }) {
                    Icon(MaterialSymbols.Outlined.History, contentDescription = state.strings.history)
                }
            }
            Column(content = content)
        }
    }
}
