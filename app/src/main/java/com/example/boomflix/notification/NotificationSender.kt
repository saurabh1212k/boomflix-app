package com.example.boomflix.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.boomflix.MainActivity

object NotificationSender {

    const val CHANNEL_ID = "boomflix_notifications"
    private const val CHANNEL_NAME = "BOOMFLIX Alerts"
    private const val CHANNEL_DESCRIPTION = "Updates, new releases, and continue watching reminders"

    // Deep link keys
    const val EXTRA_MEDIA_TYPE = "extra_media_type"
    const val EXTRA_MEDIA_ID = "extra_media_id"
    const val EXTRA_MEDIA_TITLE = "extra_media_title"
    const val EXTRA_SEEK_POSITION = "extra_seek_position"
    const val EXTRA_PREFERRED_SERVER = "extra_preferred_server"

    fun initChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun sendNotification(
        context: Context,
        title: String,
        message: String,
        mediaType: String? = null,
        mediaId: Int? = null,
        mediaTitle: String? = null,
        seekPositionMs: Long = 0L,
        preferredServer: String? = null,
        notificationId: Int = (System.currentTimeMillis() % 10000).toInt()
    ): Boolean {
        initChannel(context)

        if (!hasNotificationPermission(context)) {
            return false
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (mediaType != null) putExtra(EXTRA_MEDIA_TYPE, mediaType)
            if (mediaId != null && mediaId > 0) putExtra(EXTRA_MEDIA_ID, mediaId)
            if (mediaTitle != null) putExtra(EXTRA_MEDIA_TITLE, mediaTitle)
            if (seekPositionMs > 0L) putExtra(EXTRA_SEEK_POSITION, seekPositionMs)
            if (preferredServer != null) putExtra(EXTRA_PREFERRED_SERVER, preferredServer)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColor(0xFFE50914.toInt()) // BOOMFLIX Red
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(notificationId, builder.build())
            return true
        } catch (_: SecurityException) {
            return false
        }
    }

    fun sendAutomatedNotification(context: Context, notif: GeneratedNotification): Boolean {
        return sendNotification(
            context = context,
            title = notif.title,
            message = notif.message,
            mediaType = notif.mediaType,
            mediaId = notif.mediaId,
            mediaTitle = notif.mediaTitle,
            seekPositionMs = notif.seekPositionMs,
            preferredServer = notif.preferredServer
        )
    }

    fun sendTestNotification(context: Context): Boolean {
        return sendNotification(
            context = context,
            title = "BOOMFLIX Streaming Alert",
            message = "New high-speed streaming servers are online. Enjoy smooth 4K streaming without buffering!"
        )
    }
}
