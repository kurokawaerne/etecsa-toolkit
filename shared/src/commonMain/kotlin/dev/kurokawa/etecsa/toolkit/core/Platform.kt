package dev.kurokawa.etecsa.toolkit.core

data class SimInfo(val slot: Int, val subscriptionId: Int, val carrier: String, val number: String, val detected: Boolean)
data class ContactInfo(val name: String, val number: String, val photoUri: String?)
data class SmsInfo(val address: String, val body: String, val date: Long, val incoming: Boolean)
data class CallInfo(val number: String, val name: String?, val date: Long, val durationSec: Long, val incoming: Boolean)
data class AppInfo(val label: String, val packageName: String)
data class HistoryEntry(val id: Long, val kind: String, val title: String, val body: String, val ts: Long, val slot: Int)
data class SpeedResult(val downloadMbps: Double, val uploadMbps: Double, val pingMs: Double, val jitterMs: Double)

/** Everything Android-specific lives behind this bridge (androidMain actual).
 *  Slot-based, never subscription-guessing: slot 0/1 are always addressable
 *  even when a SIM is not detected (Ernest's SIM-slot requirement). */
interface DeviceBridge {
    fun simSlots(): List<SimInfo>
    /** Runs a USSD code on the given slot; falls back to the dialer when the
     *  platform USSD API refuses. Result delivered on an arbitrary thread. */
    fun runUssd(code: String, slot: Int, onResult: (String) -> Unit)
    fun dial(codeOrNumber: String)
    fun call(number: String)
    fun sendSms(number: String, text: String, slot: Int): Boolean
    fun contacts(): List<ContactInfo>
    fun smsInbox(limit: Int): List<SmsInfo>
    fun callLog(limit: Int): List<CallInfo>
    fun cellInfo(): String
    fun trafficInfo(): String
    fun speedTest(onResult: (SpeedResult?) -> Unit)
    fun installedApps(): List<AppInfo>
    fun openUrl(url: String)
    fun openRadioInfo()
    fun setCoverageMonitor(enabled: Boolean, alarm: Boolean)
    fun coverageMonitorOn(): Boolean
    fun setFirewall(blocked: Set<String>)
    fun stopFirewall()
    fun firewallBlocked(): Set<String>
    fun nautaPortal()
}

interface LocalStore {
    fun addHistory(kind: String, title: String, body: String, slot: Int)
    fun history(limit: Int): List<HistoryEntry>
    fun clearHistory()
    fun get(key: String): String?
    fun put(key: String, value: String)
    fun blockedNumbers(): Set<String>
    fun setBlocked(number: String, blocked: Boolean)
    fun friends(): List<ContactInfo>
    fun setFriend(contact: ContactInfo, add: Boolean)
    fun saveCard(label: String, number: String)
    fun cards(): List<Pair<String, String>>
    fun deleteCard(label: String)
    fun queueSms(number: String, text: String, slot: Int, onCoverage: Boolean)
    fun queuedSms(): List<Triple<String, String, Boolean>>
    fun clearQueuedSms()
    /** AES-GCM (AndroidKeyStore) sealed values: transfer PIN, card numbers. */
    fun putSecret(key: String, value: String)
    fun getSecret(key: String): String?
}

expect fun createBridge(): DeviceBridge
expect fun createStore(): LocalStore
