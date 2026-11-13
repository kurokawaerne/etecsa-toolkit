package dev.kurokawa.etecsa.toolkit.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import java.io.FileInputStream

/** Per-app firewall the simple, correct way: ONLY the blocked apps are
 *  added as "allowed" into this VPN, so only their traffic enters the TUN —
 *  and the TUN drops every packet. Everything else bypasses the VPN and
 *  works normally. No servers, no forwarding, like the original's design
 *  goal (it used hev-socks5-tunnel; dropping needs no tunnel at all). */
class FirewallVpnService : VpnService() {
    @Volatile private var running = false
    private var tun: ParcelFileDescriptor? = null
    private var worker: Thread? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val blocked = getSharedPreferences("etk_kv", Context.MODE_PRIVATE)
            .getStringSet("fw_blocked", emptySet()) ?: emptySet()
        if (blocked.isEmpty()) { stopSelf(); return START_NOT_STICKY }
        createChannel()
        startForeground(43, Notification.Builder(this, CHANNEL)
            .setContentTitle("ETECSA Toolkit — Cortafuegos")
            .setContentText("Bloqueando ${blocked.size} apps")
            .setSmallIcon(android.R.drawable.stat_sys_warning).build())
        startTunnel(blocked)
        return START_STICKY
    }

    private fun startTunnel(blocked: Set<String>) {
        stopTunnel()
        try {
            val builder = Builder().setSession("ETK Firewall")
                .addAddress("10.8.0.2", 32).addRoute("0.0.0.0", 0)
            blocked.forEach { pkg ->
                try { builder.addAllowedApplication(pkg) } catch (_: Exception) {}
            }
            tun = builder.establish() ?: return
            running = true
            val fd = tun!!
            worker = Thread {
                val input = FileInputStream(fd.fileDescriptor)
                val buf = ByteArray(32767)
                while (running) {
                    try { if (input.read(buf) < 0) break } catch (_: Exception) { break }
                    // Packet intentionally dropped: the app is firewalled.
                }
            }.apply { isDaemon = true; start() }
        } catch (_: Exception) {}
    }

    private fun stopTunnel() {
        running = false
        try { tun?.close() } catch (_: Exception) {}
        tun = null
    }

    override fun onRevoke() { stopTunnel(); stopSelf(); super.onRevoke() }
    override fun onDestroy() { stopTunnel(); super.onDestroy() }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "Cortafuegos",
                NotificationManager.IMPORTANCE_LOW))
        }
    }
    companion object { const val CHANNEL = "etk_firewall" }
}
