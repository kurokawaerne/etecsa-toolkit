package dev.kurokawa.etecsa.toolkit.core

import dev.kurokawa.etecsa.toolkit.platform.AndroidBridge
import dev.kurokawa.etecsa.toolkit.platform.EtkContext
import dev.kurokawa.etecsa.toolkit.platform.EtkStore

actual fun createBridge(): DeviceBridge =
    AndroidBridge(EtkContext.appContext ?: throw IllegalStateException("EtkContext not set"))

actual fun createStore(): LocalStore =
    EtkStore(EtkContext.appContext ?: throw IllegalStateException("EtkContext not set"))

actual fun nowMillis(): Long = System.currentTimeMillis()

actual fun monthStartMillis(): Long {
    val cal = java.util.Calendar.getInstance()
    cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
    cal.set(java.util.Calendar.MINUTE, 0)
    cal.set(java.util.Calendar.SECOND, 0)
    cal.set(java.util.Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}
