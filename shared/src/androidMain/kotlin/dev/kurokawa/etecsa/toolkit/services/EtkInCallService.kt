package dev.kurokawa.etecsa.toolkit.services

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.telecom.InCallService

/** In-call service (the original's RoClaInCall): binds ONLY when the user
 *  makes ETECSA Toolkit the default phone app. Owns the in-call screen,
 *  the call-duration cut timer ("el corte es irreversible" — the timer is
 *  armed when the call CONNECTS, like the original) and respond-via. */
class EtkInCallService : InCallService() {
    companion object {
        var currentCall: Call? = null
        var instance: EtkInCallService? = null
    }

    private val handler = Handler(Looper.getMainLooper())
    private var cutRunnable: Runnable? = null

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        currentCall = call
        instance = this
        call.registerCallback(object : Call.Callback() {
            override fun onStateChanged(c: Call, state: Int) {
                if (state == Call.STATE_ACTIVE) armCutTimer(c)
                if (state == Call.STATE_DISCONNECTED || state == Call.STATE_DISCONNECTING) disarmCutTimer()
            }
        })
        val intent = Intent(applicationContext, InCallActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        disarmCutTimer()
        if (currentCall == call) currentCall = null
    }

    private fun armCutTimer(call: Call) {
        disarmCutTimer()
        val minutes = applicationContext.getSharedPreferences("etk_kv", MODE_PRIVATE)
            .getString("cut_timer_min", "0")?.toIntOrNull() ?: 0
        if (minutes <= 0) return
        cutRunnable = Runnable { try { call.disconnect() } catch (_: Exception) {} }
        handler.postDelayed(cutRunnable!!, minutes * 60000L)
    }

    private fun disarmCutTimer() {
        cutRunnable?.let { handler.removeCallbacks(it) }
        cutRunnable = null
    }
}
