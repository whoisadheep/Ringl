package com.ringl.missedcallhook.webhook

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ringl.missedcallhook.util.PrefsManager
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * WorkManager worker that reliably sends the webhook HTTP request.
 * Automatically retries with exponential backoff on failure.
 * Survives app kills and device reboots.
 */
class WebhookWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "Ringl"
        const val KEY_PHONE = "phone"
        const val KEY_TIMESTAMP = "timestamp"
        private const val MAX_RETRIES = 5
    }

    override suspend fun doWork(): Result {
        val phone = inputData.getString(KEY_PHONE) ?: return Result.failure()
        val timestamp = inputData.getLong(KEY_TIMESTAMP, System.currentTimeMillis())

        val prefs = PrefsManager(applicationContext)
        val webhookUrl = prefs.getWebhookUrl()
        val webhookSecret = prefs.getWebhookSecret()
        val tenantId = prefs.getTenantId()

        if (webhookUrl.isEmpty()) {
            Log.e(TAG, "❌ No webhook URL configured — skipping")
            return Result.failure()
        }

        return try {
            val responseCode = sendHttpPost(webhookUrl, webhookSecret, phone, timestamp, tenantId)

            if (responseCode in 200..299) {
                Log.d(TAG, "✅ Webhook delivered for $phone → HTTP $responseCode")
                prefs.incrementWebhooksSent()
                Result.success()
            } else if (responseCode in 400..499) {
                // Client error — don't retry (bad request, unauthorized, etc.)
                Log.e(TAG, "❌ Webhook rejected for $phone → HTTP $responseCode (not retrying)")
                Result.failure()
            } else {
                // Server error — retry
                Log.w(TAG, "⚠️ Webhook server error for $phone → HTTP $responseCode (attempt ${runAttemptCount + 1})")
                if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Webhook network error for $phone: ${e.message}")
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        }
    }

    private fun sendHttpPost(
        webhookUrl: String,
        webhookSecret: String,
        phone: String,
        timestamp: Long,
        tenantId: String
    ): Int {
        val url = URL(webhookUrl)
        val connection = url.openConnection() as HttpURLConnection

        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("X-Webhook-Secret", webhookSecret)
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000
        connection.doOutput = true

        val payload = """
            {
                "phone": "$phone",
                "timestamp": $timestamp,
                "event": "missed_call",
                "source": "ringl_android",
                "tenant_id": "$tenantId"
            }
        """.trimIndent()

        OutputStreamWriter(connection.outputStream).use { writer ->
            writer.write(payload)
            writer.flush()
        }

        val code = connection.responseCode
        connection.disconnect()
        return code
    }
}
