package dev.kurokawa.etecsa.toolkit.services

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.net.TrafficStats
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import dev.kurokawa.etecsa.toolkit.core.fmt

/** Floating traffic monitor (the original's overlay): a small pill under
 *  the status bar showing the live up+down speed, refreshed every second
 *  from TrafficStats deltas. Tap opens the app. */
class TrafficOverlayService : Service() {
    private var wm: WindowManager? = null
    private var view: TextView? = null
    private val handler = Handler(Looper.getMainLooper())
    private var lastBytes = 0L
    private var lastTime = 0L

    private val tick = object : Runnable {
        override fun run() {
            val now = System.currentTimeMillis()
            val total = TrafficStats.getTotalRxBytes() + TrafficStats.getTotalTxBytes()
            if (lastTime > 0 && total >= lastBytes) {
                val perSec = (total - lastBytes) * 1000.0 / (now - lastTime).coerceAtLeast(1)
                view?.text = when {
                    perSec >= 1024 * 1024 -> (perSec / (1024 * 1024)).fmt(1) + " MB/s"
                    perSec >= 1024 -> (perSec / 1024).fmt(0) + " KB/s"
                    else -> perSec.fmt(0) + " B/s"
                }
            }
            lastBytes = total; lastTime = now
            handler.postDelayed(this, 1000)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (view == null) {
            try {
                wm = getSystemService(WINDOW_SERVICE) as WindowManager
                val tv = TextView(this).apply {
                    setTextColor(Color.WHITE)
                    textSize = 11f
                    text = "— KB/s"
                    setPadding(18, 8, 18, 8)
                    background = GradientDrawable().apply {
                        setColor(0xCC1B1B1F.toInt())
                        cornerRadius = 40f
                    }
                    setOnClickListener {
                        try {
                            startActivity(packageManager.getLaunchIntentForPackage(packageName)
                                ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        } catch (_: Exception) {}
                    }
                }
                val type = if (Build.VERSION.SDK_INT >= 26)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    type,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT,
                ).apply {
                    gravity = Gravity.TOP or Gravity.END
                    x = 16; y = 90
                }
                wm?.addView(tv, params)
                view = tv
                handler.post(tick)
            } catch (_: Exception) {
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        try { view?.let { wm?.removeView(it) } } catch (_: Exception) {}
        view = null
        super.onDestroy()
    }
}
