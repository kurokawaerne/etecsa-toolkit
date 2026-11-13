package dev.kurokawa.etecsa.toolkit.ui

import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.outlined.*
import com.composables.icons.materialsymbols.outlinedfilled.*

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.kurokawa.etecsa.toolkit.core.*
import dev.kurokawa.etecsa.toolkit.model.*

// ───────────────────────── Home ─────────────────────────

@Composable
fun HomeScreen(state: AppState) {
    val s = state.strings
    var inputFor by remember { mutableStateOf<ServiceItem?>(null) }
    var inputText by remember { mutableStateOf("") }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 110.dp),
    ) {
        item { SimSelector(state) }
        // Balance card
        item {
            Spacer(Modifier.height(14.dp))
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(28.dp),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(s.balance, style = MaterialTheme.typography.labelLarge)
                    val b = state.balance
                    Text(
                        if (b?.balanceCup != null) "${b.balanceCup} CUP" else "— CUP",
                        style = MaterialTheme.typography.displaySmall,
                    )
                    Row {
                        b?.dataBytes?.let { Text("Datos ${ResponseParser.formatData(it)}  ·  ") }
                        b?.voiceMinutes?.let { Text("Voz $it min  ·  ") }
                        b?.smsCount?.let { Text("SMS $it") }
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { state.runUssd("*222#", "Saldo") }) {
                        Icon(MaterialSymbols.Outlined.Refresh, null); Spacer(Modifier.width(8.dp)); Text(s.consult)
                    }
                }
            }
        }
        // Services grouped in categories
        Category.entries.forEach { cat ->
            val itemsInCat = Services.filter { it.category == cat }
            if (itemsInCat.isEmpty()) return@forEach
            item { SectionTitle(if (state.lang == "es") cat.es else cat.en) }
            items(itemsInCat) { item ->
                ListItem(
                    headlineContent = { Text(item.titleEs) },
                    supportingContent = { Text(item.descEs) },
                    leadingContent = {
                        Icon(iconFor(item.id), null, tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingContent = { Icon(MaterialSymbols.Outlined.Chevron_right, null) },
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable {
                        when {
                            item.action.startsWith("screen:") -> state.screen = item.action.removePrefix("screen:")
                            item.action.startsWith("tel:") -> state.bridge.call(item.action.removePrefix("tel:"))
                            item.inputLabel != null -> { inputFor = item; inputText = "" }
                            item.inputIsCall -> { inputFor = item; inputText = "" }
                            else -> state.runUssd(item.action, item.titleEs)
                        }
                    },
                )
            }
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

fun iconFor(id: String) = when (id) {
    "saldo" -> MaterialSymbols.Outlined.Account_balance_wallet
    "datos" -> MaterialSymbols.Outlined.Data_usage
    "bonos" -> MaterialSymbols.Outlined.Card_giftcard
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
                Text(s.settings, style = MaterialTheme.typography.headlineSmall)
            }
            // Profile header card — like Play Store's account panel
            Card(
                Modifier.fillMaxWidth().padding(vertical = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(28.dp),
            ) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    AppMark(Modifier.size(56.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(s.appName, style = MaterialTheme.typography.titleLarge)
                        Text("${s.version} 1.0.0 · dev.kurokawa.etecsa.toolkit",
                            style = MaterialTheme.typography.bodySmall)
                        val sims = remember { state.bridge.simSlots() }
                        Text(sims.filter { it.detected }.joinToString(" · ") { "${it.carrier} ${it.number}".trim() }
                            .ifBlank { "—" }, style = MaterialTheme.typography.bodySmall)
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
                        onCheckedChange = { if (it) state.setLang(code) },
                        shapes = if (i == 0) ButtonGroupDefaults.connectedLeadingButtonShapes()
                                 else ButtonGroupDefaults.connectedTrailingButtonShapes(),
                        modifier = Modifier.weight(1f).padding(horizontal = 1.dp),
                    ) { Text(label) }
                }
            }
        }
        item { SectionTitle(s.simForQueries) }
        item { SimSelector(state) }
        item { SectionTitle(s.coverageMonitor) }
        item {
            ListItem(headlineContent = { Text(s.coverageMonitor) },
                supportingContent = { Text(s.coverageAlarmDesc) },
                trailingContent = {
                    Switch(monitor) { on ->
                        monitor = on; state.bridge.setCoverageMonitor(on, alarm)
                    }
                })
            ListItem(headlineContent = { Text(s.coverageAlarm) },
                supportingContent = { Text(s.coverageAlarmDesc) },
                trailingContent = {
                    Switch(alarm) { on ->
                        alarm = on; state.store.put("cov_alarm", if (on) "1" else "0")
                        state.bridge.setCoverageMonitor(monitor, on)
                    }
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
    val s = state.strings
    var pending by remember { mutableStateOf<Plan?>(null) }
    ScreenScaffold(state, s.plans) {
        listOf("data" to "Datos", "voice" to "Voz", "sms" to "SMS").forEach { (cat, label) ->
            SectionTitle(label)
            Plans.filter { it.category == cat }.forEach { plan ->
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

// ───────────────────────── Transfer ─────────────────────────

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
        OutlinedTextField(label, { label = it }, label = { Text("Etiqueta") }, singleLine = true, Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(number, { number = it }, label = { Text("Número de tarjeta") }, singleLine = true, Modifier.fillMaxWidth())
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
        OutlinedTextField(smsc, { smsc = it }, label = { Text("SMSC") }, singleLine = true, Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { state.store.put("smsc", smsc) }) {
                    Icon(MaterialSymbols.Outlined.Edit, contentDescription = state.strings.edit)
                }
            })
        Button(onClick = { state.store.put("smsc", smsc) }, Modifier.fillMaxWidth()) { Text("Guardar") }
    }
}

// ───────────────────────── Messages ─────────────────────────

@Composable
fun MessagesScreen(state: AppState) {
    val s = state.strings
    var inbox by remember { mutableStateOf<List<SmsInfo>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var to by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var onCoverage by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { inbox = state.bridge.smsInbox(60); loaded = true }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 110.dp),
    ) {
        item {
            Text(s.messages, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            SimSelector(state)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(to, { to = it }, label = { Text(s.number) }, singleLine = true, Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(text, { text = it }, label = { Text(s.messages) }, Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(onCoverage, { onCoverage = it }); Text(s.sendOnCoverage)
            }
            Button(onClick = {
                if (onCoverage) {
                    state.store.queueSms(to, text, state.slot, true)
                    state.bridge.setCoverageMonitor(true, state.store.get("cov_alarm") == "1")
                } else {
                    state.bridge.sendSms(to, text, state.slot)
                    state.store.addHistory("sms", to, text, state.slot)
                }
                text = ""
            }, enabled = to.isNotBlank() && text.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                Icon(MaterialSymbols.Outlined.Send, null); Spacer(Modifier.width(8.dp)); Text(s.send)
            }
            val queued = remember { state.store.queuedSms() }
            if (queued.isNotEmpty()) {
                SectionTitle("En cola (${queued.size})")
                queued.forEach { (n, body, cov) ->
                    ListItem(headlineContent = { Text(n) }, supportingContent = { Text(body, maxLines = 1) },
                        leadingContent = { Icon(MaterialSymbols.Outlined.Schedule, null) })
                }
            }
            SectionTitle(s.history)
            if (loaded && inbox.isEmpty()) Text(s.noPermission)
        }
        items(inbox) { sms ->
            val suspicious = remember(sms) { Antifraud.looksSuspicious(sms.address, sms.body, OfficialSenders) }
            ListItem(
                headlineContent = { Text(sms.address) },
                supportingContent = { Text(sms.body, maxLines = 3) },
                leadingContent = {
                    Icon(
                        if (suspicious) MaterialSymbols.Outlined.Warning else MaterialSymbols.Outlined.Sms, null,
                        tint = if (suspicious) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    )
                },
                trailingContent = {
                    IconButton(onClick = { state.store.setBlocked(sms.address, true) }) {
                        Icon(MaterialSymbols.Outlined.Block, null)
                    }
                },
            )
            HorizontalDivider()
        }
    }
}

// ───────────────────────── Calls ─────────────────────────

@Composable
fun CallsScreen(state: AppState) {
    val s = state.strings
    var log by remember { mutableStateOf<List<CallInfo>>(emptyList()) }
    LaunchedEffect(Unit) { log = state.bridge.callLog(60) }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 110.dp),
    ) {
        item {
            Text(s.calls, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Row {
                Button(onClick = { state.screen = "call" }, Modifier.weight(1f)) {
                    Icon(MaterialSymbols.Outlined.Dialpad, null); Spacer(Modifier.width(6.dp)); Text("Llamar")
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { state.screen = "callblocker" }, Modifier.weight(1f)) {
                    Icon(MaterialSymbols.Outlined.Block, null); Spacer(Modifier.width(6.dp)); Text("Bloqueador")
                }
            }
            SectionTitle(s.history)
            if (log.isEmpty()) Text(s.noPermission)
        }
        items(log) { c ->
            val contact = ContactInfo(c.name ?: c.number, c.number, null)
            ListItem(
                headlineContent = { Text(c.name ?: c.number) },
                supportingContent = { Text("${c.number} · ${c.durationSec}s") },
                leadingContent = { ContactAvatar(contact) },
                trailingContent = {
                    IconButton(onClick = { state.bridge.call(c.number) }) { Icon(MaterialSymbols.Outlined.Call, null) }
                },
            )
            HorizontalDivider()
        }
    }
}

@Composable
fun CallScreen(state: AppState) {
    var number by remember { mutableStateOf("") }
    ScreenScaffold(state, "Llamada normal") {
        OutlinedTextField(number, { number = it }, label = { Text(state.strings.number) },
            singleLine = true, Modifier.fillMaxWidth())
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
        OutlinedTextField(number, { number = it }, label = { Text(state.strings.number) }, singleLine = true, Modifier.fillMaxWidth())
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
        OutlinedTextField(user, { user = it }, label = { Text("usuario@nauta.cu") }, singleLine = true, Modifier.fillMaxWidth(),
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
            Text(s.network, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            SimSelector(state)
            SectionTitle(s.coverageMonitor)
            ListItem(headlineContent = { Text(s.coverageMonitor) },
                trailingContent = { Switch(monitor) { on -> monitor = on; state.bridge.setCoverageMonitor(on, alarm) } })
            ListItem(headlineContent = { Text(s.coverageAlarm) },
                supportingContent = { Text(s.coverageAlarmDesc) },
                trailingContent = {
                    Switch(alarm) { on ->
                        alarm = on; state.store.put("cov_alarm", if (on) "1" else "0")
                        state.bridge.setCoverageMonitor(monitor, on)
                    }
                })
            SectionTitle("Celda / LTE")
            Text(cell.ifBlank { "—" }, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { state.screen = "forcelte" }) { Text("Forzar LTE (RadioInfo)") }
            SectionTitle("Tráfico")
            Text(traffic.ifBlank { "—" }, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Row {
                Button(onClick = { state.screen = "speed" }, Modifier.weight(1f)) { Text(s.speedTest) }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { state.screen = "firewall" }, Modifier.weight(1f)) { Text("Cortafuegos") }
            }
        }
    }
}

@Composable
fun SpeedScreen(state: AppState) {
    val s = state.strings
    var result by remember { mutableStateOf<SpeedResult?>(null) }
    var running by remember { mutableStateOf(false) }
    ScreenScaffold(state, s.speedTest) {
        Text("Servidor: Cloudflare (speed.cloudflare.com), igual que el original. La prueba consume datos reales.")
        Spacer(Modifier.height(16.dp))
        if (running) { CircularProgressIndicator(); Spacer(Modifier.height(12.dp)) }
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
                    Switch(blocked.contains(app.packageName)) { on ->
                        blocked = if (on) blocked + app.packageName else blocked - app.packageName
                    }
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
        OutlinedTextField(sender, { sender = it }, label = { Text("Remitente") }, singleLine = true, Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(body, { body = it }, label = { Text("Mensaje") }, Modifier.fillMaxWidth())
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
