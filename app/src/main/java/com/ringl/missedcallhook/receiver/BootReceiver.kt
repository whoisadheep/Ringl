package com.ringl.missedcallhook.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.ringl.missedcallhook.service.MissedCallService
import com.ringl.missedcallhook.util.PrefsManager

/**
 * Starts the foreground service automatically after device reboot.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val prefs = PrefsManager(context)
        if (!prefs.isMonitoringEnabled()) {
            Log.d("Ringl", "📱 Boot completed — monitoring is disabled, skipping service start")
            return
        }

        Log.d("Ringl", "📱 Boot completed — starting MissedCallService")
        val serviceIntent = Intent(context, MissedCallService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
