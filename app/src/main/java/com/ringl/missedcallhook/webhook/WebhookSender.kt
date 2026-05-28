package com.ringl.missedcallhook.webhook

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ringl.missedcallhook.util.PhoneUtils
import com.ringl.missedcallhook.util.PrefsManager
import java.util.concurrent.TimeUnit

/**
 * Handles sending missed call webhooks to the backend.
 * Uses WorkManager for reliable delivery with automatic retries.
 */
object WebhookSender {

    private const val TAG = "Ringl"

    // Deduplication: Don't send the same number twice within 60 seconds
    private val recentCalls = mutableMapOf<String, Long>()
    private const val DEDUP_WINDOW_MS = 60_000L

    /**
     * Queue a missed call webhook for delivery.
     * Deduplicates, normalizes the number, and dispatches via WorkManager.
     */
    fun send(context: Context, phoneNumber: String, timestamp: Long) {
        val normalized = PhoneUtils.normalize(phoneNumber)

        // Dedup check
        val lastSent = recentCalls[normalized] ?: 0
        if (System.currentTimeMillis() - lastSent < DEDUP_WINDOW_MS) {
            Log.d(TAG, "⏭️ Skipping duplicate for $normalized (sent ${(System.currentTimeMillis() - lastSent) / 1000}s ago)")
            return
        }
        recentCalls[normalized] = System.currentTimeMillis()

        // Cleanup old entries
        val cutoff = System.currentTimeMillis() - DEDUP_WINDOW_MS
        recentCalls.entries.removeAll { it.value < cutoff }

        // Update prefs for UI stats
        val prefs = PrefsManager(context)
        prefs.recordMissedCall(normalized)

        Log.d(TAG, "📤 Queueing webhook for $normalized")

        // Use WorkManager for reliable delivery
        val request = OneTimeWorkRequestBuilder<WebhookWorker>()
            .setInputData(
                workDataOf(
                    WebhookWorker.KEY_PHONE to normalized,
                    WebhookWorker.KEY_TIMESTAMP to timestamp
                )
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30, TimeUnit.SECONDS  // Retry: 30s → 60s → 120s → ...
            )
            .build()

        WorkManager.getInstance(context).enqueue(request)
    }
}
