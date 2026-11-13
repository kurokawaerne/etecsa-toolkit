package dev.kurokawa.etecsa.toolkit.core

/** Kotlin twin of the Rust toolkit-core parser (same rules, unit-tested in
 *  both). Parses ETECSA/Cubacel USSD & SMS replies in Spanish. */
data class ParsedResponse(
    val balanceCup: Double? = null,
    val dataBytes: Long? = null,
    val voiceMinutes: Int? = null,
    val smsCount: Int? = null,
    val transferAmount: Double? = null,
    val transferNumber: String? = null,
    val planName: String? = null,
    val kind: Kind = Kind.UNKNOWN,
) { enum class Kind { BALANCE, TRANSFER_CONFIRMATION, PLAN_PURCHASE, GENERIC, UNKNOWN } }

private fun num(s: String): Double? = s.replace(",", ".").toDoubleOrNull()

/** Locale-free decimal formatting (String.format is JVM-only, this is common code). */
fun Double.fmt(decimals: Int): String {
    var factor = 1.0; repeat(decimals) { factor *= 10 }
    val scaled = (this * factor + 0.5).toLong()
    if (decimals == 0) return scaled.toString()
    val whole = scaled / factor.toLong()
    val frac = (scaled % factor.toLong()).toString().padStart(decimals, '0')
    return "$whole.$frac"
}

object ResponseParser {
    private val balanceRe = Regex("""(?i)Saldo\s+(?:principal|disponible|actual)?\s*:?\s*([0-9]+(?:[.,][0-9]+)?)\s*CUP""")
    private val dataRe = Regex("""(?i)Datos\s*:?\s*([0-9]+(?:[.,][0-9]+)?)\s*(GB|MB|KB)""")
    private val voiceRe = Regex("""(?i)(?:Voz|Minutos?)\s*:?\s*([0-9]+)\s*(?:min)?""")
    private val smsRe = Regex("""(?i)(?:SMSs?|Mensajes?)\s*:?\s*([0-9]+)""")
    private val transferRe = Regex("""(?iu)(?:ha\s+transferido|transferencia\s+(?:realizada|enviada)).*?([0-9]+(?:[.,][0-9]+)?)\s*CUP.*?(?:al\s+n[uú]mero\s+|al\s+)?([0-9]{8})""")
    private val planRe = Regex("""(?iu)(?:Se\s+ha\s+comprado|Ha\s+comprado|Ha\s+adquirido)\s+Plan\s+(.+?)(?:\.|$)""")

    fun parse(text: String): ParsedResponse {
        if (text.isBlank()) return ParsedResponse()
        transferRe.find(text)?.let {
            return ParsedResponse(transferAmount = num(it.groupValues[1]),
                transferNumber = it.groupValues[2], kind = ParsedResponse.Kind.TRANSFER_CONFIRMATION)
        }
        planRe.find(text)?.let {
            return ParsedResponse(planName = it.groupValues[1].trim(), kind = ParsedResponse.Kind.PLAN_PURCHASE)
        }
        val bal = balanceRe.find(text)?.let { num(it.groupValues[1]) }
        val data = dataRe.find(text)?.let {
            val v = num(it.groupValues[1]) ?: 0.0
            (when (it.groupValues[2].uppercase()) {
                "GB" -> v * 1024 * 1024 * 1024; "MB" -> v * 1024 * 1024; else -> v * 1024
            }).toLong()
        }
        val voice = voiceRe.find(text)?.groupValues?.get(1)?.toIntOrNull()
        val sms = smsRe.find(text)?.groupValues?.get(1)?.toIntOrNull()
        val kind = if (bal != null || data != null || voice != null || sms != null)
            ParsedResponse.Kind.BALANCE else ParsedResponse.Kind.GENERIC
        return ParsedResponse(bal, data, voice, sms, kind = kind)
    }

    fun formatData(bytes: Long): String = when {
        bytes >= 1024L * 1024 * 1024 -> (bytes / (1024.0 * 1024 * 1024)).fmt(2) + " GB"
        bytes >= 1024L * 1024 -> (bytes / (1024.0 * 1024)).fmt(1) + " MB"
        else -> (bytes / 1024.0).fmt(0) + " KB"
    }
}

object Antifraud {
    private val brands = listOf("etecsa", "cubacel", "pagoxmovil", "pago por movil", "enzona", "transfermovil")
    private val urlRe = Regex("""(?i)(?:https?://|www\.)[^\s]+""")
    /** True when the message smells like brand impersonation. */
    fun looksSuspicious(sender: String, body: String, officials: Set<String>): Boolean {
        if (officials.any { it.equals(sender, ignoreCase = true) }) return false
        val b = body.lowercase()
        val mentionsBrand = brands.any { b.contains(it) }
        val senderLooksOfficial = brands.any { sender.lowercase().contains(it.replace(" ", "")) }
        return senderLooksOfficial || (mentionsBrand && urlRe.containsMatchIn(body))
    }
}
