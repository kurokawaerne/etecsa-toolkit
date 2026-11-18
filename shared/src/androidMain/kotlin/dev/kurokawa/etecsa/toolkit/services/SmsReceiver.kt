package dev.kurokawa.etecsa.toolkit.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import dev.kurokawa.etecsa.toolkit.core.Antifraud
import dev.kurokawa.etecsa.toolkit.model.OfficialSenders
import dev.kurokawa.etecsa.toolkit.platform.EtkStore

/** Incoming SMS: drops a history trace, and warns (notification) when the
 *  message smells like ETECSA/Cubacel/Pago×Móvil impersonation. All local. */
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        try {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
            val store = EtkStore(context.applicationContext)
            for (msg in messages) {
                val sender = msg.originatingAddress ?: continue
                val body = msg.messageBody ?: ""
                if (store.blockedNumbers().contains(sender)) {
                    store.addHistory("sms_blocked", sender, body, 0)
                    continue
                }
                if (Antifraud.looksSuspicious(sender, body, OfficialSenders)) {
                    store.addHistory("antifraud", sender, body, 0)
                    notify(context, sender, body)
                }
                maybeAutoReply(context, store, sender, body)
            }
        } catch (_: Exception) {}
    }

    /** The original's auto-reply: when enabled and the incoming body contains
     *  the user's keyword, answer once with their canned text. Settings live
     *  in plain store keys (autoreply_on / _keyword / _text), set from the
     *  Messages tab. Never replies to blocked or service senders. */
    private fun maybeAutoReply(context: Context, store: EtkStore, sender: String, body: String) {
        try {
            if (store.get("autoreply_on") != "1") return
            val keyword = store.get("autoreply_keyword")?.trim().orEmpty()
            val reply = store.get("autoreply_text")?.trim().orEmpty()
            if (keyword.isEmpty() || reply.isEmpty()) return
            if (!body.contains(keyword, ignoreCase = true)) return
            if (sender.length < 7) return // service/short senders: never auto-reply
            val mgr = @Suppress("DEPRECATION") android.telephony.SmsManager.getDefault()
            val parts = mgr.divideMessage(reply)
            if (parts.size > 1) mgr.sendMultipartTextMessage(sender, null, parts, null, null)
            else mgr.sendTextMessage(sender, null, reply, null, null)
            store.addHistory("sms_autoreply", sender, reply, 0)
        } catch (_: Exception) {}
    }

    private fun notify(context: Context, sender: String, body: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(NotificationChannel("etk_fraud", "Antifraude",
                NotificationManager.IMPORTANCE_HIGH))
        }
        val n = Notification.Builder(context, "etk_fraud")
            .setContentTitle("Posible suplantación: $sender")
            .setContentText(body.take(80))
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setAutoCancel(true).build()
        nm.notify((System.currentTimeMillis() % 100000).toInt(), n)
    }
}
