package dev.kurokawa.etecsa.toolkit.services

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import dev.kurokawa.etecsa.toolkit.shared.R

/** Shared widget plumbing: tap opens the app; text comes from the prefs
 *  the store/UI keep fresh (last_balance + parsed widget_* resources). */
internal fun openAppIntent(context: Context): PendingIntent {
    val intent = Intent().setComponent(
        ComponentName(context.packageName, "dev.kurokawa.etecsa.toolkit.MainActivity")
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return PendingIntent.getActivity(context, 0, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}

/** Compact widget (the original's Resource Compact): one line, last reply. */
class ResourceCompactWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val prefs = context.getSharedPreferences("etk_kv", Context.MODE_PRIVATE)
        val full = prefs.getString("last_balance", "ETECSA Toolkit — consulta tu saldo") ?: ""
        val text = full.lineSequence().firstOrNull { it.isNotBlank() } ?: full
        for (id in ids) {
            val views = RemoteViews(context.packageName, R.layout.widget_compact)
            views.setTextViewText(R.id.widget_text, text)
            views.setOnClickPendingIntent(R.id.widget_root, openAppIntent(context))
            manager.updateAppWidget(id, views)
        }
    }
}

/** Multi-line widget (the original's Etecsa Multi-Line): saldo / datos /
 *  voz / SMS on separate lines, fed by the parser on every consult. */
class MultiLineWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val prefs = context.getSharedPreferences("etk_kv", Context.MODE_PRIVATE)
        fun line(key: String, label: String) = "$label: " + (prefs.getString(key, null) ?: "—")
        for (id in ids) {
            val views = RemoteViews(context.packageName, R.layout.widget_multiline)
            views.setTextViewText(R.id.widget_saldo, line("widget_saldo", "Saldo"))
            views.setTextViewText(R.id.widget_datos, line("widget_datos", "Datos"))
            views.setTextViewText(R.id.widget_voz, line("widget_voz", "Voz"))
            views.setTextViewText(R.id.widget_sms, line("widget_sms", "SMS"))
            views.setOnClickPendingIntent(R.id.widget_root, openAppIntent(context))
            manager.updateAppWidget(id, views)
        }
    }
}
