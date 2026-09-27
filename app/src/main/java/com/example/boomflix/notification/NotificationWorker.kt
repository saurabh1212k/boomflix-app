package com.example.boomflix.notification

import android.content.Context
import android.content.SharedPreferences
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.util.Calendar

class NotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "boomflix_automated_notifications"
        private const val PREFS_NAME = "boomflix_notifications_prefs"
        private const val KEY_LAST_SENT_TIME = "last_notification_time_ms"
        private const val MIN_INTERVAL_MS = 4 * 60 * 60 * 1000L // 4 hours frequency cap
    }

    override suspend fun doWork(): Result {
        if (!NotificationSender.hasNotificationPermission(applicationContext)) {
            return Result.success()
        }

        // Quiet hours check: Suppress notifications between 11:30 PM and 8:30 AM
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        if (currentHour < 8 || (currentHour == 8 && currentMinute < 30) || (currentHour == 23 && currentMinute >= 30)) {
            return Result.success()
        }

        // Frequency cap check
        val prefs: SharedPreferences = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastSentTime = prefs.getLong(KEY_LAST_SENT_TIME, 0L)
        val now = System.currentTimeMillis()
        if (now - lastSentTime < MIN_INTERVAL_MS) {
            return Result.success()
        }

        val generated = AiNotificationEngine.generateNextNotification(applicationContext)
        if (generated != null) {
            val sent = NotificationSender.sendAutomatedNotification(applicationContext, generated)
            if (sent) {
                prefs.edit().putLong(KEY_LAST_SENT_TIME, now).apply()
            }
        }

        return Result.success()
    }
}
