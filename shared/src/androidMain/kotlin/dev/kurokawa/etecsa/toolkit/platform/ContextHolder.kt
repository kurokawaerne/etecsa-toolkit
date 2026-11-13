package dev.kurokawa.etecsa.toolkit.platform

import android.content.Context

/** Set once by the app module's MainActivity. */
object EtkContext {
    @Volatile var appContext: Context? = null
}
