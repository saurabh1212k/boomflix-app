package com.example.boomflix

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.boomflix.notification.NotificationSender
import com.example.boomflix.theme.BOOMFLIXTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val mediaType = intent?.getStringExtra(NotificationSender.EXTRA_MEDIA_TYPE)
        val mediaId = intent?.getIntExtra(NotificationSender.EXTRA_MEDIA_ID, -1) ?: -1
        val mediaTitle = intent?.getStringExtra(NotificationSender.EXTRA_MEDIA_TITLE) ?: ""
        val seekPos = intent?.getLongExtra(NotificationSender.EXTRA_SEEK_POSITION, 0L) ?: 0L
        val preferredServer = intent?.getStringExtra(NotificationSender.EXTRA_PREFERRED_SERVER)

        val deepLink = if (!mediaType.isNullOrBlank() && mediaId > 0) {
            DeepLinkMedia(
                type = mediaType,
                id = mediaId,
                title = mediaTitle,
                seekPositionMs = seekPos,
                preferredServer = preferredServer
            )
        } else null

        setContent {
            BOOMFLIXTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigation(initialDeepLink = deepLink)
                }
            }
        }
    }
}
