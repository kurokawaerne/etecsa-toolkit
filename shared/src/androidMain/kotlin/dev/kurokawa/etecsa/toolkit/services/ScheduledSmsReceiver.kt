package dev.kurokawa.etecsa.toolkit.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import dev.kurokawa.etecsa.toolkit.platform.EtkStore

/** Fires a scheduled SMS at its alarm time: loads the row, sends on the
 *  stored SIM slot (same slot→subscription mapping as the bridge), removes
 *  the row, and leaves a history trace + a quiet notification. */
class ScheduledSmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra("sched_id", -1)
        if (id < 0) return
        try {
            val store = EtkStore(context.applicationContext)
            val entry = store.scheduledSms().firstOrNull { it.id == id } ?: return
            val sent = sendSms(context, entry.number, entry.body, entry.slot)
            store.cancelScheduledSms(id)
            store.addHistory("sms_scheduled", entry.number,
                (if (sent) "Enviado: " else "FALLÓ el envío: ") + entry.body, entry.slot)
            notify(context, entry.number, sent)
        } catch (_: Exception) {}
    }

    private fun sendSms(context: Context, number: String, text: String, slot: Int): Boolean = try {
        val subId = subIdForSlot(context, slot)
        val mgr: SmsManager = when {
            Build.VERSION.SDK_INT >= 31 && subId >= 0 ->
                context.getSystemService(SmsManager::class.java).createForSubscriptionId(subId)
            else -> @Suppress("DEPRECATION") SmsManager.getDefault()
        }
        val parts = mgr.divideMessage(text)
        if (parts.size > 1) mgr.sendMultipartTextMessage(number, null, parts, null, null)
        else mgr.sendTextMessage(number, null, text, null, null)
        true
    } catch (_: Exception) { false }

    private fun subIdForSlot(context: Context, slot: Int): Int = try {
        val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
        @Suppress("MissingPermission")
        val infos = sm.activeSubscriptionInfoList ?: emptyList()
        infos.firstOrNull { it.simSlotIndex == slot }?.subscriptionId ?: -1
    } catch (_: Exception) { -1 }

    private fun notify(context: Context, number: String, sent: Boolean) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(NotificationChannel("etk_sms", "Mensajes",
                NotificationManager.IMPORTANCE_DEFAULT))
        }
        val n = Notification.Builder(context, "etk_sms")
            .setContentTitle(if (sent) "Mensaje programado enviado" else "No se pudo enviar el mensaje programado")
            .setContentText(number)
            .setSmallIcon(android.R.drawable.sym_action_email)
            .setAutoCancel(true).build()
        nm.notify((System.currentTimeMillis() % 100000).toInt(), n)
    }
}
