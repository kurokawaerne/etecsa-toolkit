package dev.kurokawa.etecsa.toolkit.services

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.TelephonyManager
import dev.kurokawa.etecsa.toolkit.core.ResponseParser
import dev.kurokawa.etecsa.toolkit.core.nowMillis
import dev.kurokawa.etecsa.toolkit.platform.AndroidBridge
import dev.kurokawa.etecsa.toolkit.platform.EtkStore

private const val AUTOSYNC_REQUEST = 44001

/** (Re)arms the auto-sync chain: next run in [hours] from now; cancels
 *  when hours <= 0. Self-rescheduling alarm chain, same pattern as the
 *  scheduled SMS (setExactAndAllowWhileIdle with the canScheduleExactAlarms
 *  fallback). */
fun armAutoSync(ctx: Context, hours: Int) {
    val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(ctx, AutoSyncReceiver::class.java)
    val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    val pi = PendingIntent.getBroadcast(ctx, AUTOSYNC_REQUEST, intent, flags)
    am.cancel(pi)
    if (hours <= 0) return
    val at = System.currentTimeMillis() + hours * 3600000L
    if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
    } else {
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
    }
}

/** Background balance consult (the original's automatic synchronisation):
 *  runs *222# on the chosen SIM, stores the reply, refreshes the widget
 *  values and re-arms any expiry reminder. Also re-arms the chain after
 *  a reboot (BOOT_COMPLETED) without running a consult. */
class AutoSyncReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("etk_kv", Context.MODE_PRIVATE)
        val hours = prefs.getString("autosync_hours", "0")?.toIntOrNull() ?: 0
        if (hours <= 0) return
        armAutoSync(context, hours)
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) return
        val slot = prefs.getString("sim_slot", "0")?.toIntOrNull() ?: 0
        try {
            val subs = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE)
                as android.telephony.SubscriptionManager
            val subId = subs.activeSubscriptionInfoList
                ?.firstOrNull { it.simSlotIndex == slot }?.subscriptionId
                ?: android.telephony.SubscriptionManager.DEFAULT_SUBSCRIPTION_ID
            val base = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val tm = if (subId == android.telephony.SubscriptionManager.DEFAULT_SUBSCRIPTION_ID) base
                else base.createForSubscriptionId(subId)
            val store = EtkStore(context)
            tm.sendUssdRequest("*222#", object : TelephonyManager.UssdResponseCallback() {
                override fun onReceiveUssdResponse(
                    telephonyManager: TelephonyManager, request: String, response: CharSequence,
                ) {
                    val reply = response.toString()
                    store.addHistory("ussd", "Saldo (auto)", reply, slot)
                    val parsed = ResponseParser.parse(reply)
                    val e = prefs.edit()
                    parsed.balanceCup?.let { e.putString("widget_saldo", "$it CUP") }
                    parsed.dataBytes?.let { e.putString("widget_datos", ResponseParser.formatData(it)) }
                    parsed.voiceMinutes?.let { e.putString("widget_voz", "$it min") }
                    parsed.smsCount?.let { e.putString("widget_sms", "$it SMS") }
                    e.apply()
                    parsed.expires?.let { exp ->
                        ResponseParser.expiryReminderMillis(exp, nowMillis())?.let { at ->
                            AndroidBridge(context).scheduleReminder(
                                "Vencimiento cercano", "Tu saldo o plan vence el $exp", at)
                        }
                    }
                }
            }, Handler(Looper.getMainLooper()))
        } catch (_: Exception) {}
    }
}
