package com.example.boomflix.notification

import android.content.Context
import com.example.boomflix.data.local.BoomflixDatabase
import com.example.boomflix.data.local.ContinueWatchingEntity
import com.example.boomflix.data.models.MediaItem
import com.example.boomflix.data.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

data class GeneratedNotification(
    val title: String,
    val message: String,
    val mediaType: String? = null,
    val mediaId: Int? = null,
    val mediaTitle: String? = null,
    val seekPositionMs: Long = 0L,
    val preferredServer: String? = null
)

object AiNotificationEngine {

    suspend fun generateNextNotification(context: Context): GeneratedNotification? = withContext(Dispatchers.IO) {
        val db = BoomflixDatabase.getInstance(context)
        val continueWatchingList = db.continueWatchingDao().getAll()
        val repository = MediaRepository()

        val calendar = Calendar.getInstance()
        val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
        val isEvening = hourOfDay in 18..23

        // Strategy 1: Smart Resume from Continue Watching
        val candidate = continueWatchingList
            .filter { it.progress in 0.08f..0.88f }
            .maxByOrNull { it.updatedAt }

        if (candidate != null) {
            val minutesLeft = ((candidate.duration - candidate.currentTime) / 60_000L).coerceAtLeast(1L)
            val titleClean = candidate.title.trim()

            val heading = if (isEvening) {
                "Tonight on BOOMFLIX"
            } else {
                "Continue Watching"
            }

            val body = if (candidate.type == "tv" && candidate.season != null && candidate.episode != null) {
                "Resume Season ${candidate.season}, Episode ${candidate.episode} of $titleClean. Only $minutesLeft minutes remaining."
            } else {
                "Pick up where you left off in $titleClean. $minutesLeft minutes remaining."
            }

            return@withContext GeneratedNotification(
                title = heading,
                message = body,
                mediaType = candidate.type,
                mediaId = candidate.mediaId.toIntOrNull(),
                mediaTitle = candidate.title,
                seekPositionMs = candidate.currentTime,
                preferredServer = candidate.serverId
            )
        }

        // Strategy 2: Trending Title Recommendation
        val trendingList = repository.getTrending(timeWindow = "day")
        val topItem = trendingList.firstOrNull()

        if (topItem != null) {
            val isMovie = topItem.title != null
            val mediaType = if (isMovie) "movie" else "tv"
            val displayTitle = topItem.displayTitle

            val heading = if (isEvening) "Trending Tonight" else "Popular on BOOMFLIX"
            val body = "$displayTitle is today's most streamed ${if (isMovie) "movie" else "show"}. Stream it now in HD."

            return@withContext GeneratedNotification(
                title = heading,
                message = body,
                mediaType = mediaType,
                mediaId = topItem.id,
                mediaTitle = displayTitle
            )
        }

        null
    }
}
