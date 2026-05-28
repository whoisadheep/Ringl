package com.ringl.missedcallhook.util

import android.content.Context
import android.content.SharedPreferences
import com.ringl.missedcallhook.BuildConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * SharedPreferences wrapper for app settings and stats.
 */
class PrefsManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "ringl_prefs"
        private const val KEY_WEBHOOK_URL = "webhook_url"
        private const val KEY_WEBHOOK_SECRET = "webhook_secret"
        private const val KEY_TENANT_ID = "tenant_id"
        private const val KEY_MONITORING_ENABLED = "monitoring_enabled"
        private const val KEY_TOTAL_SENT = "total_webhooks_sent"
        private const val KEY_LAST_MISSED_NUMBER = "last_missed_number"
        private const val KEY_LAST_MISSED_TIME = "last_missed_time"
        
        // New Keys
        private const val KEY_FIRST_RUN = "first_run_2026"
        private const val KEY_REPLY_DELAY = "reply_delay"
        private const val KEY_REPLY_MESSAGE = "reply_message"
        private const val KEY_CALL_LOGS = "call_history_json"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Navigation & First Run ──

    fun isFirstRun(): Boolean = prefs.getBoolean(KEY_FIRST_RUN, true)
    
    fun setFirstRunComplete() = prefs.edit().putBoolean(KEY_FIRST_RUN, false).apply()

    // ── Webhook Config ──

    fun getWebhookUrl(): String =
        prefs.getString(KEY_WEBHOOK_URL, BuildConfig.WEBHOOK_URL) ?: ""

    fun setWebhookUrl(url: String) =
        prefs.edit().putString(KEY_WEBHOOK_URL, url).apply()

    fun getWebhookSecret(): String =
        prefs.getString(KEY_WEBHOOK_SECRET, BuildConfig.WEBHOOK_SECRET) ?: ""

    fun setWebhookSecret(secret: String) =
        prefs.edit().putString(KEY_WEBHOOK_SECRET, secret).apply()

    fun getTenantId(): String =
        prefs.getString(KEY_TENANT_ID, "") ?: ""

    fun setTenantId(tenantId: String) =
        prefs.edit().putString(KEY_TENANT_ID, tenantId).apply()

    // ── Automation Settings ──

    fun getReplyDelay(): Int = prefs.getInt(KEY_REPLY_DELAY, 5) // Default 5s
    
    fun setReplyDelay(seconds: Int) = prefs.edit().putInt(KEY_REPLY_DELAY, seconds).apply()

    fun getReplyMessage(): String = prefs.getString(KEY_REPLY_MESSAGE, 
        "Hi, we missed your call from Ringl. How can we help you on WhatsApp?") ?: ""
    
    fun setReplyMessage(msg: String) = prefs.edit().putString(KEY_REPLY_MESSAGE, msg).apply()

    // ── Monitoring Toggle ──

    fun isMonitoringEnabled(): Boolean =
        prefs.getBoolean(KEY_MONITORING_ENABLED, true)

    fun setMonitoringEnabled(enabled: Boolean) =
        prefs.edit().putBoolean(KEY_MONITORING_ENABLED, enabled).apply()

    // ── Stats & History ──

    fun getTotalWebhooksSent(): Int =
        prefs.getInt(KEY_TOTAL_SENT, 0)

    fun incrementWebhooksSent() =
        prefs.edit().putInt(KEY_TOTAL_SENT, getTotalWebhooksSent() + 1).apply()

    fun getLastMissedCallNumber(): String =
        prefs.getString(KEY_LAST_MISSED_NUMBER, "") ?: ""

    fun getLastMissedCallTime(): String =
        prefs.getString(KEY_LAST_MISSED_TIME, "") ?: ""

    fun getCallLogs(): String = prefs.getString(KEY_CALL_LOGS, "[]") ?: "[]"

    fun recordMissedCall(number: String, status: String = "Replied") {
        val timeStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date())
        
        // 1. Record basic last call stats
        prefs.edit()
            .putString(KEY_LAST_MISSED_NUMBER, number)
            .putString(KEY_LAST_MISSED_TIME, timeStr)
            .apply()

        // 2. Add to JSON history (simple limited circular log)
        try {
            val logs = getCallLogs()
            val newEntry = "{\"number\":\"$number\",\"time\":\"$timeStr\",\"status\":\"$status\"}"
            val updatedLogs = if (logs == "[]") "[$newEntry]" else {
                 val current = logs.substring(1, logs.length - 1)
                 val list = current.split("},{")
                 val lastN = list.takeLast(49) // Keep last 50
                 "[$newEntry,${lastN.joinToString("},{")}]"
            }
            prefs.edit().putString(KEY_CALL_LOGS, updatedLogs).apply()
        } catch (e: Exception) {
            // Fallback if JSON manual slicing fail
        }
    }
}
