package dev.kurokawa.etecsa.toolkit.ui

import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.outlined.*
import com.composables.icons.materialsymbols.outlinedfilled.*

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
            if (parsed.kind == ParsedResponse.Kind.BALANCE) balance = parsed
        }
    }
    fun back() { screen = "home" }
}

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
                text = { Row { CircularProgressIndicator(); Spacer(Modifier.width(16.dp)); Text(state.ussdResult ?: "") } },
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
            AppMark(Modifier.scale(scale.value).size(110.dp), big = true)
            Spacer(Modifier.height(20.dp))
            Text("ETECSA Toolkit", style = MaterialTheme.typography.headlineMedium)
            Text("Material 3 Expressive", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** The new app mark: ETECSA-blue → teal gradient tile with signal arcs.
 *  Used as splash, top-bar logo and (in vector form) the launcher icon. */
@Composable
fun AppMark(modifier: Modifier = Modifier, big: Boolean = false) {
    Box(
        modifier.clip(RoundedCornerShape(if (big) 30.dp else 14.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF0057A8), Color(0xFF00A99D)))),
        contentAlignment = Alignment.Center,
    ) {
        Icon(MaterialSymbols.Outlined.Signal_cellular_alt, contentDescription = null,
            tint = Color.White, modifier = Modifier.fillMaxSize(0.62f))
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MainShell(state: AppState, dark: Boolean, onDark: (Boolean) -> Unit) {
    val s = state.strings
    var query by remember { mutableStateOf("") }
    var searchOpen by remember { mutableStateOf(false) }
    val tab = when (state.screen) {
        "messages" -> 1; "calls" -> 2; "network", "speed", "firewall", "forcelte" -> 3; else -> 0
    }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            // ── Top bar: the MD3E SearchBar housing logo + wordmark + avatar ──
            SearchBar(
                inputField = {
                    SearchBarDefaults.InputField(
                        query = query, onQueryChange = { query = it },
                        onSearch = { searchOpen = false }, expanded = searchOpen,
                        onExpandedChange = { searchOpen = it },
                        placeholder = { Text(s.searchHint) },
                        leadingIcon = { AppMark(Modifier.size(36.dp)) },
                        trailingIcon = {
                            // Account avatar with circle container → Settings
                            Box(
                                Modifier.size(38.dp).clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .clickable { state.screen = "settings" },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(MaterialSymbols.Outlined.Person, contentDescription = s.settings,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        },
                    )
                },
                expanded = searchOpen, onExpandedChange = { searchOpen = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            ) {
                // Global search: services, plans, screens — everything indexed
                val hits = remember(query) {
                    if (query.isBlank()) emptyList()
                    else Services.filter {
                        it.titleEs.contains(query, true) || it.descEs.contains(query, true) || it.action.contains(query)
                    } + Plans.filter { it.title.contains(query, true) || it.code.contains(query) }
                        .map { ServiceItem("plan_" + it.id, Category.PLANS, it.title, it.detail + " · " + it.price, it.code) }
                }
                LazyColumn {
                    items(hits) { item ->
                        ListItem(
                            headlineContent = { Text(item.titleEs) },
                            supportingContent = { Text(item.descEs) },
                            leadingContent = { Icon(MaterialSymbols.Outlined.Search, null) },
                            modifier = Modifier.clickable {
                                searchOpen = false; query = ""
                                if (item.action.startsWith("screen:")) state.screen = item.action.removePrefix("screen:")
                                else state.runUssd(item.action, item.titleEs)
                            },
                        )
                    }
                }
            }
            if (!searchOpen) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(s.appName, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    IconButton(onClick = { state.screen = "history" }) {
                        Icon(MaterialSymbols.Outlined.History, contentDescription = s.history)
                    }
                }
                Box(Modifier.weight(1f)) {
                    when (state.screen) {
                        "home" -> HomeScreen(state)
                        "messages" -> MessagesScreen(state)
                        "calls" -> CallsScreen(state)
                        "network" -> NetworkScreen(state)
                        "settings" -> SettingsScreen(state, dark, onDark)
                        "history" -> HistoryScreen(state)
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
        }
        // ── Floating native ShortNavigationBar (82.7% width, as mocked) ──
        // NAV FIX (root cause proven on UDYAT 2026-11-13 by decompile +
        // instrumented repro): material3 alpha05's ShortNavigationBar
        // measure policies coerce every item's width UP to the bar's
        // incoming minWidth, so any fillMaxWidth/width on the BAR itself
        // inflates items to full bar width and pushes items 2..n off the
        // bar (UDYAT v1.3.2 rendered only its first tab). The bar must
        // receive LOOSE constraints: the chrome width lives on this
        // wrapper Box (a plain Box does not propagate min constraints),
        // and the bar only caps itself with widthIn.
        Column(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Box(Modifier.fillMaxWidth(0.827f), contentAlignment = Alignment.Center) {
            ShortNavigationBar(
                modifier = Modifier.widthIn(max = 552.dp)
                    .shadow(8.dp, RoundedCornerShape(36.dp)).clip(RoundedCornerShape(36.dp)),
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface,
                windowInsets = WindowInsets(0, 0, 0, 0),
                arrangement = ShortNavigationBarArrangement.EqualWeight,
            ) {
                ShortNavigationBarItem(
                    selected = tab == 0, onClick = { state.screen = "home" },
                    icon = { Icon(if (tab == 0) MaterialSymbols.OutlinedFilled.Home else MaterialSymbols.Outlined.Home, contentDescription = null) },
                    label = { Text(s.home) })
                ShortNavigationBarItem(
                    selected = tab == 1, onClick = { state.screen = "messages" },
                    icon = { Icon(if (tab == 1) MaterialSymbols.OutlinedFilled.Sms else MaterialSymbols.Outlined.Sms, contentDescription = null) },
                    label = { Text(s.messages) })
                ShortNavigationBarItem(
                    selected = tab == 2, onClick = { state.screen = "calls" },
                    icon = { Icon(if (tab == 2) MaterialSymbols.OutlinedFilled.Call else MaterialSymbols.Outlined.Call, contentDescription = null) },
                    label = { Text(s.calls) })
                ShortNavigationBarItem(
                    selected = tab == 3, onClick = { state.screen = "network" },
                    icon = { Icon(if (tab == 3) MaterialSymbols.OutlinedFilled.Cell_tower else MaterialSymbols.Outlined.Cell_tower, contentDescription = null) },
                    label = { Text(s.network) })
            }
          }
        }
    }
}


/** Connected ToggleButton group — the SIM slot selector. ALWAYS shows both
 *  slots, even with a single/undetected SIM (Ernest). Slot-based: choosing a
 *  slot re-targets every query/transfer at that slot. */
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
                val shapes = when (i) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    else -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                }
                ToggleButton(
                    checked = state.slot == slot,
                    onCheckedChange = { if (it) { state.slot = slot; state.store.put("sim_slot", slot.toString()) } },
                    shapes = shapes,
                    modifier = Modifier.weight(1f).padding(horizontal = 1.dp),
                ) {
                    val info = sims.firstOrNull { it.slot == slot }
                    Text(
                        if (info?.detected == true) "$label · ${info.carrier}" else "$label · —",
                        maxLines = 1, softWrap = false,
                    )
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
                Text(title, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { state.screen = "history" }) {
                    Icon(MaterialSymbols.Outlined.History, contentDescription = state.strings.history)
                }
            }
            Column(content = content)
        }
    }
}
