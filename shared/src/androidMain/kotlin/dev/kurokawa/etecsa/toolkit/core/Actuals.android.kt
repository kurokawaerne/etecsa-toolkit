package dev.kurokawa.etecsa.toolkit.core

import dev.kurokawa.etecsa.toolkit.platform.AndroidBridge
import dev.kurokawa.etecsa.toolkit.platform.EtkContext
import dev.kurokawa.etecsa.toolkit.platform.EtkStore

actual fun createBridge(): DeviceBridge =
    AndroidBridge(EtkContext.appContext ?: throw IllegalStateException("EtkContext not set"))

actual fun createStore(): LocalStore =
    EtkStore(EtkContext.appContext ?: throw IllegalStateException("EtkContext not set"))
