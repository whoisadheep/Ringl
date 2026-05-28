package com.ringl.missedcallhook.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.CallLog
import android.telephony.TelephonyManager
import android.util.Log
import com.ringl.missedcallhook.util.PrefsManager
import com.ringl.missedcallhook.webhook.WebhookSender

/**
 * Hybrid missed call detector:
 *   1. BroadcastReceiver catches RINGING → IDLE transition (real-time)
 *   2. Then confirms via CallLog query (accuracy)
 *   3. Falls back to BroadcastReceiver detection if CallLog doesn't have an entry yet
 */
class MissedCallReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "Ringl"
        private var lastState = TelephonyManager.CALL_STATE_IDLE
        private var incomingNumber: String? = null
        private var wasRinging = false
        private var callAnswered = false
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        // Check if monitoring is enabled
        val prefs = PrefsManager(context)
        if (!prefs.isMonitoringEnabled()) return

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        Log.d(TAG, "📞 State: $state | Number: ${number ?: "(hidden)"}")

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                wasRinging = true
                callAnswered = false
                if (number != null) {
                    incomingNumber = number
                }
                lastState = TelephonyManager.CALL_STATE_RINGING
            }

            TelephonyManager.EXTRA_STATE_IDLE -> {
                if (wasRinging && !callAnswered && lastState == TelephonyManager.CALL_STATE_RINGING) {
                    val missedNumber = incomingNumber
                    Log.d(TAG, "🔴 Missed call detected (RINGING→IDLE) from: $missedNumber")

                    if (missedNumber != null) {
                        // Use user-defined delay (minimum 2s for stability)
                        val delayMs = (prefs.getReplyDelay().coerceAtLeast(2) * 1000L)
                        
                        Handler(Looper.getMainLooper()).postDelayed({
                            confirmOrFallback(context, missedNumber)
                        }, delayMs)
                    }
                }
                wasRinging = false
                callAnswered = false
                incomingNumber = null
                lastState = TelephonyManager.CALL_STATE_IDLE
            }

            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                // Call was answered — NOT a missed call
                callAnswered = true
                wasRinging = false
                lastState = TelephonyManager.CALL_STATE_OFFHOOK
            }
        }
    }

    /**
     * Try to confirm via CallLog. If CallLog doesn't have the entry yet,
     * trust the BroadcastReceiver's RINGING→IDLE detection and send anyway.
     */
    private fun confirmOrFallback(context: Context, phoneNumber: String) {
        val prefs = PrefsManager(context)
        
        // Record the call in our internal history
        prefs.recordMissedCall(phoneNumber)
        
        // First, try CallLog confirmation
        val confirmedViaLog = checkCallLog(context)

        if (confirmedViaLog) {
            Log.d(TAG, "✅ Confirmed missed call via CallLog for: $phoneNumber")
            WebhookSender.send(context, phoneNumber, System.currentTimeMillis())
        } else {
            // Fallback: Trust the RINGING → IDLE transition
            Log.d(TAG, "⚠️ CallLog didn't confirm, but RINGING→IDLE detected. Sending webhook anyway for: $phoneNumber")
            WebhookSender.send(context, phoneNumber, System.currentTimeMillis())
        }
    }

    /**
     * Check CallLog for a missed call in the last 30 seconds.
     */
    private fun checkCallLog(context: Context): Boolean {
        try {
            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.TYPE,
                    CallLog.Calls.DATE
                ),
                "${CallLog.Calls.TYPE} = ? AND ${CallLog.Calls.DATE} > ?",
                arrayOf(
                    CallLog.Calls.MISSED_TYPE.toString(),
                    (System.currentTimeMillis() - 30_000).toString() // last 30 seconds
                ),
                "${CallLog.Calls.DATE} DESC"
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    val number = it.getString(it.getColumnIndexOrThrow(CallLog.Calls.NUMBER))
                    Log.d(TAG, "📋 CallLog has missed call from: $number")
                    return true
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ READ_CALL_LOG permission not granted", e)
        }
        return false
    }
}
