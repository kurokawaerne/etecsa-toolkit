package dev.kurokawa.etecsa.toolkit.services

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import dev.kurokawa.etecsa.toolkit.shared.R

/** Home-screen widget: last balance reply at a glance. */
class ResourceWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val prefs = context.getSharedPreferences("etk_kv", Context.MODE_PRIVATE)
        val text = prefs.getString("last_balance", "ETECSA Toolkit — consulta tu saldo")
        for (id in ids) {
            val views = RemoteViews(context.packageName, R.layout.widget_resource)
            views.setTextViewText(R.id.widget_text, text)
            manager.updateAppWidget(id, views)
        }
    }
}
