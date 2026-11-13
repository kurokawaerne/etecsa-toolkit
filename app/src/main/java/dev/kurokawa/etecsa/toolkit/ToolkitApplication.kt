package dev.kurokawa.etecsa.toolkit

import android.app.Application
import dev.kurokawa.etecsa.toolkit.platform.EtkContext

class ToolkitApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        EtkContext.appContext = applicationContext
    }
}
