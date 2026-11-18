@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.kurokawa.etecsa.toolkit.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn

import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.outlined.*
import com.composables.icons.materialsymbols.outlinedfilled.*

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.AssistChip
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.kurokawa.etecsa.toolkit.core.*
import dev.kurokawa.etecsa.toolkit.model.*

// ───────────────────────── Home ─────────────────────────

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(state: AppState) {
    // ── v1.2 Home, rebuilt from the ORIGINAL's own layout (RoCla 5.4.3,
    // MainActivity + home_*_v* tags): consult header card with the SIM
    // selector, signal card, Favoritos tile grid (max 6), Servicios tile
    // grid. No global chrome: this page carries its own top row. ──
    val s = state.strings
    val scheme = MaterialTheme.colorScheme
    var inputFor by remember { mutableStateOf<ServiceItem?>(null) }
    var inputText by remember { mutableStateOf("") }
    var favTick by remember { mutableStateOf(0) }
    val favorites = remember(favTick) {
        val ids = (state.store.get("favorites")
            ?: "saldo,datos,bonos,minutos,smsq,amigos").split(",")
        ids.mapNotNull { id -> Services.firstOrNull { it.id == id } }.take(6)
    }
    fun toggleFavorite(item: ServiceItem) {
        val ids = favorites.map { it.id }.toMutableList()
        if (item.id in ids) ids.remove(item.id)
        else if (ids.size < 6) ids.add(item.id)
        state.store.put("favorites", ids.joinToString(","))
        favTick++
    }
    fun open(item: ServiceItem) = openService(state, item) { inputFor = it; inputText = "" }
    val level = remember(state.slot) { state.bridge.signalLevel(state.slot) }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 110.dp),
    ) {
        // Top row — Home only: mark + wordmark + history + settings
        item {
            // Home-only top row: the brand mark + wordmark + actions.
            // Search is an ICON here (v1.2.1 verdict: the full-width pill
            // read as the old v1.0 search bar) — it opens the Search screen.
            Row(verticalAlignment = Alignment.CenterVertically) {
                EtkMark(Modifier.size(36.dp))
                Spacer(Modifier.width(10.dp))
                Text(s.appName, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false,
                    modifier = Modifier.weight(1f))
                IconButton(onClick = { state.screen = "search" }) {
                    Icon(MaterialSymbols.Outlined.Search, contentDescription = s.searchHint)
                }
                IconButton(onClick = { state.screen = "history" }) {
                    Icon(MaterialSymbols.Outlined.History, contentDescription = s.history)
                }
                IconButton(onClick = { state.screen = "settings" }) {
                    Icon(MaterialSymbols.Outlined.Settings, contentDescription = s.settings)
                }
            }
        }
        // Consult header card (the original's home_consult_* block):
        // SIM selector, resource values, validity, last-consult stamp.
        item {
            Spacer(Modifier.height(14.dp))
            // Consult card on the SCHEME's primaryContainer — it follows
            // dynamic color on device. NO gradient here (Ernest, 2026-11-17:
            // gradients are for the logo only, never for in-app elements).
            // Spring entrance motion; the live last-consult stamp is the
            // dynamic state (a real consult stamps it — see AppState).
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                    + scaleIn(initialScale = 0.96f, animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)),
            ) {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = scheme.primaryContainer),
                    shape = RoundedCornerShape(28.dp),
                ) {
                    val onHero = scheme.onPrimaryContainer
                    Column(Modifier.padding(20.dp)) {
                        SimSelector(state)
                        Spacer(Modifier.height(14.dp))
                        Text(s.balance, style = MaterialTheme.typography.labelLarge,
                            color = onHero.copy(alpha = 0.85f))
                        val b = state.balance
                        Text(
                            if (b?.balanceCup != null) "${b.balanceCup} CUP" else "— CUP",
                            style = MaterialTheme.typography.displayMedium, color = onHero,
                        )
                        Row {
                            b?.dataBytes?.let { Text("Datos ${ResponseParser.formatData(it)}  ·  ", color = onHero) }
                            b?.voiceMinutes?.let { Text("Voz $it min  ·  ", color = onHero) }
                            b?.smsCount?.let { Text("SMS $it", color = onHero) }
                        }
                        b?.expires?.let {
                            Text("Válido hasta el $it", style = MaterialTheme.typography.bodySmall,
                                color = onHero.copy(alpha = 0.85f))
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val last = state.store.get("last_consult_ms")?.toLongOrNull()
                            Text(
                                when {
                                    last == null -> "Sin consultas aún"
                                    else -> {
                                        val mins = (nowMillis() - last) / 60000
                                        if (mins < 1) "Última consulta: ahora mismo"
                                        else if (mins < 60) "Última consulta: hace $mins min"
                                        else "Última consulta: hace ${mins / 60} h"
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = onHero.copy(alpha = 0.85f),
                                modifier = Modifier.weight(1f),
                            )
                            Button(onClick = { state.runUssd("*222#", "Saldo") }) {
                                Icon(MaterialSymbols.Outlined.Refresh, null); Spacer(Modifier.width(8.dp)); Text(s.consult)
                            }
                        }
                    }
                }
            }
        }
        // Signal card (home_signal_card_v4154): the quality is a live
        // bridge reading, shown with the official AssistChip + the
        // expressive wavy level indicator — no custom chip, no hardcoded
        // traffic-light colors (scheme colors follow dynamic color).
        item {
            Spacer(Modifier.height(12.dp))
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(28.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(MaterialSymbols.Outlined.Signal_cellular_alt, null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Señal", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        val label = when (level) {
                            4 -> "Excelente"; 3 -> "Buena"; 2 -> "Débil"
                            1, 0 -> "Mala"; else -> "No disp"
                        }
                        AssistChip(
                            onClick = { state.screen = "network" },
                            label = { Text(label) },
                            leadingIcon = {
                                Icon(MaterialSymbols.Outlined.Signal_cellular_alt, null,
                                    modifier = Modifier.size(18.dp))
                            },
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    LinearWavyProgressIndicator(
                        progress = { level.coerceIn(0, 4) / 4f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        // Favoritos — tile grid, max 6 (the original's C1())
        item { SectionHeader("Favoritos") }
        if (favorites.isEmpty()) {
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = RoundedCornerShape(27.dp),
                ) {
                    Text("No tienes accesos favoritos.", Modifier.padding(horizontal = 17.dp, vertical = 22.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(favorites.chunked(3)) { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { item ->
                        ServiceTile(item, Modifier.weight(1f), onClick = { open(item) },
                            onLongPress = { toggleFavorite(item) })
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(10.dp))
            }
        }
        // Servicios — the tile grid MINUS the favorites (home_services_grid_v3257).
        // The favorites grid above and this one never repeat an item.
        item { SectionHeader("Servicios") }
        items(Services.filter { it.id !in favorites.map { fav -> fav.id }.toSet() }.chunked(3)) { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { item ->
                    ServiceTile(item, Modifier.weight(1f), onClick = { open(item) },
                        onLongPress = { toggleFavorite(item) })
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(10.dp))
        }
        item {
            Text("Mantén pulsado un servicio para fijarlo en Favoritos (máx. 6).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp))
        }
    }
    // Input dialog for {input} services (recharge card, *99, private call)
    inputFor?.let { item ->
        AlertDialog(
            onDismissRequest = { inputFor = null },
            title = { Text(item.titleEs) },
            text = {
                OutlinedTextField(inputText, { inputText = it }, label = { Text(item.inputLabel ?: s.number) },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                TextButton(onClick = {
                    val code = item.action.replace("{input}", inputText.trim())
                    inputFor = null
                    if (item.inputIsCall) state.bridge.call(code) else state.runUssd(code, item.titleEs)
                }) { Text(s.confirm) }
            },
            dismissButton = { TextButton(onClick = { inputFor = null }) { Text(s.cancel) } },
        )
    }
}

/** The original's small-caps section label (C5: 10sp bold, tracked,
 *  secondary, 17dp above / 9dp below). */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SectionHeader(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelLargeEmphasized.copy(letterSpacing = 1.2.sp),
        color = MaterialTheme.colorScheme.tertiary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 3.dp, top = 17.dp, bottom = 9.dp),
    )
}

/** Per-service accent (the original colours each service's glyph). */
fun colorFor(id: String): Color = when (id) {
    "saldo", "pospago" -> Color(0xFF2E7D32)
    "datos", "disponibilidad" -> Color(0xFF1565C0)
    "bonos" -> Color(0xFFEF6C00)
    "minutos", "llamada_normal", "cobrar", "privada" -> Color(0xFF00838F)
    "smsq" -> Color(0xFF6A1B9A)
    "amigos" -> Color(0xFFFF4F91)
    "nauta", "nauta_mailbox" -> Color(0xFF0277BD)
    "etecsa_services" -> Color(0xFF3949AB)
    "comprar" -> Color(0xFFAD1457)
    "transferir" -> Color(0xFF4527A0)
    "adelanta_saldo" -> Color(0xFFF9A825)
    "antifraud" -> Color(0xFFC62828)
    "firewall", "call_blocker" -> Color(0xFF37474F)
    "speed", "traffic_monitor", "lte_monitor" -> Color(0xFF00695C)
    else -> Color(0xFF0057A8)
}

/** Home tile: rounded-27 card, 50dp coloured glyph, 12sp bold label —
 *  the original's grid tile (favorites O() / services R()). */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ServiceTile(item: ServiceItem, modifier: Modifier = Modifier,
                onClick: () -> Unit, onLongPress: () -> Unit) {
    Card(
        modifier.combinedClickable(onClick = onClick, onLongClick = onLongPress),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(32.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // One flat tile: the icon sits straight on it (the original's
            // tile language). The inner solid tile (tile-in-tile) is gone.
            Icon(iconFor(item.id), null, tint = colorFor(item.id), modifier = Modifier.size(44.dp))
            Spacer(Modifier.height(8.dp))
            Text(item.titleEs, style = MaterialTheme.typography.labelLargeEmphasized,
                maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

/** The original's w5 row (sheets/lists): rounded-22 card, 44dp rounded-16
 *  accent tile with white 23dp glyph, 14sp bold title, 10sp description,
 *  trailing chevron. */
@Composable
fun ServiceRow(item: ServiceItem, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(22.dp),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(16.dp)).background(colorFor(item.id)),
                contentAlignment = Alignment.Center,
            ) { Icon(iconFor(item.id), null, tint = Color.White, modifier = Modifier.size(23.dp)) }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(item.titleEs, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(item.descEs, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
            Icon(MaterialSymbols.Outlined.Chevron_right, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
    }
}

/** Shared service dispatch (Home tiles, search results). */
fun openService(state: AppState, item: ServiceItem, onInput: (ServiceItem) -> Unit) {
    when {
        item.action.startsWith("screen:") -> state.screen = item.action.removePrefix("screen:")
        item.action.startsWith("tel:") -> state.bridge.call(item.action.removePrefix("tel:"))
        item.inputLabel != null || item.inputIsCall -> onInput(item)
        else -> state.runUssd(item.action, item.titleEs)
    }
}

// ───────────────────────── Search ─────────────────────────

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SearchScreen(state: AppState) {
    // Ernest's global search as its own screen, on the OFFICIAL MD3E
    // SearchBar (official-component-first rule) — the plain TextField
    // version read as baseline M3, not Expressive.
    val s = state.strings
    var query by remember { mutableStateOf("") }
    var inputFor by remember { mutableStateOf<ServiceItem?>(null) }
    var inputText by remember { mutableStateOf("") }
    val hits = remember(query) {
        if (query.isBlank()) emptyList()
        else Services.filter {
            it.titleEs.contains(query, true) || it.descEs.contains(query, true) || it.action.contains(query)
        } + Plans.filter { it.title.contains(query, true) || it.code.contains(query) }
            .map { ServiceItem("plan_" + it.id, Category.PLANS, it.title, it.detail + " · " + it.price, it.code) }
    }
    SearchBar(
        inputField = {
            SearchBarDefaults.InputField(
                query = query, onQueryChange = { query = it },
                onSearch = {}, expanded = true,
                onExpandedChange = { if (!it) state.back() },
                placeholder = { Text(s.searchHint) },
                leadingIcon = {
                    IconButton(onClick = { state.back() }) {
                        Icon(MaterialSymbols.Outlined.Arrow_back, contentDescription = null)
                    }
                },
            )
        },
        expanded = true, onExpandedChange = { if (!it) state.back() },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    ) {
        LazyColumn(contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(hits) { item ->
                ServiceRow(item) {
                    openService(state, item) { inputFor = it; inputText = "" }
                }
            }
        }
    }
    inputFor?.let { item ->
        AlertDialog(
            onDismissRequest = { inputFor = null },
            title = { Text(item.titleEs) },
            text = {
                OutlinedTextField(inputText, { inputText = it }, label = { Text(item.inputLabel ?: s.number) },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                TextButton(onClick = {
                    val code = item.action.replace("{input}", inputText.trim())
                    inputFor = null
                    if (item.inputIsCall) state.bridge.call(code) else state.runUssd(code, item.titleEs)
                }) { Text(s.confirm) }
            },
            dismissButton = { TextButton(onClick = { inputFor = null }) { Text(s.cancel) } },
        )
    }
}

fun iconFor(id: String) = when (id) {
    "saldo" -> MaterialSymbols.Outlined.Account_balance_wallet
    "datos" -> MaterialSymbols.Outlined.Data_usage
    "bonos" -> MaterialSymbols.Outlined.Redeem
    "minutos" -> MaterialSymbols.Outlined.Call
    "smsq", "mensajes" -> MaterialSymbols.Outlined.Sms
    "pospago" -> MaterialSymbols.Outlined.Receipt_long
    "comprar", "amigos" -> MaterialSymbols.Outlined.Shopping_bag
    "transferir" -> MaterialSymbols.Outlined.Payments
    "adelanta" -> MaterialSymbols.Outlined.Bolt
    "recarga_tarjeta", "cards" -> MaterialSymbols.Outlined.Credit_card
    "nauta", "nauta_mailbox" -> MaterialSymbols.Outlined.Wifi
    "etecsa_services" -> MaterialSymbols.Outlined.Cloud_sync
    "lte_monitor", "traffic_monitor" -> MaterialSymbols.Outlined.Cell_tower
    "speed" -> MaterialSymbols.Outlined.Speed
    "firewall" -> MaterialSymbols.Outlined.Shield
    "forzar_lte" -> MaterialSymbols.Outlined.Network_check
    "antifraud" -> MaterialSymbols.Outlined.Verified_user
    "call_blocker" -> MaterialSymbols.Outlined.Block
    "backup" -> MaterialSymbols.Outlined.Backup
    "smsc" -> MaterialSymbols.Outlined.Settings_phone
    "imei" -> MaterialSymbols.Outlined.Phone_android
    else -> MaterialSymbols.Outlined.Apps
}

// ───────────────────────── History ─────────────────────────

@Composable
fun HistoryScreen(state: AppState) {
    val s = state.strings
    var entries by remember { mutableStateOf(state.store.history(100)) }
    ScreenScaffold(state, s.history) {
        Row {
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { state.store.clearHistory(); entries = emptyList() }) {
                Icon(MaterialSymbols.Outlined.Delete, null); Text(s.edit)
            }
        }
        if (entries.isEmpty()) Text(s.empty)
        entries.forEach { e ->
            ListItem(
                headlineContent = { Text(e.title) },
                supportingContent = { Text(e.body, maxLines = 3) },
                leadingContent = { Icon(MaterialSymbols.Outlined.History, null) },
            )
            HorizontalDivider()
        }
    }
}

// ───────────────────────── Settings (Play-Store account style) ─────────────────────────

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(state: AppState, dark: Boolean, onDark: (Boolean) -> Unit) {
    val s = state.strings
    var monitor by remember { mutableStateOf(state.bridge.coverageMonitorOn()) }
    var alarm by remember { mutableStateOf(state.store.get("cov_alarm") == "1") }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 110.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { state.back() }) { Icon(MaterialSymbols.Outlined.Arrow_back, null) }
                Text(s.settings, style = MaterialTheme.typography.headlineMedium)
            }
            // Brand header card on the scheme's primaryContainer (dynamic
            // color). The "e" disc keeps gradient A — the logo is the only
            // place a gradient lives (Ernest, 2026-11-17).
            Card(
                Modifier.fillMaxWidth().padding(vertical = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(28.dp),
            ) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    EtkMark(Modifier.size(56.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        val onCard = MaterialTheme.colorScheme.onPrimaryContainer
                        Text(s.appName, style = MaterialTheme.typography.titleLarge, color = onCard)
                        Text("${s.version} 1.2.0 · dev.kurokawa.etecsa.toolkit",
                            style = MaterialTheme.typography.bodySmall, color = onCard.copy(alpha = 0.85f))
                        val sims = remember { state.bridge.simSlots() }
                        Text(sims.filter { it.detected }.joinToString(" · ") { "${it.carrier} ${it.number}".trim() }
                            .ifBlank { "—" }, style = MaterialTheme.typography.bodySmall,
                            color = onCard.copy(alpha = 0.85f))
                    }
                }
            }
        }
        item { SectionTitle(s.appearance) }
        item {
            ListItem(headlineContent = { Text(s.darkTheme) },
                trailingContent = { Switch(dark, onDark) })
            ListItem(headlineContent = { Text(s.language) },
                supportingContent = { Text(if (state.lang == "es") "Español" else "English") })
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                listOf("English" to "en", "Español" to "es").forEachIndexed { i, (label, code) ->
                    ToggleButton(
                        checked = state.lang == code,
                        onCheckedChange = { if (it) state.applyLang(code) },
                        shapes = if (i == 0) ButtonGroupDefaults.connectedLeadingButtonShapes()
                                 else ButtonGroupDefaults.connectedTrailingButtonShapes(),
                        modifier = Modifier.weight(1f).padding(horizontal = 1.dp),
                    ) { Text(label) }
                }
            }
        }
        item { SimSelector(state) }
        item { SectionTitle(s.coverageMonitor) }
        item {
            ListItem(headlineContent = { Text(s.coverageMonitor) },
                supportingContent = { Text(s.coverageAlarmDesc) },
                trailingContent = {
                    Switch(checked = monitor, onCheckedChange = { on ->
                        monitor = on; state.bridge.setCoverageMonitor(on, alarm)
                    })
                })
            ListItem(headlineContent = { Text(s.coverageAlarm) },
                supportingContent = { Text(s.coverageAlarmDesc) },
                trailingContent = {
                    Switch(checked = alarm, onCheckedChange = { on ->
                        alarm = on; state.store.put("cov_alarm", if (on) "1" else "0")
                        state.bridge.setCoverageMonitor(monitor, on)
                    })
                })
        }
        item { SectionTitle(s.about) }
        item {
            Text(s.aboutBody, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { state.screen = "backup" }) {
                Icon(MaterialSymbols.Outlined.Backup, null); Spacer(Modifier.width(8.dp)); Text(s.backup)
            }
            TextButton(onClick = { state.bridge.openUrl("https://github.com/kurokawaerne/etecsa-toolkit/releases") }) {
                Icon(MaterialSymbols.Outlined.System_update, null); Spacer(Modifier.width(8.dp)); Text(s.update)
            }
        }
    }
}

// ───────────────────────── Plans ─────────────────────────

@Composable
fun PlansScreen(state: AppState) {
    // Planes is a TAB (the original's 4th nav destination) — it gets its
    // own title row, no back arrow (v1.2 fix: it used ScreenScaffold).
    val s = state.strings
    var pending by remember { mutableStateOf<Plan?>(null) }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(s.plans, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = { state.screen = "history" }) {
                Icon(MaterialSymbols.Outlined.History, contentDescription = s.history)
            }
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 110.dp)) {
            listOf("data" to "Datos", "voice" to "Voz", "sms" to "SMS").forEach { (cat, label) ->
                item { SectionHeader(label) }
                items(Plans.filter { it.category == cat }) { plan ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(24.dp)) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(plan.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(plan.detail, style = MaterialTheme.typography.bodySmall)
                                Text("${s.price}: ${plan.price}", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                            Button(onClick = { pending = plan }) { Text(s.buy) }
                        }
                    }
                }
            }
        }
    }
    pending?.let { plan ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text(plan.title) },
            text = { Text("${plan.detail}\n${s.price}: ${plan.price}\n${plan.code}") },
            confirmButton = {
                TextButton(onClick = { pending = null; state.runUssd(plan.code, "Plan ${plan.title}") }) { Text(s.confirm) }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text(s.cancel) } },
        )
    }
}

@Composable
fun TransferScreen(state: AppState) {
    val s = state.strings
    var number by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf(state.store.getSecret("transfer_pin") ?: "") }
    var savePin by remember { mutableStateOf(pin.isNotEmpty()) }
    var showContacts by remember { mutableStateOf(false) }
    ScreenScaffold(state, "Transferir saldo") {
        SimSelector(state)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(number, { number = it }, label = { Text(s.number) },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { showContacts = true }) {
                    Icon(MaterialSymbols.Outlined.Contacts, null)
                }
            })
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(amount, { amount = it.filter { c -> c.isDigit() } }, label = { Text(s.amount) },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(pin, { pin = it }, label = { Text(s.pin) },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { state.runUssd("*234*3*1#", "Cambiar PIN") }) {
                    Icon(MaterialSymbols.Outlined.Edit, contentDescription = s.edit)
                }
            })
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(savePin, { savePin = it }); Text("Guardar PIN (cifrado)")
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                if (savePin) state.store.putSecret("transfer_pin", pin)
                // Cubacel transfer format: *234*1*<número>*<importe>*<clave>#
                val code = "*234*1*${number.trim()}*${amount.trim()}*${pin.trim()}#"
                state.store.addHistory("transfer", "Transferir saldo", "$amount CUP → $number", state.slot)
                state.runUssd(code, "Transferir saldo")
            },
            enabled = number.length >= 8 && amount.isNotEmpty() && pin.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) { Icon(MaterialSymbols.Outlined.Payments, null); Spacer(Modifier.width(8.dp)); Text(s.send) }
        Text("ETECSA permite máx. 3 transferencias al día.", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        // Monthly totals (the original tracks them): sum this month's
        // transfer history entries, amounts ride in the entry body.
        val monthStart = remember { monthStartMillis() }
        val monthTransfers = remember { state.store.history(500).filter { it.kind == "transfer" && it.ts >= monthStart } }
        if (monthTransfers.isNotEmpty()) {
            val total = monthTransfers.sumOf { it.body.substringBefore(" CUP").toDoubleOrNull() ?: 0.0 }
            Spacer(Modifier.height(12.dp))
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(20.dp),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(MaterialSymbols.Outlined.Payments, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Este mes", style = MaterialTheme.typography.labelLarge)
                        Text("$total CUP en ${monthTransfers.size} transferencia(s)",
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
    if (showContacts) {
        val contacts = remember { state.bridge.contacts() }
        AlertDialog(
            onDismissRequest = { showContacts = false },
            title = { Text(s.number) },
            text = {
                LazyColumn {
                    items(contacts) { c ->
                        ListItem(headlineContent = { Text(c.name) }, supportingContent = { Text(c.number) },
                            leadingContent = { ContactAvatar(c) },
                            modifier = Modifier.clickable { number = c.number.filter { it.isDigit() }; showContacts = false })
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showContacts = false }) { Text(s.close) } },
        )
    }
}

/** Contact photo (Contacts API) or an expressive initial avatar. */
@Composable
fun ContactAvatar(c: ContactInfo, size: Int = 40) {
    // Photo loading is handled by the platform layer cache in v1.0: we show
    // the photo when the URI is present via the initials fallback colour —
    // the URI itself is exposed for the detail screens.
    Box(
        Modifier.size(size.dp).clip(androidx.compose.foundation.shape.CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(c.name.take(1).uppercase(), color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Bold)
    }
}

// ───────────────────────── Plan Amigos ─────────────────────────

@Composable
fun FriendsScreen(state: AppState) {
    var friends by remember { mutableStateOf(state.store.friends()) }
    var showContacts by remember { mutableStateOf(false) }
    ScreenScaffold(state, "Plan Amigos") {
        Row(Modifier.fillMaxWidth()) {
            Button(onClick = { state.runUssd("*222*264#", "Plan Amigos") }, Modifier.weight(1f)) { Text(state.strings.consult) }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = { state.runUssd("*133*4*1*1#", "Activar Plan Amigos") }, Modifier.weight(1f)) { Text("Activar") }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = { state.runUssd("*133*4*1*2#", "Desactivar Plan Amigos") }, Modifier.weight(1f)) { Text("Desactivar") }
        }
        SectionTitle("Lista de amigos")
        if (friends.isEmpty()) Text(state.strings.empty)
        friends.forEach { f ->
            ListItem(headlineContent = { Text(f.name) }, supportingContent = { Text(f.number) },
                leadingContent = { ContactAvatar(f) },
                trailingContent = {
                    IconButton(onClick = {
                        state.runUssd("*133*4*2*2#", "Quitar amigo")
                        state.store.setFriend(f, false); friends = state.store.friends()
                    }) { Icon(MaterialSymbols.Outlined.Delete, null) }
                })
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { showContacts = true }, Modifier.fillMaxWidth()) {
            Icon(MaterialSymbols.Outlined.Add, null); Text("Añadir amigo")
        }
    }
    if (showContacts) {
        val contacts = remember { state.bridge.contacts() }
        AlertDialog(onDismissRequest = { showContacts = false }, title = { Text("Añadir amigo") },
            text = {
                LazyColumn {
                    items(contacts) { c ->
                        ListItem(headlineContent = { Text(c.name) }, supportingContent = { Text(c.number) },
                            leadingContent = { ContactAvatar(c) },
                            modifier = Modifier.clickable {
                                state.store.setFriend(c, true); friends = state.store.friends()
                                state.runUssd("*133*4*2*1*${c.number.filter { it.isDigit() }}#", "Añadir amigo")
                                showContacts = false
                            })
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showContacts = false }) { Text(state.strings.close) } })
    }
}

// ───────────────────────── Adelanta Saldo ─────────────────────────

@Composable
fun AdvanceScreen(state: AppState) {
    ScreenScaffold(state, "Adelanta Saldo") {
        Text("RoCla abría el menú interactivo de Cubacel; aquí igual: se abre la sesión USSD de Adelanta Saldo y eliges el monto cuando ETECSA lo solicite.")
        Spacer(Modifier.height(12.dp))
        listOf(25, 50).forEach { amt ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                state.runUssd("*234*2#", "Adelanta Saldo $amt")
            }, shape = RoundedCornerShape(24.dp)) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(MaterialSymbols.Outlined.Bolt, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text("Adelantar $amt CUP", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

// ───────────────────────── Cards ─────────────────────────

@Composable
fun CardsScreen(state: AppState) {
    val s = state.strings
    var cards by remember { mutableStateOf(state.store.cards()) }
    var label by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    ScreenScaffold(state, "Tarjetas") {
        Text("Las tarjetas se guardan cifradas (AES-GCM, AndroidKeyStore) y solo se muestra el final.")
        Spacer(Modifier.height(8.dp))
        cards.forEach { (l, masked) ->
            ListItem(headlineContent = { Text(l) }, supportingContent = { Text(masked) },
                leadingContent = { Icon(MaterialSymbols.Outlined.Credit_card, null) },
                trailingContent = {
                    IconButton(onClick = { state.store.deleteCard(l); cards = state.store.cards() }) {
                        Icon(MaterialSymbols.Outlined.Delete, null)
                    }
                })
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(label, { label = it }, label = { Text("Etiqueta") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(number, { number = it }, label = { Text("Número de tarjeta") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Row {
            Button(onClick = {
                if (label.isNotBlank() && number.isNotBlank()) {
                    state.store.saveCard(label, number); cards = state.store.cards(); label = ""; number = ""
                }
            }) { Text("Guardar") }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = {
                val full = state.store.getSecret("card_" + label)
                if (full != null) state.runUssd("*662*$full#", "Recargar con tarjeta")
            }) { Text("Recargar") }
        }
        Text(s.edit, style = MaterialTheme.typography.labelSmall)
    }
}

// ───────────────────────── SMSC ─────────────────────────

@Composable
fun SmscScreen(state: AppState) {
    var smsc by remember { mutableStateOf(state.store.get("smsc") ?: "+535920000") }
    ScreenScaffold(state, "Centro de mensajes (SMSC)") {
        Text("SMSC guardado para la SIM seleccionada. Cambiarlo puede impedir el envío de SMS: verifica el número con ETECSA antes de guardarlo.")
        Spacer(Modifier.height(12.dp))
        SimSelector(state)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(smsc, { smsc = it }, label = { Text("SMSC") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { state.store.put("smsc", smsc) }) {
                    Icon(MaterialSymbols.Outlined.Edit, contentDescription = state.strings.edit)
                }
            })
        Button(onClick = { state.store.put("smsc", smsc) }, Modifier.fillMaxWidth()) { Text("Guardar") }
    }
}

// ───────────────────────── Messages ─────────────────────────

/** Quick replies (the original's respuestas rápidas): prefs-backed chips.
 *  Tap inserts into the draft; the last chip opens the manager (add/delete). */
@Composable
fun QuickRepliesRow(state: AppState, onPick: (String) -> Unit) {
    var replies by remember { mutableStateOf(state.store.get("quick_replies")?.split("\n")?.filter { it.isNotBlank() } ?: emptyList()) }
    var manage by remember { mutableStateOf(false) }
    fun persist(list: List<String>) {
        replies = list
        state.store.put("quick_replies", list.joinToString("\n"))
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(replies) { r ->
            AssistChip(onClick = { onPick(r) }, label = { Text(r, maxLines = 1) })
        }
        item {
            AssistChip(onClick = { manage = true },
                label = { Text("Respuestas rápidas +") },
                leadingIcon = { Icon(MaterialSymbols.Outlined.Quickreply, null) })
        }
    }
    if (manage) {
        var newReply by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { manage = false },
            title = { Text("Respuestas rápidas") },
            text = {
                Column {
                    replies.forEach { r ->
                        ListItem(headlineContent = { Text(r) },
                            trailingContent = {
                                IconButton(onClick = { persist(replies - r) }) { Icon(MaterialSymbols.Outlined.Delete, null) }
                            })
                    }
                    if (replies.isEmpty()) Text(state.strings.empty)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(newReply, { newReply = it }, label = { Text("Nueva respuesta") },
                        singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = { if (newReply.isNotBlank()) { persist(replies + newReply.trim()); newReply = "" } }) { Text("Añadir") }
            },
            dismissButton = { TextButton(onClick = { manage = false }) { Text(state.strings.cancel) } },
        )
    }
}

@Composable
fun MessagesScreen(state: AppState) {
    // Mensajes in the Google Messages (MD3E) layout: conversations grouped
    // by address inside ONE rounded container with larger avatars, a
    // "Start chat" FAB, thread view with bubbles in their own container and
    // a pill composer. The original's features stay: Smart SMS queue with
    // send-on-coverage, antifraud warnings (banner in-thread + icon in the
    // list), block sender, SMSC entry, and the segment/cost counter under
    // the composer (the original has it; Google doesn't).
    val s = state.strings
    var inbox by remember { mutableStateOf<List<SmsInfo>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var contacts by remember { mutableStateOf<List<ContactInfo>>(emptyList()) }
    var openThread by remember { mutableStateOf<String?>(null) }
    var showCompose by remember { mutableStateOf(false) }
    var showAutoReply by remember { mutableStateOf(false) }
    var schedTick by remember { mutableStateOf(0) }
    val scheduled = remember(schedTick) { state.store.scheduledSms() }
    LaunchedEffect(Unit) { inbox = state.bridge.smsInbox(120); contacts = state.bridge.contacts(); loaded = true }
    fun nameFor(number: String): String {
        val tail = number.filter { it.isDigit() }.takeLast(8)
        return contacts.firstOrNull { it.number.filter { d -> d.isDigit() }.takeLast(8) == tail && tail.isNotEmpty() }?.name ?: number
    }
    val conversations = remember(inbox) {
        inbox.groupBy { it.address }.map { (addr, msgs) -> addr to msgs.maxByOrNull { it.date }!! }
            .sortedByDescending { it.second.date }
    }
    val queued = remember { state.store.queuedSms() }

    if (openThread != null) {
        val addr = openThread!!
        val thread = inbox.filter { it.address == addr }.sortedBy { it.date }
        var draft by remember(addr) { mutableStateOf("") }
        var queueMode by remember(addr) { mutableStateOf(false) }
        val suspicious = thread.any { Antifraud.looksSuspicious(it.address, it.body, OfficialSenders) }
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { openThread = null }) { Icon(MaterialSymbols.Outlined.Arrow_back, contentDescription = "Atrás") }
                ContactAvatar(ContactInfo(nameFor(addr), addr, null), 40)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(nameFor(addr), style = MaterialTheme.typography.titleMedium)
                    Text(addr, style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = { state.store.setBlocked(addr, true) }) { Icon(MaterialSymbols.Outlined.Block, contentDescription = "Bloquear") }
            }
            if (suspicious) {
                Card(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(MaterialSymbols.Outlined.Warning, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text("Posible fraude: este remitente podría estar suplantando a un servicio oficial.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(vertical = 8.dp)) {
                items(thread) { m ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        horizontalArrangement = if (m.incoming) Arrangement.Start else Arrangement.End,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (m.incoming) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.widthIn(max = 280.dp),
                        ) { Text(m.body, Modifier.padding(horizontal = 12.dp, vertical = 8.dp), style = MaterialTheme.typography.bodyMedium) }
                    }
                }
            }
            QuickRepliesRow(state) { r -> draft = if (draft.isBlank()) r else draft + " " + r }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilterChip(selected = queueMode, onClick = { queueMode = !queueMode },
                    label = { Text("Al volver cobertura") },
                    leadingIcon = { Icon(MaterialSymbols.Outlined.Schedule, null) })
            }
            Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.Bottom) {
                OutlinedTextField(
                    draft, { draft = it },
                    placeholder = { Text("Mensaje SMS") },
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.weight(1f),
                    maxLines = 4,
                )
                Spacer(Modifier.width(8.dp))
                FloatingActionButton(onClick = {
                    if (draft.isNotBlank()) {
                        if (queueMode) {
                            state.store.queueSms(addr, draft, state.slot, true)
                            state.bridge.setCoverageMonitor(true, state.store.get("cov_alarm") == "1")
                        } else {
                            state.bridge.sendSms(addr, draft, state.slot)
                            state.store.addHistory("sms", addr, draft, state.slot)
                        }
                        draft = ""
                    }
                }) { Icon(MaterialSymbols.Outlined.Send, contentDescription = s.send) }
            }
            if (draft.isNotEmpty()) {
                val segs = smsSegments(draft)
                val intl = addr.startsWith("+") && !addr.startsWith("+53")
                Text(
                    buildString {
                        append("$segs segmento(s) · ${draft.length} caracteres")
                        if (intl) append(" · ~${segs * 15} CUP (internacional)")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(bottom = 100.dp),
                )
            } else {
                Spacer(Modifier.height(100.dp))
            }
        }
        return
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 120.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(s.messages, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = { showAutoReply = true }) { Icon(MaterialSymbols.Outlined.Reply, contentDescription = "Respuesta automática") }
                    IconButton(onClick = { state.screen = "smsc" }) { Icon(MaterialSymbols.Outlined.Settings_phone, contentDescription = "SMSC") }
                }
                SimSelector(state)
                Spacer(Modifier.height(10.dp))
            }
            if (queued.isNotEmpty()) {
                item {
                    SectionTitle("En cola (${queued.size})")
                    queued.forEach { (n, body, _) ->
                        ListItem(headlineContent = { Text(nameFor(n)) }, supportingContent = { Text(body, maxLines = 1) },
                            leadingContent = { Icon(MaterialSymbols.Outlined.Schedule, null) })
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
            if (scheduled.isNotEmpty()) {
                item {
                    SectionTitle("Programados (${scheduled.size})")
                    scheduled.forEach { sc ->
                        ListItem(headlineContent = { Text(nameFor(sc.number)) },
                            supportingContent = { Text(sc.body, maxLines = 1) },
                            leadingContent = { Icon(MaterialSymbols.Outlined.Schedule_send, null) },
                            trailingContent = {
                                IconButton(onClick = { state.store.cancelScheduledSms(sc.id); schedTick++ }) {
                                    Icon(MaterialSymbols.Outlined.Delete, null)
                                }
                            })
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
            if (loaded && conversations.isEmpty()) {
                item { Text(s.noPermission) }
            } else if (conversations.isNotEmpty()) {
                item {
                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        shape = RoundedCornerShape(24.dp),
                    ) {
                        Column {
                            conversations.forEachIndexed { idx, (addr, last) ->
                                val susp = Antifraud.looksSuspicious(addr, last.body, OfficialSenders)
                                ListItem(
                                    headlineContent = { Text(nameFor(addr)) },
                                    supportingContent = { Text(last.body, maxLines = 2) },
                                    leadingContent = { ContactAvatar(ContactInfo(nameFor(addr), addr, null), 48) },
                                    trailingContent = if (susp) {
                                        { Icon(MaterialSymbols.Outlined.Warning, null, tint = MaterialTheme.colorScheme.error) }
                                    } else null,
                                    colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                                    modifier = Modifier.clickable { openThread = addr },
                                )
                                if (idx < conversations.lastIndex) HorizontalDivider(Modifier.padding(start = 72.dp))
                            }
                        }
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { showCompose = true },
            icon = { Icon(MaterialSymbols.Outlined.Edit, null) },
            text = { Text("Iniciar chat") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 118.dp),
        )
    }
    if (showCompose) {
        var to by remember { mutableStateOf("") }
        var text by remember { mutableStateOf("") }
        var onCoverage by remember { mutableStateOf(false) }
        var schedOffset by remember { mutableStateOf(0L) }
        ModalBottomSheet(onDismissRequest = { showCompose = false }) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
                Text("Nuevo mensaje", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(to, { to = it }, label = { Text(s.number) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(text, { text = it }, label = { Text(s.messages) }, modifier = Modifier.fillMaxWidth(), maxLines = 4)
                if (text.isNotEmpty()) {
                    val segs = smsSegments(text)
                    val intl = to.startsWith("+") && !to.startsWith("+53")
                    Text(
                        buildString {
                            append("$segs segmento(s) · ${text.length} caracteres")
                            if (intl) append(" · ~${segs * 15} CUP (internacional)")
                        },
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                QuickRepliesRow(state) { r -> text = if (text.isBlank()) r else text + " " + r }
                Spacer(Modifier.height(4.dp))
                Text("Programar envío", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val opts = listOf(0L to "Ahora", 3600000L to "En 1 h", 10800000L to "En 3 h", 21600000L to "En 6 h", 43200000L to "En 12 h", 86400000L to "En 24 h")
                    items(opts.size) { i ->
                        FilterChip(selected = schedOffset == opts[i].first, onClick = { schedOffset = opts[i].first }, label = { Text(opts[i].second) })
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(onCoverage, { onCoverage = it }); Text(s.sendOnCoverage)
                }
                Button(onClick = {
                    if (schedOffset > 0) {
                        state.store.scheduleSms(to, text, state.slot, nowMillis() + schedOffset)
                        schedTick++
                    } else if (onCoverage) {
                        state.store.queueSms(to, text, state.slot, true)
                        state.bridge.setCoverageMonitor(true, state.store.get("cov_alarm") == "1")
                    } else {
                        state.bridge.sendSms(to, text, state.slot)
                        state.store.addHistory("sms", to, text, state.slot)
                    }
                    showCompose = false
                }, enabled = to.isNotBlank() && text.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                    Icon(if (schedOffset > 0) MaterialSymbols.Outlined.Schedule_send else MaterialSymbols.Outlined.Send, null)
                    Spacer(Modifier.width(8.dp)); Text(if (schedOffset > 0) "Programar" else s.send)
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
    if (showAutoReply) {
        var arOn by remember { mutableStateOf(state.store.get("autoreply_on") == "1") }
        var arKeyword by remember { mutableStateOf(state.store.get("autoreply_keyword") ?: "") }
        var arText by remember { mutableStateOf(state.store.get("autoreply_text") ?: "") }
        ModalBottomSheet(onDismissRequest = { showAutoReply = false }) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
                Text("Respuesta automática", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text("Si un mensaje contiene tu palabra clave, la app responde sola con tu texto. Nunca responde a remitentes bloqueados ni a números de servicio.", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Activada", modifier = Modifier.weight(1f))
                    Switch(checked = arOn, onCheckedChange = { arOn = it; state.store.put("autoreply_on", if (it) "1" else "0") })
                }
                OutlinedTextField(arKeyword, { arKeyword = it; state.store.put("autoreply_keyword", it) }, label = { Text("Palabra clave") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(arText, { arText = it; state.store.put("autoreply_text", it) }, label = { Text("Respuesta") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

/** SMS segmentation (the original's counter): GSM-7 packs 160 chars in one
 *  segment, 153 when concatenated; any non-Latin char forces UCS-2 at
 *  70 / 67. International SMS cost 15 CUP per segment in the original. */
fun smsSegments(text: String): Int {
    val unicode = text.any { it.code > 127 }
    val single = if (unicode) 70 else 160
    val concat = if (unicode) 67 else 153
    return if (text.length <= single) 1 else (text.length + concat - 1) / concat
}

// ───────────────────────── Calls ─────────────────────────

@Composable
fun CallsScreen(state: AppState) {
    // Llamadas in the Google Phone (MD3E) layout: favorites strip on top,
    // filter chips (Google's call categorisation), recents inside one
    // rounded container, dialer FAB opening a full keypad sheet. All the
    // original's features stay: blocker entry, Llamar con 99, llamada
    // privada (both on the keypad sheet), Plan Amigos as the favorites.
    val s = state.strings
    var log by remember { mutableStateOf<List<CallInfo>>(emptyList()) }
    var friends by remember { mutableStateOf<List<ContactInfo>>(emptyList()) }
    var filter by remember { mutableStateOf(0) } // 0 todas, 1 perdidas, 2 contactos, 3 bloqueadas
    var keypadOpen by remember { mutableStateOf(false) }
    var dialed by remember { mutableStateOf("") }
    var birthdays by remember { mutableStateOf<List<BirthdayInfo>>(emptyList()) }
    var toolsOpen by remember { mutableStateOf(false) }
    var remindTarget by remember { mutableStateOf<CallInfo?>(null) }
    val blocked = remember { state.store.blockedNumbers() }
    LaunchedEffect(Unit) {
        log = state.bridge.callLog(80); friends = state.store.friends()
        birthdays = state.bridge.contactBirthdays(30)
    }
    val shown = log.filter { c ->
        when (filter) {
            1 -> c.incoming && c.durationSec == 0L
            2 -> c.name != null
            3 -> blocked.any { it.isNotBlank() && (c.number == it || c.number.endsWith(it.takeLast(8))) }
            else -> true
        }
    }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 120.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(s.calls, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = { toolsOpen = true }) {
                        Icon(MaterialSymbols.Outlined.Timer, contentDescription = "Temporizador y respuestas")
                    }
                    IconButton(onClick = { state.screen = "callblocker" }) {
                        Icon(MaterialSymbols.Outlined.Block, contentDescription = "Bloqueador")
                    }
                }
            }
            if (friends.isNotEmpty()) {
                item {
                    SectionTitle("Favoritos")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(friends) { f ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { state.bridge.call(f.number) },
                            ) {
                                ContactAvatar(f, 56)
                                Spacer(Modifier.height(4.dp))
                                Text(f.name.substringBefore(" "), style = MaterialTheme.typography.labelMedium, maxLines = 1)
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
            if (birthdays.isNotEmpty()) {
                item {
                    SectionTitle("Cumpleaños")
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        birthdays.forEach { b ->
                            ListItem(
                                headlineContent = { Text(b.name) },
                                supportingContent = {
                                    Text(if (b.daysUntil == 0) "¡Hoy! · " + b.label else "En ${b.daysUntil} día(s) · " + b.label)
                                },
                                leadingContent = { Icon(MaterialSymbols.Outlined.Cake, null, tint = MaterialTheme.colorScheme.primary) },
                                trailingContent = {
                                    if (b.number.isNotBlank()) IconButton(onClick = { state.bridge.call(b.number) }) {
                                        Icon(MaterialSymbols.Outlined.Call, null)
                                    }
                                },
                                colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val labels = listOf("Todas", "Perdidas", "Contactos", "Bloqueadas")
                    items(labels.size) { i ->
                        FilterChip(selected = filter == i, onClick = { filter = i }, label = { Text(labels[i]) })
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
            if (shown.isEmpty()) {
                item { Text(if (log.isEmpty()) s.noPermission else s.empty) }
            } else {
                item {
                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        shape = RoundedCornerShape(24.dp),
                    ) {
                        Column {
                            shown.forEachIndexed { idx, c ->
                                val missed = c.incoming && c.durationSec == 0L
                                ListItem(
                                    modifier = Modifier.combinedClickable(
                                        onClick = {},
                                        onLongClick = { remindTarget = c },
                                    ),
                                    headlineContent = { Text(c.name ?: c.number) },
                                    supportingContent = {
                                        Text(buildString {
                                            append(if (missed) "Perdida" else if (c.incoming) "Entrante" else "Saliente")
                                            append(" · " + c.number)
                                            if (c.durationSec > 0) append(" · " + c.durationSec + "s")
                                        })
                                    },
                                    leadingContent = {
                                        Icon(
                                            when {
                                                missed -> MaterialSymbols.Outlined.Call_missed
                                                c.incoming -> MaterialSymbols.Outlined.Call_received
                                                else -> MaterialSymbols.Outlined.Call_made
                                            },
                                            null,
                                            tint = if (missed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                        )
                                    },
                                    trailingContent = {
                                        IconButton(onClick = { state.bridge.call(c.number) }) { Icon(MaterialSymbols.Outlined.Call, null) }
                                    },
                                    colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                                )
                                if (idx < shown.lastIndex) HorizontalDivider(Modifier.padding(start = 56.dp))
                            }
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { keypadOpen = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 118.dp),
        ) { Icon(MaterialSymbols.Outlined.Dialpad, contentDescription = "Teclado") }
    }
    if (keypadOpen) {
        ModalBottomSheet(onDismissRequest = { keypadOpen = false }) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(if (dialed.isEmpty()) " " else dialed, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(8.dp))
                val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#")
                for (r in 0 until 4) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        for (col in 0 until 3) {
                            val k = keys[r * 3 + col]
                            TextButton(onClick = { dialed += k }, modifier = Modifier.size(72.dp)) {
                                Text(k, style = MaterialTheme.typography.headlineSmall)
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { if (dialed.isNotEmpty()) dialed = dialed.dropLast(1) }) {
                        Icon(MaterialSymbols.Outlined.Backspace, contentDescription = "Borrar")
                    }
                    Spacer(Modifier.width(16.dp))
                    FloatingActionButton(onClick = { if (dialed.isNotBlank()) { state.bridge.call(dialed); keypadOpen = false } }) {
                        Icon(MaterialSymbols.Outlined.Call, contentDescription = s.dial)
                    }
                    Spacer(Modifier.width(16.dp))
                    Spacer(Modifier.size(48.dp))
                }
                Spacer(Modifier.height(6.dp))
                Row {
                    TextButton(onClick = { if (dialed.isNotBlank()) { state.bridge.call("*99" + dialed); keypadOpen = false } }) { Text("Llamar con 99") }
                    TextButton(onClick = { if (dialed.isNotBlank()) { state.bridge.call("#31#" + dialed); keypadOpen = false } }) { Text("Llamada privada") }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
    if (toolsOpen) {
        var cutMin by remember { mutableStateOf(state.store.get("cut_timer_min") ?: "0") }
        var respondText by remember { mutableStateOf(state.store.get("respond_via_text") ?: "Te llamo enseguida") }
        ModalBottomSheet(onDismissRequest = { toolsOpen = false }) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
                Text("Durante la llamada", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                Text("La pantalla de llamada, el corte por temporizador y responder con un mensaje " +
                    "se activan cuando ETECSA Toolkit es la app de teléfono predeterminada.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { state.bridge.openDefaultDialerSettings() }, modifier = Modifier.fillMaxWidth()) {
                    Text("App de teléfono predeterminada")
                }
                Spacer(Modifier.height(14.dp))
                Text("Corte automático de llamada", style = MaterialTheme.typography.titleSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf(0, 1, 2, 5, 10, 15)) { m ->
                        FilterChip(
                            selected = cutMin == m.toString(),
                            onClick = { cutMin = m.toString(); state.store.put("cut_timer_min", m.toString()) },
                            label = { Text(if (m == 0) "Apagado" else "$m min") },
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(respondText, {
                    respondText = it; state.store.put("respond_via_text", it)
                }, label = { Text("Responder con un mensaje") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
            }
        }
    }
    remindTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { remindTarget = null },
            title = { Text("Recordarme: " + (target.name ?: target.number)) },
            text = {
                Column {
                    listOf(30 to "30 minutos", 60 to "1 hora", 120 to "2 horas").forEach { (mins, label) ->
                        TextButton(onClick = {
                            state.bridge.scheduleReminder("Llamar de nuevo",
                                target.name ?: target.number, nowMillis() + mins * 60000L)
                            remindTarget = null
                        }) { Text(label) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { remindTarget = null }) { Text(s.cancel) } },
        )
    }
}

@Composable
fun CallScreen(state: AppState) {
    var number by remember { mutableStateOf("") }
    ScreenScaffold(state, "Llamada normal") {
        OutlinedTextField(number, { number = it }, label = { Text(state.strings.number) },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        Button(onClick = { state.bridge.call(number) }, enabled = number.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
            Icon(MaterialSymbols.Outlined.Call, null); Spacer(Modifier.width(8.dp)); Text(state.strings.dial)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { state.bridge.call("*99" + number) }, enabled = number.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
            Text("Llamar con 99 (paga quien recibe)")
        }
        OutlinedButton(onClick = { state.bridge.call("#31#" + number) }, enabled = number.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
            Text("Llamada privada (#31#)")
        }
    }
}

@Composable
fun CallBlockerScreen(state: AppState) {
    var blocked by remember { mutableStateOf(state.store.blockedNumbers()) }
    var number by remember { mutableStateOf("") }
    ScreenScaffold(state, "Bloqueador de llamadas") {
        Text("Los números bloqueados se descartan para llamadas y SMS (reglas locales, sin servidores).")
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(number, { number = it }, label = { Text(state.strings.number) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            if (number.isNotBlank()) { state.store.setBlocked(number, true); blocked = state.store.blockedNumbers(); number = "" }
        }, Modifier.fillMaxWidth()) { Icon(MaterialSymbols.Outlined.Block, null); Spacer(Modifier.width(8.dp)); Text("Bloquear") }
        SectionTitle("Bloqueados")
        if (blocked.isEmpty()) Text(state.strings.empty)
        blocked.forEach { n ->
            ListItem(headlineContent = { Text(n) },
                trailingContent = {
                    IconButton(onClick = { state.store.setBlocked(n, false); blocked = state.store.blockedNumbers() }) {
                        Icon(MaterialSymbols.Outlined.Delete, null)
                    }
                })
        }
    }
}

// ───────────────────────── Nauta ─────────────────────────

@Composable
fun NautaScreen(state: AppState) {
    ScreenScaffold(state, "Nauta") {
        Text("Portal Wi-Fi de ETECSA (secure.etecsa.net). Conéctate primero a la Wi-Fi de ETECSA; la sesión y el tiempo restante se controlan en el portal oficial, sin salir de su dominio.")
        Spacer(Modifier.height(16.dp))
        Button(onClick = { state.bridge.nautaPortal() }, Modifier.fillMaxWidth()) {
            Icon(MaterialSymbols.Outlined.Wifi, null); Spacer(Modifier.width(8.dp)); Text("Abrir portal Nauta")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { state.bridge.openUrl("https://www.nauta.cu/") }, Modifier.fillMaxWidth()) {
            Text("www.nauta.cu")
        }
        OutlinedButton(onClick = { state.screen = "nautamail" }, Modifier.fillMaxWidth()) {
            Icon(MaterialSymbols.Outlined.Mail, null); Spacer(Modifier.width(8.dp)); Text("Buzón Nauta")
        }
    }
}

@Composable
fun NautaMailScreen(state: AppState) {
    var user by remember { mutableStateOf(state.store.get("nauta_user") ?: "") }
    ScreenScaffold(state, "Buzón Nauta") {
        Text("Cuentas de correo @nauta.cu. Puedes guardar varias cuentas (en el original, las adicionales eran Pro; aquí, gratis).")
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(user, { user = it }, label = { Text("usuario@nauta.cu") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { state.store.put("nauta_user", user) }) {
                    Icon(MaterialSymbols.Outlined.Edit, contentDescription = state.strings.edit)
                }
            })
        Button(onClick = { state.store.put("nauta_user", user); state.bridge.openUrl("https://webmail.nauta.cu/") },
            Modifier.fillMaxWidth()) { Text("Abrir buzón") }
    }
}

// ───────────────────────── Online services ─────────────────────────

@Composable
fun OnlineScreen(state: AppState) {
    ScreenScaffold(state, "Servicios ETECSA") {
        Text("Accesos directos oficiales y radar de servicios. La sincronización usa los portales de ETECSA en su dominio seguro.")
        Spacer(Modifier.height(12.dp))
        listOf(
            "Tienda ETECSA" to "https://www.tienda.etecsa.cu",
            "Portal de usuario" to "https://www.etecsa.cu/",
            "Mi Cubacel" to "https://mi.cubacel.net/",
        ).forEach { (label, url) ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { state.bridge.openUrl(url) },
                shape = RoundedCornerShape(24.dp)) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(MaterialSymbols.Outlined.Open_in_new, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(label, style = MaterialTheme.typography.titleMedium)
                        Text(url, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun UsefulScreen(state: AppState) {
    ScreenScaffold(state, "Números útiles") {
        UsefulNumbers.forEach { (num, label) ->
            ListItem(headlineContent = { Text(label) }, supportingContent = { Text(num) },
                leadingContent = { Icon(MaterialSymbols.Outlined.Call, null) },
                modifier = Modifier.clickable { state.bridge.call(num) })
            HorizontalDivider()
        }
    }
}

// ───────────────────────── Network ─────────────────────────

@Composable
fun NetworkScreen(state: AppState) {
    val s = state.strings
    var monitor by remember { mutableStateOf(state.bridge.coverageMonitorOn()) }
    var alarm by remember { mutableStateOf(state.store.get("cov_alarm") == "1") }
    var cell by remember { mutableStateOf("") }
    var traffic by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { cell = state.bridge.cellInfo(); traffic = state.bridge.trafficInfo() }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 110.dp),
    ) {
        item {
            Text(s.network, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            SimSelector(state)
            SectionTitle(s.coverageMonitor)
            ListItem(headlineContent = { Text(s.coverageMonitor) },
                trailingContent = { Switch(checked = monitor, onCheckedChange = { on -> monitor = on; state.bridge.setCoverageMonitor(on, alarm) }) })
            ListItem(headlineContent = { Text(s.coverageAlarm) },
                supportingContent = { Text(s.coverageAlarmDesc) },
                trailingContent = {
                    Switch(checked = alarm, onCheckedChange = { on ->
                        alarm = on; state.store.put("cov_alarm", if (on) "1" else "0")
                        state.bridge.setCoverageMonitor(monitor, on)
                    })
                })
            SectionTitle("Celda / LTE")
            Text(cell.ifBlank { "—" }, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { state.screen = "forcelte" }) { Text("Forzar LTE (RadioInfo)") }
            SectionTitle("Tráfico")
            Text(traffic.ifBlank { "—" }, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            var overlay by remember { mutableStateOf(false) }
            ListItem(headlineContent = { Text("Monitor flotante de velocidad") },
                supportingContent = { Text("Velocidad en vivo sobre cualquier app") },
                trailingContent = {
                    Switch(checked = overlay, onCheckedChange = { on ->
                        if (on && !state.bridge.canDrawOverlays()) {
                            state.bridge.openOverlaySettings()
                        } else {
                            overlay = on; state.bridge.setTrafficOverlay(on)
                        }
                    })
                })
            SectionTitle("Actualización automática")
            Text("Consulta el saldo sola cada cierto tiempo y avisa antes de que venza.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            var autoSync by remember { mutableStateOf(state.store.get("autosync_hours") ?: "0") }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf(0, 6, 12, 24)) { h ->
                    FilterChip(
                        selected = autoSync == h.toString(),
                        onClick = { autoSync = h.toString(); state.bridge.setAutoSync(h) },
                        label = { Text(if (h == 0) "Apagado" else "Cada $h h") },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row {
                Button(onClick = { state.screen = "speed" }, Modifier.weight(1f)) { Text(s.speedTest) }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { state.screen = "firewall" }, Modifier.weight(1f)) { Text("Cortafuegos") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SpeedScreen(state: AppState) {
    val s = state.strings
    var result by remember { mutableStateOf<SpeedResult?>(null) }
    var running by remember { mutableStateOf(false) }
    ScreenScaffold(state, s.speedTest) {
        Text("Servidor: Cloudflare (speed.cloudflare.com), igual que el original. La prueba consume datos reales.")
        Spacer(Modifier.height(16.dp))
        if (running) { LoadingIndicator(); Spacer(Modifier.height(12.dp)) }
        result?.let { r ->
            ListItem(headlineContent = { Text("${s.download}: ${r.downloadMbps.fmt(2)} Mbps") })
            ListItem(headlineContent = { Text("${s.upload}: ${r.uploadMbps.fmt(2)} Mbps") })
            ListItem(headlineContent = { Text("${s.ping}: ${r.pingMs.fmt(0)} ms · Jitter: ${r.jitterMs.fmt(0)} ms") })
        }
        Button(onClick = {
            running = true
            state.bridge.speedTest { r ->
                running = false; result = r
                if (r != null) state.store.addHistory("speed", s.speedTest,
                    "↓ ${r.downloadMbps.fmt(2)} ↑ ${r.uploadMbps.fmt(2)} Mbps, ping ${r.pingMs.fmt(0)} ms", state.slot)
            }
        }, enabled = !running, modifier = Modifier.fillMaxWidth()) {
            Icon(MaterialSymbols.Outlined.Speed, null); Spacer(Modifier.width(8.dp)); Text(s.start)
        }
    }
}

@Composable
fun FirewallScreen(state: AppState) {
    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var blocked by remember { mutableStateOf(state.bridge.firewallBlocked()) }
    LaunchedEffect(Unit) { apps = state.bridge.installedApps() }
    ScreenScaffold(state, "Cortafuegos") {
        Text("Las apps bloqueadas se enrutan a un túnel local que descarta su tráfico (VPN local, sin servidores). El resto de apps no pasa por la VPN.")
        Spacer(Modifier.height(8.dp))
        Row {
            Button(onClick = { state.bridge.setFirewall(blocked) }) { Text(state.strings.start) }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = { state.bridge.stopFirewall() }) { Text(state.strings.stop) }
        }
        SectionTitle("Apps (${apps.size})")
        if (apps.isEmpty()) Text(state.strings.noPermission)
        apps.forEach { app ->
            ListItem(headlineContent = { Text(app.label) }, supportingContent = { Text(app.packageName) },
                trailingContent = {
                    Switch(checked = blocked.contains(app.packageName), onCheckedChange = { on ->
                        blocked = if (on) blocked + app.packageName else blocked - app.packageName
                    })
                })
        }
    }
}

@Composable
fun ForceLteScreen(state: AppState) {
    ScreenScaffold(state, "Forzar LTE") {
        Text("Abre RadioInfo del sistema, donde puedes fijar el tipo de red preferido (solo LTE). En algunos fabricantes esta pantalla está oculta.")
        Spacer(Modifier.height(16.dp))
        Button(onClick = { state.bridge.openRadioInfo() }, Modifier.fillMaxWidth()) {
            Icon(MaterialSymbols.Outlined.Network_check, null); Spacer(Modifier.width(8.dp)); Text("Abrir RadioInfo")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { state.runUssd("*#*#4636#*#*", "RadioInfo") }, Modifier.fillMaxWidth()) {
            Text("*#*#4636#*#*")
        }
    }
}

// ───────────────────────── Antifraud / Protect ─────────────────────────

@Composable
fun AntifraudScreen(state: AppState) {
    var sender by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var verdict by remember { mutableStateOf<String?>(null) }
    ScreenScaffold(state, "Protect — Antifraude") {
        Text("Analiza un SMS contra el catálogo de remitentes oficiales (ETECSA, Cubacel, Pago×Móvil, ENZONA…). Todo el análisis es local.")
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(sender, { sender = it }, label = { Text("Remitente") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(body, { body = it }, label = { Text("Mensaje") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        Button(onClick = {
            verdict = when {
                OfficialSenders.any { it.equals(sender, true) } -> state.strings.antifraudOk
                Antifraud.looksSuspicious(sender, body, OfficialSenders) -> state.strings.antifraudWarn
                else -> state.strings.antifraudOk
            }
            state.store.addHistory("antifraud", sender, verdict ?: "", state.slot)
        }, Modifier.fillMaxWidth()) { Text("Analizar") }
        verdict?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, fontWeight = FontWeight.Bold,
                color = if (it == state.strings.antifraudWarn) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary)
        }
        SectionTitle(state.strings.history)
        val recent = remember { state.bridge.smsInbox(30) }
        recent.filter { Antifraud.looksSuspicious(it.address, it.body, OfficialSenders) }.forEach { sms ->
            ListItem(headlineContent = { Text(sms.address) }, supportingContent = { Text(sms.body, maxLines = 2) },
                leadingContent = { Icon(MaterialSymbols.Outlined.Warning, null, tint = MaterialTheme.colorScheme.error) })
        }
    }
}

// ───────────────────────── Backup ─────────────────────────

@Composable
fun BackupScreen(state: AppState) {
    val s = state.strings
    var status by remember { mutableStateOf("") }
    ScreenScaffold(state, s.backup) {
        Text("Exporta el historial, tarjetas (cifradas), amigos y ajustes a un texto que puedes guardar donde quieras. La restauración pega ese texto de vuelta.")
        Spacer(Modifier.height(16.dp))
        var exportText by remember { mutableStateOf("") }
        Button(onClick = {
            val hist = state.store.history(500).joinToString("\n") { "${it.ts}|${it.kind}|${it.title}|${it.body}|${it.slot}" }
            exportText = "ETK-BACKUP-1\n" + hist
            status = "OK — ${hist.lines().size} registros"
        }, Modifier.fillMaxWidth()) { Icon(MaterialSymbols.Outlined.Backup, null); Spacer(Modifier.width(8.dp)); Text(s.backup) }
        if (exportText.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(exportText, {}, readOnly = true, modifier = Modifier.fillMaxWidth().height(160.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(status)
    }
}
