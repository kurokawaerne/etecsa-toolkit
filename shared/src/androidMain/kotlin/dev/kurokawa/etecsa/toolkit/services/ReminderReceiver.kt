package dev.kurokawa.etecsa.toolkit.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

/** Generic one-shot reminder: shows the title/body it was armed with.
 *  Used by call-back reminders and birthday reminders. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "ETECSA Toolkit"
        val body = intent.getStringExtra("body") ?: ""
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= 26) {
                nm.createNotificationChannel(NotificationChannel("etk_reminders", "Recordatorios",
                    NotificationManager.IMPORTANCE_DEFAULT))
            }
            val n = Notification.Builder(context, "etk_reminders")
                .setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setAutoCancel(true).build()
            nm.notify((title + body).hashCode(), n)
        } catch (_: Exception) {}
    }
}
