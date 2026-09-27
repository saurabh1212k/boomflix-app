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
    val imageUrl: String? = null,
    val seekPositionMs: Long = 0L,
    val preferredServer: String? = null,
    val isTrendingAlert: Boolean = false
)

object AiNotificationEngine {

    private const val PREFS_NAME = "boomflix_trending_prefs"
    private const val KEY_LAST_TRENDING_ID = "last_trending_movie_id"
    private const val KEY_LAST_TRENDING_TITLE = "last_trending_movie_title"
    private const val KEY_LAST_NOTIFIED_TIME = "last_trending_notified_time"

    suspend fun checkTrendingMovieChange(context: Context): GeneratedNotification? = withContext(Dispatchers.IO) {
        try {
            val repository = MediaRepository()
            val trendingMovies = repository.getTrending(mediaType = "movie", timeWindow = "day")
            val currentTop = trendingMovies.firstOrNull() ?: return@withContext null

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val lastTrendingId = prefs.getInt(KEY_LAST_TRENDING_ID, -1)
            val lastNotifiedTime = prefs.getLong(KEY_LAST_NOTIFIED_TIME, 0L)
            val now = System.currentTimeMillis()

            val hasChanged = lastTrendingId != currentTop.id
            val isExpired = (now - lastNotifiedTime) > (20 * 60 * 60 * 1000L) // 20 hours threshold

            if (hasChanged || isExpired) {
                val titleClean = currentTop.displayTitle
                val ratingStr = if (currentTop.voteAverage > 0.0) String.format("%.1f", currentTop.voteAverage) else "8.5"
                val calendar = Calendar.getInstance()
                val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
                val isEvening = hourOfDay in 18..23

                val heading = if (hasChanged && lastTrendingId != -1) {
                    if (isEvening) "Tonight's New #1 Trending: $titleClean" else "New #1 Trending: $titleClean"
                } else {
                    if (isEvening) "Trending Tonight: $titleClean" else "Trending Now: $titleClean"
                }

                val body = if (hasChanged && lastTrendingId != -1) {
                    "$titleClean just claimed the #1 spot on BOOMFLIX today (Rating: $ratingStr/10). Stream it now in HD!"
                } else {
                    "$titleClean is today's most streamed movie on BOOMFLIX (Rating: $ratingStr/10). Tap to watch now."
                }

                val image = currentTop.backdropUrl ?: currentTop.posterUrl

                prefs.edit()
                    .putInt(KEY_LAST_TRENDING_ID, currentTop.id)
                    .putString(KEY_LAST_TRENDING_TITLE, titleClean)
                    .putLong(KEY_LAST_NOTIFIED_TIME, now)
                    .apply()

                return@withContext GeneratedNotification(
                    title = heading,
                    message = body,
                    mediaType = "movie",
                    mediaId = currentTop.id,
                    mediaTitle = titleClean,
                    imageUrl = image,
                    isTrendingAlert = true
                )
            }
        } catch (_: Exception) {}

        null
    }

    suspend fun getTrendingTestNotification(context: Context): GeneratedNotification = withContext(Dispatchers.IO) {
        try {
            val repository = MediaRepository()
            val trendingMovies = repository.getTrending(mediaType = "movie", timeWindow = "day")
            val topItem = trendingMovies.firstOrNull() ?: repository.getTopRatedMovies().firstOrNull()

            if (topItem != null) {
                val titleClean = topItem.displayTitle
                val ratingStr = if (topItem.voteAverage > 0.0) String.format("%.1f", topItem.voteAverage) else "8.8"
                val image = topItem.backdropUrl ?: topItem.posterUrl

                return@withContext GeneratedNotification(
                    title = "New #1 Trending: $titleClean",
                    message = "$titleClean is trending #1 today (Rating: $ratingStr/10). Tap to stream now in 4K!",
                    mediaType = "movie",
                    mediaId = topItem.id,
                    mediaTitle = titleClean,
                    imageUrl = image,
                    isTrendingAlert = true
                )
            }
        } catch (_: Exception) {}

        GeneratedNotification(
            title = "BOOMFLIX Trending Alert",
            message = "New high-speed 4K streaming servers are online. Discover today's trending movies now!",
            mediaType = "movie",
            mediaId = -1,
            mediaTitle = "BOOMFLIX",
            isTrendingAlert = true
        )
    }

    suspend fun generateNextNotification(context: Context): GeneratedNotification? = withContext(Dispatchers.IO) {
        // Priority 1: Trending Movie Change Detection
        val trendingChange = checkTrendingMovieChange(context)
        if (trendingChange != null) {
            return@withContext trendingChange
        }

        // Priority 2: Smart Resume from Continue Watching
        try {
            val db = BoomflixDatabase.getInstance(context)
            val continueWatchingList = db.continueWatchingDao().getAll()

            val calendar = Calendar.getInstance()
            val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
            val isEvening = hourOfDay in 18..23

            val candidate = continueWatchingList
                .filter { it.progress in 0.08f..0.88f }
                .maxByOrNull { it.updatedAt }

            if (candidate != null) {
                val minutesLeft = ((candidate.duration - candidate.currentTime) / 60_000L).coerceAtLeast(1L)
                val titleClean = candidate.title.trim()

                val heading = if (isEvening) "Tonight on BOOMFLIX" else "Continue Watching"
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
                    imageUrl = candidate.backdropPath ?: candidate.posterPath,
                    seekPositionMs = candidate.currentTime,
                    preferredServer = candidate.serverId
                )
            }
        } catch (_: Exception) {}

        null
    }
}
