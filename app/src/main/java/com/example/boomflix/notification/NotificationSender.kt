package com.example.boomflix.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.boomflix.MainActivity
import com.example.boomflix.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object NotificationSender {

    const val CHANNEL_ID = "boomflix_notifications"
    private const val CHANNEL_NAME = "BOOMFLIX Alerts"
    private const val CHANNEL_DESCRIPTION = "Updates, trending releases, and continue watching reminders"

    // Deep link keys
    const val EXTRA_MEDIA_TYPE = "extra_media_type"
    const val EXTRA_MEDIA_ID = "extra_media_id"
    const val EXTRA_MEDIA_TITLE = "extra_media_title"
    const val EXTRA_SEEK_POSITION = "extra_seek_position"
    const val EXTRA_PREFERRED_SERVER = "extra_preferred_server"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    fun initChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
                lightColor = 0xFFE50914.toInt()
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
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

    private suspend fun fetchBitmap(url: String?): Bitmap? = withContext(Dispatchers.IO) {
        if (url.isNullOrBlank()) return@withContext null
        try {
            val request = Request.Builder().url(url).build()
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bytes = response.body?.bytes()
                    if (bytes != null) {
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    } else null
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun sendNotification(
        context: Context,
        title: String,
        message: String,
        mediaType: String? = null,
        mediaId: Int? = null,
        mediaTitle: String? = null,
        imageUrl: String? = null,
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

        val imageBitmap = fetchBitmap(imageUrl)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.boomflix_b)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(0xFFE50914.toInt()) // BOOMFLIX Red
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (imageBitmap != null) {
            builder.setLargeIcon(imageBitmap)
            builder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(imageBitmap)
                    .setBigContentTitle(title)
                    .setSummaryText(message)
            )
        } else {
            try {
                val emblemBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.boomflix_b)
                if (emblemBitmap != null) {
                    builder.setLargeIcon(emblemBitmap)
                }
            } catch (_: Exception) {}
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(message))
        }

        // Add direct "Watch Now" action button if target title is known
        if (mediaId != null && mediaId > 0) {
            val watchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_MEDIA_TYPE, mediaType ?: "movie")
                putExtra(EXTRA_MEDIA_ID, mediaId)
                putExtra(EXTRA_MEDIA_TITLE, mediaTitle ?: "")
                if (seekPositionMs > 0L) putExtra(EXTRA_SEEK_POSITION, seekPositionMs)
                if (preferredServer != null) putExtra(EXTRA_PREFERRED_SERVER, preferredServer)
            }

            val watchPendingIntent = PendingIntent.getActivity(
                context,
                notificationId + 100,
                watchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            builder.addAction(
                android.R.drawable.ic_media_play,
                "Watch Now",
                watchPendingIntent
            )
        }

        return try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(notificationId, builder.build())
            true
        } catch (_: SecurityException) {
            false
        }
    }

    suspend fun sendAutomatedNotification(context: Context, notif: GeneratedNotification): Boolean {
        return sendNotification(
            context = context,
            title = notif.title,
            message = notif.message,
            mediaType = notif.mediaType,
            mediaId = notif.mediaId,
            mediaTitle = notif.mediaTitle,
            imageUrl = notif.imageUrl,
            seekPositionMs = notif.seekPositionMs,
            preferredServer = notif.preferredServer
        )
    }

    suspend fun sendTestNotification(context: Context): Boolean {
        val testNotif = AiNotificationEngine.getTrendingTestNotification(context)
        return sendAutomatedNotification(context, testNotif)
    }
}
