package dev.kurokawa.etecsa.toolkit.services

import android.os.Bundle
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.VideoProfile
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.kurokawa.etecsa.toolkit.theme.EtecsaToolkitTheme
import kotlinx.coroutines.delay

/** Google-Phone-style in-call screen: identity up top, elapsed time, big
 *  pill controls. Ringing calls get Answer / Decline / respond-via chips. */
class InCallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        val prefs = getSharedPreferences("etk_kv", MODE_PRIVATE)
        val dark = prefs.getString("dark", "0") == "1"
        val respondText = prefs.getString("respond_via_text", "Te llamo enseguida") ?: "Te llamo enseguida"
        setContent {
            EtecsaToolkitTheme(darkMode = dark) {
                Surface(Modifier.fillMaxSize()) { InCallScreen(respondText) { finish() } }
            }
        }
    }
}

@Composable
private fun InCallScreen(respondText: String, onDone: () -> Unit) {
    val call = EtkInCallService.currentCall
    if (call == null) { LaunchedEffect(Unit) { onDone() }; return }
    var state by remember { mutableStateOf(call.state) }
    var muted by remember { mutableStateOf(EtkInCallService.instance?.callAudioState?.isMuted == true) }
    var speaker by remember { mutableStateOf(
        EtkInCallService.instance?.callAudioState?.route == CallAudioState.ROUTE_SPEAKER) }
    var elapsed by remember { mutableStateOf(0L) }
    DisposableEffect(call) {
        val cb = object : Call.Callback() {
            override fun onStateChanged(c: Call, s: Int) {
                state = s
                if (s == Call.STATE_DISCONNECTED) onDone()
            }
        }
        call.registerCallback(cb)
        onDispose { call.unregisterCallback(cb) }
    }
    LaunchedEffect(state) {
        while (state == Call.STATE_ACTIVE) {
            elapsed = (System.currentTimeMillis() - call.details.connectTimeMillis) / 1000
            delay(500)
        }
    }
    val number = call.details.handle?.schemeSpecificPart ?: ""
    val name = try { call.details.callerDisplayName } catch (_: Exception) { null }
    val ringing = state == Call.STATE_RINGING
    val service = EtkInCallService.instance

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(48.dp))
            Text(name?.takeIf { it.isNotBlank() } ?: number.ifBlank { "Llamada" },
                style = MaterialTheme.typography.headlineMedium)
            Text(number, style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Text(
                when {
                    ringing -> "Llamada entrante"
                    state == Call.STATE_ACTIVE ->
                        "%d:%02d".format(elapsed / 60, elapsed % 60)
                    state == Call.STATE_DIALING || state == Call.STATE_CONNECTING -> "Llamando…"
                    else -> "Llamada"
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        if (ringing) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Responder con un mensaje", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(respondText, "Estoy ocupado", "Te escribo ahora").distinct().forEach { msg ->
                        AssistChip(onClick = {
                            try { call.reject(true, msg) } catch (_: Exception) {}
                            onDone()
                        }, label = { Text(msg) })
                    }
                }
                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Button(
                        onClick = { try { call.reject(false, null) } catch (_: Exception) {}; onDone() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier.height(56.dp).weight(1f).padding(end = 8.dp),
                    ) { Text("Rechazar") }
                    Button(
                        onClick = { try { call.answer(VideoProfile.STATE_AUDIO_ONLY) } catch (_: Exception) {} },
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier.height(56.dp).weight(1f).padding(start = 8.dp),
                    ) { Text("Contestar") }
                }
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilledTonalButton(onClick = {
                        muted = !muted; try { service?.setMuted(muted) } catch (_: Exception) {}
                    }, shape = RoundedCornerShape(24.dp)) { Text(if (muted) "Activar mic" else "Silenciar") }
                    FilledTonalButton(onClick = {
                        speaker = !speaker
                        try {
                            service?.setAudioRoute(
                                if (speaker) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_EARPIECE)
                        } catch (_: Exception) {}
                    }, shape = RoundedCornerShape(24.dp)) { Text(if (speaker) "Auricular" else "Altavoz") }
                }
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { try { call.disconnect() } catch (_: Exception) {}; onDone() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.height(56.dp).fillMaxWidth(),
                ) { Text("Finalizar llamada") }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
