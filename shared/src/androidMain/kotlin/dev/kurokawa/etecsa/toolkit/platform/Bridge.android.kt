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
            val executor = java.util.concurrent.Executor { r -> Thread(r).start() }
            tm.sendUssdRequest(code, executor, object : TelephonyManager.UssdResponseCallback() {
                override fun onReceiveUssdResponse(tm: TelephonyManager, request: String, response: CharSequence) {
                    deliver(response.toString())
                }
                override fun onReceiveUssdResponseFailed(tm: TelephonyManager, request: String, failureCode: Int) {
                    // Platform refused (e.g. no subscription for the slot):
                    // fall back to the dialer, like the original does.
                    dial(code)
                    deliver("USSD abierto en el marcador: $code")
                }
            })
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
}
