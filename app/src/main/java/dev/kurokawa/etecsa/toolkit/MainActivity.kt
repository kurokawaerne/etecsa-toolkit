package dev.kurokawa.etecsa.toolkit

import android.Manifest
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dev.kurokawa.etecsa.toolkit.platform.EtkContext
import dev.kurokawa.etecsa.toolkit.ui.ToolkitApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Animated splash (SplashScreen API) hands over to the Compose
        // animated splash — installSplashScreen must run before super.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        EtkContext.appContext = applicationContext

        // Runtime permissions the features need (each feature also degrades
        // gracefully on its own if one is missing).
        val perms = buildList {
            add(Manifest.permission.CALL_PHONE)
            add(Manifest.permission.READ_PHONE_STATE)
            add(Manifest.permission.READ_CONTACTS)
            add(Manifest.permission.READ_SMS)
            add(Manifest.permission.SEND_SMS)
            add(Manifest.permission.RECEIVE_SMS)
            add(Manifest.permission.READ_CALL_LOG)
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (perms.isNotEmpty()) requestPermissions(perms.toTypedArray(), 1)

        // Firewall VPN consent (asked once; the system remembers it)
        try {
            VpnService.prepare(this)?.let { startActivityForResult(it, 2) }
        } catch (_: Exception) {}

        setContent { ToolkitApp() }
    }
}
