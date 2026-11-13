package dev.kurokawa.etecsa.toolkit.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.*
import android.telephony.*
import dev.kurokawa.etecsa.toolkit.platform.EtkStore

/** Watches cellular service state. On a lost → restored transition it:
 *  - posts a notification (with bubble metadata, Android 10+),
 *  - if the alarm toggle is on, sounds the alarm tone + vibrates
 *    (Ernest's coverage-back alarm),
 *  - flushes SMS queued "send when coverage returns".
 *  Debounce: coverage must stay up 20 s before firing, so cell-edge
 *  flapping does not scream. */
class CoverageMonitorService : Service() {
    private var hadService: Boolean? = null
    private var pendingRestore: Runnable? = null
    private val handler = Handler(Looper.getMainLooper())
    private var callback: TelephonyCallback? = null
    private var listener: PhoneStateListener? = null

    override fun onBind(intent: Intent?) = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(41, baseNotification("Monitor de cobertura activo").build())
        val tm = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        if (Build.VERSION.SDK_INT >= 31) {
            val cb = object : TelephonyCallback(), TelephonyCallback.ServiceStateListener {
                override fun onServiceStateChanged(state: ServiceState) =
                    onState(state.state == ServiceState.STATE_IN_SERVICE)
            }
            callback = cb
            try { tm.registerTelephonyCallback(mainExecutor, cb) } catch (_: Exception) {}
        } else {
            @Suppress("DEPRECATION")
            val l = object : PhoneStateListener() {
                @Deprecated("Deprecated in Java")
                override fun onServiceStateChanged(state: ServiceState?) =
                    onState(state?.state == ServiceState.STATE_IN_SERVICE)
            }
            listener = l
            try { @Suppress("DEPRECATION") tm.listen(l, PhoneStateListener.LISTEN_SERVICE_STATE) } catch (_: Exception) {}
        }
    }

    private fun onState(inService: Boolean) {
        val prev = hadService
        hadService = inService
        if (prev == false && inService) {
            // Debounced restore
            pendingRestore?.let { handler.removeCallbacks(it) }
            val r = Runnable { onCoverageBack() }
            pendingRestore = r
            handler.postDelayed(r, 20_000)
        } else if (!inService) {
            pendingRestore?.let { handler.removeCallbacks(it) }
        }
    }

    private fun onCoverageBack() {
        val prefs = getSharedPreferences("etk_kv", Context.MODE_PRIVATE)
        val alarm = prefs.getBoolean("cov_alarm", false)
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notif = baseNotification("¡Volvió la cobertura celular!")
        // Bubble metadata (Android 10+): the alert can pop as a bubble.
        if (Build.VERSION.SDK_INT >= 29) {
            try {
                val bubbleIntent = PendingIntent.getActivity(this, 0,
                    packageManager.getLaunchIntentForPackage(packageName),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
                val bubble = Notification.BubbleMetadata.Builder(bubbleIntent,
                    android.graphics.drawable.Icon.createWithResource(this, android.R.drawable.stat_sys_phone_call))
                    .setDesiredHeight(400).build()
                notif.setBubbleMetadata(bubble)
            } catch (_: Exception) {}
        }
        nm.notify(42, notif.build())
        if (alarm) {
            try {
                val tone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                RingtoneManager.getRingtone(this, tone)?.play()
            } catch (_: Exception) {}
            try {
                val vib = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= 26)
                    vib.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400, 200, 800), -1))
                else @Suppress("DEPRECATION") vib.vibrate(1200)
            } catch (_: Exception) {}
        }
        // Flush queued SMS marked send-on-coverage
        try {
            val store = EtkStore(applicationContext)
            val queued = store.queuedSms()
            if (queued.isNotEmpty()) {
                val mgr = android.telephony.SmsManager.getDefault()
                queued.forEach { (number, body, _) ->
                    try { mgr.sendTextMessage(number, null, body, null, null) } catch (_: Exception) {}
                }
                store.clearQueuedSms()
                store.addHistory("sms", "Cola SMS", "Enviados ${queued.size} al volver la cobertura", 0)
            }
        } catch (_: Exception) {}
    }

    private fun baseNotification(text: String): Notification.Builder {
        val launch = packageManager.getLaunchIntentForPackage(packageName)
        val pi = PendingIntent.getActivity(this, 0, launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, CHANNEL)
            .setContentTitle("ETECSA Toolkit").setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_phone_call)
            .setContentIntent(pi).setAutoCancel(true)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "Cobertura",
                NotificationManager.IMPORTANCE_HIGH))
        }
    }

    override fun onDestroy() {
        try {
            val tm = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            if (Build.VERSION.SDK_INT >= 31) callback?.let { tm.unregisterTelephonyCallback(it) }
            else @Suppress("DEPRECATION") listener?.let { tm.listen(it, PhoneStateListener.LISTEN_NONE) }
        } catch (_: Exception) {}
        super.onDestroy()
    }

    companion object { const val CHANNEL = "etk_coverage" }
}
