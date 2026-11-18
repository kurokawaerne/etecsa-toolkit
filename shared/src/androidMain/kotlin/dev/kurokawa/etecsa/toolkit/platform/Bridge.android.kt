package dev.kurokawa.etecsa.toolkit.platform

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.TrafficStats
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.Telephony
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import dev.kurokawa.etecsa.toolkit.core.*
import dev.kurokawa.etecsa.toolkit.services.CoverageMonitorService
import dev.kurokawa.etecsa.toolkit.services.FirewallVpnService
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class AndroidBridge(private val ctx: Context) : DeviceBridge {
    private val main = Handler(Looper.getMainLooper())
    private val pool = Executors.newCachedThreadPool()
    private val subs: SubscriptionManager? =
        ctx.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
    private val prefs = ctx.getSharedPreferences("etk_kv", Context.MODE_PRIVATE)

    private fun subIdForSlot(slot: Int): Int = try {
        subs?.getActiveSubscriptionInfoForSimSlotIndex(slot)?.subscriptionId ?: -1
    } catch (_: Exception) { -1 }

    private fun telephonyForSlot(slot: Int): TelephonyManager {
        val base = ctx.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val subId = subIdForSlot(slot)
        return if (subId >= 0) base.createForSubscriptionId(subId) else base
    }

    override fun signalLevel(slot: Int): Int = try {
        val subId = subIdForSlot(slot)
        if (subId < 0) -1 else {
            val tm = ctx.getSystemService(TelephonyManager::class.java)
                .createForSubscriptionId(subId)
            tm.signalStrength?.level ?: -1
        }
    } catch (e: Exception) { -1 }

    override fun simSlots(): List<SimInfo> = (0..1).map { slot ->
        try {
            val info = subs?.getActiveSubscriptionInfoForSimSlotIndex(slot)
            if (info != null) SimInfo(slot, info.subscriptionId,
                info.carrierName?.toString() ?: "Cubacel",
                info.number ?: "", true)
            else SimInfo(slot, -1, "", "", false)
        } catch (_: Exception) { SimInfo(slot, -1, "", "", false) }
    }

    override fun runUssd(code: String, slot: Int, onResult: (String) -> Unit) {
        val done = java.util.concurrent.atomic.AtomicBoolean(false)
        fun deliver(text: String) { if (done.compareAndSet(false, true)) main.post { onResult(text) } }
        try {
            val tm = telephonyForSlot(slot)
            // Handler overload (API 26+): the Executor overload is
            // ambiguous to Kotlin's overload resolution here.
            tm.sendUssdRequest(code, object : TelephonyManager.UssdResponseCallback() {
                override fun onReceiveUssdResponse(tm: TelephonyManager, request: String, response: CharSequence) {
                    deliver(response.toString())
                }
                override fun onReceiveUssdResponseFailed(tm: TelephonyManager, request: String, failureCode: Int) {
                    // Platform refused (e.g. no subscription for the slot):
                    // fall back to the dialer, like the original does.
                    dial(code)
                    deliver("USSD abierto en el marcador: $code")
                }
            }, main)
            // Safety timeout so the UI never spins forever
            main.postDelayed({ deliver("Sin respuesta de ETECSA (tiempo agotado): $code") }, 45_000)
        } catch (e: Exception) {
            dial(code)
            deliver("USSD abierto en el marcador: $code (${e.message ?: "sin API USSD"})")
        }
    }

    override fun dial(codeOrNumber: String) {
        try {
            val uri = Uri.parse("tel:" + Uri.encode(codeOrNumber))
            val intent = Intent(Intent.ACTION_CALL, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
        } catch (_: Exception) {
            try {
                val uri = Uri.parse("tel:" + Uri.encode(codeOrNumber))
                ctx.startActivity(Intent(Intent.ACTION_DIAL, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (_: Exception) {}
        }
    }
    override fun call(number: String) = dial(number)

    override fun sendSms(number: String, text: String, slot: Int): Boolean = try {
        val subId = subIdForSlot(slot)
        val mgr: SmsManager = when {
            Build.VERSION.SDK_INT >= 31 && subId >= 0 ->
                ctx.getSystemService(SmsManager::class.java).createForSubscriptionId(subId)
            else -> @Suppress("DEPRECATION") SmsManager.getDefault()
        }
        val parts = mgr.divideMessage(text)
        if (parts.size > 1) mgr.sendMultipartTextMessage(number, null, parts, null, null)
        else mgr.sendTextMessage(number, null, text, null, null)
        true
    } catch (_: Exception) { false }

    override fun contacts(): List<ContactInfo> = try {
        val out = mutableListOf<ContactInfo>()
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        ctx.contentResolver.query(uri,
            arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI),
            null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC")?.use { c ->
            val seen = mutableSetOf<String>()
            while (c.moveToNext()) {
                val name = c.getString(0) ?: ""; val num = c.getString(1) ?: continue
                if (seen.add(num)) out += ContactInfo(name, num, c.getString(2))
            }
        }
        out
    } catch (_: Exception) { emptyList() }

    override fun smsInbox(limit: Int): List<SmsInfo> = try {
        val out = mutableListOf<SmsInfo>()
        ctx.contentResolver.query(Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.TYPE),
            null, null, Telephony.Sms.DATE + " DESC")?.use { c ->
            while (c.moveToNext() && out.size < limit) {
                out += SmsInfo(c.getString(0) ?: "", c.getString(1) ?: "",
                    c.getLong(2), c.getInt(3) == Telephony.Sms.MESSAGE_TYPE_INBOX)
            }
        }
        out
    } catch (_: Exception) { emptyList() }

    override fun callLog(limit: Int): List<CallInfo> = try {
        val out = mutableListOf<CallInfo>()
        ctx.contentResolver.query(CallLog.Calls.CONTENT_URI,
            arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME, CallLog.Calls.DATE,
                CallLog.Calls.DURATION, CallLog.Calls.TYPE),
            null, null, CallLog.Calls.DATE + " DESC")?.use { c ->
            while (c.moveToNext() && out.size < limit) {
                out += CallInfo(c.getString(0) ?: "", c.getString(1), c.getLong(2),
                    c.getLong(3), c.getInt(4) == CallLog.Calls.INCOMING_TYPE)
            }
        }
        out
    } catch (_: Exception) { emptyList() }

    override fun cellInfo(): String = try {
        val tm = ctx.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val net = tm.networkOperatorName ?: "—"
        val type = when (tm.dataNetworkType) {
            TelephonyManager.NETWORK_TYPE_LTE -> "LTE"
            TelephonyManager.NETWORK_TYPE_NR -> "5G NR"
            TelephonyManager.NETWORK_TYPE_HSPAP, TelephonyManager.NETWORK_TYPE_HSPA -> "3G HSPA"
            TelephonyManager.NETWORK_TYPE_EDGE, TelephonyManager.NETWORK_TYPE_GPRS -> "2G"
            TelephonyManager.NETWORK_TYPE_UMTS -> "3G UMTS"
            else -> "tipo " + tm.dataNetworkType
        }
        val sig = try { tm.signalStrength?.level?.let { "Nivel de señal: $it/4" } ?: "" } catch (_: Exception) { "" }
        val cell = try {
            tm.allCellInfo?.firstOrNull { it.isRegistered }?.cellIdentity?.toString() ?: ""
        } catch (_: Exception) { "" }
        listOf("Operador: $net", "Red: $type", sig, cell).filter { it.isNotBlank() }.joinToString("\n")
    } catch (e: Exception) { "Sin datos de celda: ${e.message}" }

    override fun trafficInfo(): String {
        fun fmt(bytes: Long): String = when {
            bytes < 0 -> "—"
            bytes >= 1_073_741_824 -> "%.2f GB".format(bytes / 1_073_741_824.0)
            bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
            else -> "%.0f KB".format(bytes / 1024.0)
        }
        val rx = TrafficStats.getTotalRxBytes(); val tx = TrafficStats.getTotalTxBytes()
        val mRx = TrafficStats.getMobileRxBytes(); val mTx = TrafficStats.getMobileTxBytes()
        return "Desde el arranque del teléfono:\nTotal: ↓ ${fmt(rx)} · ↑ ${fmt(tx)}\nDatos móviles: ↓ ${fmt(mRx)} · ↑ ${fmt(mTx)}"
    }

    override fun speedTest(onResult: (SpeedResult?) -> Unit) {
        pool.execute {
            try {
                // Ping: 3 HEAD requests
                val pings = mutableListOf<Double>()
                repeat(3) {
                    val t0 = System.nanoTime()
                    val c = URL("https://speed.cloudflare.com/__down?bytes=0").openConnection() as HttpURLConnection
                    c.requestMethod = "HEAD"; c.connectTimeout = 8000; c.readTimeout = 8000
                    c.responseCode; c.disconnect()
                    pings += (System.nanoTime() - t0) / 1e6
                }
                // Download ~3 MB
                val t0 = System.nanoTime()
                var total = 0L
                val dc = URL("https://speed.cloudflare.com/__down?bytes=3000000").openConnection() as HttpURLConnection
                dc.connectTimeout = 10000; dc.readTimeout = 30000
                dc.inputStream.use { ins ->
                    val buf = ByteArray(64 * 1024)
                    while (true) { val n = ins.read(buf); if (n < 0) break; total += n }
                }
                val downSec = (System.nanoTime() - t0) / 1e9
                val downMbps = total * 8 / downSec / 1e6
                // Upload ~1 MB
                val payload = ByteArray(1_000_000) { 0x61 }
                val t1 = System.nanoTime()
                val uc = URL("https://speed.cloudflare.com/__up").openConnection() as HttpURLConnection
                uc.requestMethod = "POST"; uc.doOutput = true
                uc.connectTimeout = 10000; uc.readTimeout = 30000
                uc.outputStream.use { it.write(payload) }
                uc.responseCode; uc.disconnect()
                val upSec = (System.nanoTime() - t1) / 1e9
                val upMbps = payload.size * 8 / upSec / 1e6
                val ping = pings.average()
                val jitter = if (pings.size > 1)
                    pings.zipWithNext { a, b -> kotlin.math.abs(a - b) }.average() else 0.0
                val result = SpeedResult(downMbps, upMbps, ping, jitter)
                main.post { onResult(result) }
            } catch (_: Exception) { main.post { onResult(null) } }
        }
    }

    override fun installedApps(): List<AppInfo> = try {
        val pm = ctx.packageManager
        pm.getInstalledApplications(0)
            .map { AppInfo(pm.getApplicationLabel(it).toString(), it.packageName) }
            .sortedBy { it.label.lowercase() }
    } catch (_: Exception) { emptyList() }

    override fun openUrl(url: String) {
        try {
            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {}
    }

    override fun openRadioInfo() {
        val attempts = listOf(
            ComponentName("com.android.phone", "com.android.phone.settings.RadioInfo"),
            ComponentName("com.android.settings", "com.android.settings.RadioInfo"),
        )
        for (cn in attempts) {
            try {
                ctx.startActivity(Intent().setComponent(cn).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return
            } catch (_: Exception) {}
        }
        dial("*#*#4636#*#*")
    }

    override fun setCoverageMonitor(enabled: Boolean, alarm: Boolean) {
        prefs.edit().putBoolean("cov_monitor", enabled).putBoolean("cov_alarm", alarm).apply()
        val intent = Intent(ctx, CoverageMonitorService::class.java)
        if (enabled) {
            if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(intent) else ctx.startService(intent)
        } else ctx.stopService(intent)
    }
    override fun coverageMonitorOn(): Boolean = prefs.getBoolean("cov_monitor", false)

    override fun setFirewall(blocked: Set<String>) {
        prefs.edit().putStringSet("fw_blocked", blocked).apply()
        val intent = Intent(ctx, FirewallVpnService::class.java)
        try { ctx.startService(intent) } catch (_: Exception) {}
    }
    override fun stopFirewall() {
        try { ctx.stopService(Intent(ctx, FirewallVpnService::class.java)) } catch (_: Exception) {}
    }
    override fun firewallBlocked(): Set<String> =
        prefs.getStringSet("fw_blocked", emptySet()) ?: emptySet()

    override fun nautaPortal() = openUrl("https://secure.etecsa.net:8443/")

    override fun scheduleReminder(title: String, body: String, atMillis: Long) {
        try {
            val intent = Intent(ctx, dev.kurokawa.etecsa.toolkit.services.ReminderReceiver::class.java)
                .putExtra("title", title).putExtra("body", body)
            val pi = android.app.PendingIntent.getBroadcast(ctx, (title + body + atMillis).hashCode(), intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
            val am = ctx.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, atMillis, pi)
            } else {
                am.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, atMillis, pi)
            }
        } catch (_: Exception) {}
    }

    override fun openDefaultDialerSettings() {
        try {
            val intent = Intent(android.telecom.TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
                .putExtra(android.telecom.TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, ctx.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
        } catch (_: Exception) {
            try {
                ctx.startActivity(Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (_: Exception) {}
        }
    }

    override fun setAutoSync(hours: Int) {
        prefs.edit().putString("autosync_hours", hours.toString()).apply()
        dev.kurokawa.etecsa.toolkit.services.armAutoSync(ctx, hours)
    }

    override fun setTrafficOverlay(on: Boolean) {
        val intent = Intent(ctx, dev.kurokawa.etecsa.toolkit.services.TrafficOverlayService::class.java)
        if (on) {
            try { ctx.startService(intent) } catch (_: Exception) {}
        } else {
            try { ctx.stopService(intent) } catch (_: Exception) {}
        }
    }

    override fun canDrawOverlays(): Boolean =
        Build.VERSION.SDK_INT < 23 || android.provider.Settings.canDrawOverlays(ctx)

    override fun openOverlaySettings() {
        try {
            ctx.startActivity(
                Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + ctx.packageName))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {}
    }

    /** Birthdays from the contacts' Event rows. START_DATE arrives as
     *  yyyy-MM-dd, --MM-dd or MM-dd depending on the source account; only
     *  month/day matter. Arms a 9:00 reminder for each upcoming one. */
    override fun contactBirthdays(daysAhead: Int): List<BirthdayInfo> = try {
        val out = mutableListOf<BirthdayInfo>()
        val today = java.util.Calendar.getInstance()
        val fmt = java.text.SimpleDateFormat("d MMM", java.util.Locale("es"))
        val uri = ContactsContract.Data.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Event.START_DATE,
            ContactsContract.CommonDataKinds.Event.CONTACT_ID,
        )
        val selection = ContactsContract.Data.MIMETYPE + "=? AND " +
            ContactsContract.CommonDataKinds.Event.TYPE + "=?"
        val args = arrayOf(
            ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY.toString(),
        )
        ctx.contentResolver.query(uri, projection, selection, args, null)?.use { c ->
            while (c.moveToNext()) {
                val name = c.getString(0) ?: continue
                val raw = c.getString(1) ?: continue
                val contactId = c.getLong(2)
                val parts = raw.split("-").filter { it.isNotBlank() }
                if (parts.size < 2) continue
                val month = parts[parts.size - 2].toIntOrNull() ?: continue
                val day = parts[parts.size - 1].toIntOrNull() ?: continue
                val next = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.MONTH, month - 1)
                    set(java.util.Calendar.DAY_OF_MONTH, day)
                    set(java.util.Calendar.HOUR_OF_DAY, 9)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                }
                if (next.timeInMillis < today.timeInMillis) next.add(java.util.Calendar.YEAR, 1)
                val daysUntil = ((next.timeInMillis - today.timeInMillis) / 86400000L).toInt()
                if (daysUntil > daysAhead) continue
                // First phone number for this contact, if any
                var number = ""
                ctx.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?",
                    arrayOf(contactId.toString()), null,
                )?.use { pc -> if (pc.moveToFirst()) number = pc.getString(0) ?: "" }
                out += BirthdayInfo(name, number, daysUntil, fmt.format(next.time))
                scheduleReminder("Cumpleaños", "Hoy cumple años $name", next.timeInMillis)
            }
        }
        out.sortedBy { it.daysUntil }
    } catch (_: Exception) { emptyList() }
}
