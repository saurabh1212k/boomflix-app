package com.example.boomflix.ui.screens

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.boomflix.data.local.BoomflixDatabase
import com.example.boomflix.data.local.ContinueWatchingEntity
import com.example.boomflix.data.models.ServerItem
import com.example.boomflix.player.CdnHeaderInterceptor
import com.example.boomflix.player.PlayerViewModel
import com.example.boomflix.theme.BoomflixRed
import com.example.boomflix.theme.BoomflixGreyButton
import com.example.boomflix.theme.BoomflixChipSelected
import com.example.boomflix.theme.BoomflixWhiteButton
import com.example.boomflix.ui.components.BoomflixLogo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import okhttp3.OkHttpClient

data class AudioTrackItem(
    val id: String,
    val name: String,
    val lang: String,
    val isSelected: Boolean,
    val trackGroup: TrackGroup? = null,
    val trackIndex: Int = 0
)

data class SubtitleTrackItem(
    val id: String,
    val name: String,
    val lang: String,
    val isSelected: Boolean,
    val trackGroup: TrackGroup? = null,
    val trackIndex: Int = 0
)

data class SubtitleCue(
    val startMs: Long,
    val endMs: Long,
    val text: String
)

object SubtitleParser {
    private val TIME_REGEX = Regex("""(?:(\d{1,2}):)?(\d{2}):(\d{2})[.,](\d{3})""")

    private fun parseTimestamp(str: String): Long? {
        val match = TIME_REGEX.find(str.trim()) ?: return null
        val hours = match.groupValues[1].ifEmpty { "0" }.toLong()
        val minutes = match.groupValues[2].toLong()
        val seconds = match.groupValues[3].toLong()
        val millis = match.groupValues[4].toLong()
        return hours * 3600000L + minutes * 60000L + seconds * 1000L + millis
    }

    fun parse(content: String): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()
        val lines = content.lines()
        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()
            if (line.contains("-->")) {
                val parts = line.split("-->")
                if (parts.size >= 2) {
                    val start = parseTimestamp(parts[0])
                    val endPart = parts[1].trim().split(Regex("""\s+"""))[0]
                    val end = parseTimestamp(endPart)
                    if (start != null && end != null) {
                        val textLines = mutableListOf<String>()
                        i++
                        while (i < lines.size && lines[i].isNotBlank()) {
                            var cleanLine = lines[i].replace(Regex("<[^>]*>"), "").trim()
                            cleanLine = cleanLine
                                .replace("&amp;", "&")
                                .replace("&#39;", "'")
                                .replace("&quot;", "\"")
                                .replace("&lt;", "<")
                                .replace("&gt;", ">")
                                .replace("&lrm;", "")
                                .replace("&rlm;", "")
                            if (cleanLine.isNotEmpty() && !cleanLine.startsWith("NOTE") && !cleanLine.startsWith("STYLE")) {
                                textLines.add(cleanLine)
                            }
                            i++
                        }
                        if (textLines.isNotEmpty()) {
                            cues.add(SubtitleCue(start, end, textLines.joinToString("\n")))
                        }
                        continue
                    }
                }
            }
            i++
        }
        cues.sortBy { it.startMs }
        return cues
    }

    fun findActiveCue(cues: List<SubtitleCue>, timeMs: Long): String? {
        if (cues.isEmpty()) return null
        var low = 0
        var high = cues.size - 1
        var candidateIndex = -1
        while (low <= high) {
            val mid = (low + high) ushr 1
            val cue = cues[mid]
            if (timeMs < cue.startMs) {
                high = mid - 1
            } else if (timeMs > cue.endMs) {
                low = mid + 1
            } else {
                candidateIndex = mid
                break
            }
        }
        if (candidateIndex == -1) return null

        val result = mutableListOf<String>()
        var left = candidateIndex
        while (left >= 0 && cues[left].endMs >= timeMs) {
            if (cues[left].startMs <= timeMs) {
                result.add(0, cues[left].text)
            }
            left--
        }
        var right = candidateIndex + 1
        while (right < cues.size && cues[right].startMs <= timeMs) {
            if (timeMs <= cues[right].endMs) {
                result.add(cues[right].text)
            }
            right++
        }
        return if (result.isEmpty()) null else result.distinct().joinToString("\n")
    }
}

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    type: String,
    id: Int,
    title: String,
    year: String = "",
    season: Int = -1,
    episode: Int = -1,
    isAnime: Boolean = false,
    startPositionMs: Long = 0L,
    preferredServer: String? = null,
    posterPath: String? = null,
    backdropPath: String? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = viewModel(key = "player_${type}_${id}_${season}_${episode}")
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val currentStream by viewModel.currentStream.collectAsState()
    val mediaDetails by viewModel.mediaDetails.collectAsState()
    val subtitles by viewModel.subtitles.collectAsState()
    val isExtracting by viewModel.isExtracting.collectAsState()
    val isSwitchingServer by viewModel.isSwitchingServer.collectAsState()
    val error by viewModel.error.collectAsState()
    val servers by viewModel.servers.collectAsState()
    val activeServerId by viewModel.activeServerId.collectAsState()
    val activeServerName by viewModel.activeServerName.collectAsState()

    // Playback state tracking (keyed to current media)
    var isPlaying by remember(type, id, season, episode) { mutableStateOf(false) }
    var isBuffering by remember(type, id, season, episode) { mutableStateOf(false) }
    var currentPositionMs by remember(type, id, season, episode) { mutableLongStateOf(0L) }
    var totalDurationMs by remember(type, id, season, episode) { mutableLongStateOf(0L) }
    var showControls by remember(type, id, season, episode) { mutableStateOf(true) }

    // Audio volume & playback speed
    var isMuted by remember(type, id, season, episode) { mutableStateOf(false) }
    var playbackSpeed by remember(type, id, season, episode) { mutableFloatStateOf(1.0f) }

    // Resume position tracking
    var hasRestoredPosition by remember(type, id, season, episode) { mutableStateOf(false) }
    var resumeNotification by remember(type, id, season, episode) { mutableStateOf<String?>(null) }

    // Settings Modal (Unified Quality, Audio, Subtitles, Speed, Servers)
    var showSettingsDialog by remember(type, id, season, episode) { mutableStateOf(false) }
    var settingsTab by remember(type, id, season, episode) { mutableStateOf("quality") }

    // Scrubber
    var isUserScrubbing by remember(type, id, season, episode) { mutableStateOf(false) }
    var scrubPositionMs by remember(type, id, season, episode) { mutableFloatStateOf(0f) }

    // Option to access progress bar area when paused
    var showPausedProgressArea by remember(type, id, season, episode) { mutableStateOf(false) }

    // Fullscreen / Aspect Ratio mode (Fit vs Zoom/Fill)
    var resizeMode by remember(type, id, season, episode) { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var aspectModeNotification by remember(type, id, season, episode) { mutableStateOf<String?>(null) }

    // Track selection states
    var detectedQualities by remember(type, id, season, episode) { mutableStateOf<List<Int>>(emptyList()) }
    var selectedQualityHeight by remember(type, id, season, episode) { mutableStateOf<Int?>(null) }

    var audioTracksList by remember(type, id, season, episode) { mutableStateOf<List<AudioTrackItem>>(emptyList()) }
    var selectedAudioTrackId by remember(type, id, season, episode) { mutableStateOf("default") }

    var subtitleTracksList by remember(type, id, season, episode) { mutableStateOf<List<SubtitleTrackItem>>(emptyList()) }
    var isSubtitlesOff by remember(type, id, season, episode) { mutableStateOf(false) }
    var selectedSubtitleTrackId by remember(type, id, season, episode) { mutableStateOf<String?>(null) }
    var subtitleOffsetSeconds by remember(type, id, season, episode) { mutableFloatStateOf(0f) }
    var subtitleFontSize by remember(type, id, season, episode) { mutableIntStateOf(16) }
    var customCues by remember(type, id, season, episode) { mutableStateOf<List<SubtitleCue>>(emptyList()) }
    var inStreamCueText by remember(type, id, season, episode) { mutableStateOf<String?>(null) }
    val subtitleCache = remember(type, id, season, episode) { mutableMapOf<String, List<SubtitleCue>>() }

    // Lock to landscape & enable system bars immersive sticky mode
    DisposableEffect(activity) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        val window = activity?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            val window = activity?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Auto-hide controls after inactivity when playing
    LaunchedEffect(showControls, isPlaying, showSettingsDialog) {
        if (showControls && isPlaying && !showSettingsDialog) {
            delay(4000)
            showControls = false
        }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            showPausedProgressArea = false
        }
    }

    // Auto-clear pill notifications
    LaunchedEffect(aspectModeNotification) {
        if (aspectModeNotification != null) {
            delay(2000)
            aspectModeNotification = null
        }
    }

    LaunchedEffect(resumeNotification) {
        if (resumeNotification != null) {
            delay(3000)
            resumeNotification = null
        }
    }

    // Extraction starts immediately on mount with preferredServer if available
    LaunchedEffect(type, id, season, episode) {
        viewModel.extractStreams(type, id, title, year, season, episode, isAnime, preferredServer)
    }

    // OkHttp client configured with CDN interceptor & DNS bypass
    val okHttpClient = remember {
        OkHttpClient.Builder()
            .dns(com.example.boomflix.data.api.TmdbDns)
            .addInterceptor(CdnHeaderInterceptor)
            .build()
    }

    // ExoPlayer instance scoped per media item
    val player = remember(type, id, season, episode) {
        val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
    }

    // Player event listener
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (!playing) {
                    showControls = false
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    totalDurationMs = player.duration.coerceAtLeast(0L)

                    // Pick right where user left off
                    if (!hasRestoredPosition && startPositionMs > 1000L) {
                        hasRestoredPosition = true
                        val targetSeek = startPositionMs.coerceAtMost(player.duration.coerceAtLeast(0L))
                        if (targetSeek > 0) {
                            player.seekTo(targetSeek)
                            resumeNotification = "Resumed from ${formatDuration(targetSeek)}"
                        }
                    }
                }
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                currentPositionMs = newPosition.positionMs.coerceAtLeast(0L)
            }

            override fun onCues(cueGroup: CueGroup) {
                val text = cueGroup.cues.mapNotNull { it.text?.toString() }.joinToString("\n").trim()
                inStreamCueText = text.ifEmpty { null }
            }

            override fun onTracksChanged(tracks: Tracks) {
                val heights = mutableSetOf<Int>()
                val audios = mutableListOf<AudioTrackItem>()
                val inStreamSubs = mutableListOf<SubtitleTrackItem>()

                for (group in tracks.groups) {
                    when (group.type) {
                        C.TRACK_TYPE_VIDEO -> {
                            for (i in 0 until group.length) {
                                val h = group.getTrackFormat(i).height
                                if (h > 0) heights.add(h)
                            }
                        }
                        C.TRACK_TYPE_AUDIO -> {
                            for (i in 0 until group.length) {
                                val fmt = group.getTrackFormat(i)
                                val lang = fmt.language ?: ""
                                val label = fmt.label ?: if (lang.isNotBlank()) getLanguageDisplayName(lang) else "Track ${i + 1}"
                                audios.add(
                                    AudioTrackItem(
                                        id = "audio_${audios.size}",
                                        name = label,
                                        lang = lang,
                                        isSelected = group.isTrackSelected(i),
                                        trackGroup = group.mediaTrackGroup,
                                        trackIndex = i
                                    )
                                )
                            }
                        }
                        C.TRACK_TYPE_TEXT -> {
                            for (i in 0 until group.length) {
                                val fmt = group.getTrackFormat(i)
                                val lang = fmt.language ?: ""
                                val label = fmt.label ?: if (lang.isNotBlank()) getLanguageDisplayName(lang) else "Subtitle ${i + 1}"
                                inStreamSubs.add(
                                    SubtitleTrackItem(
                                        id = "sub_${inStreamSubs.size}",
                                        name = label,
                                        lang = lang,
                                        isSelected = group.isTrackSelected(i),
                                        trackGroup = group.mediaTrackGroup,
                                        trackIndex = i
                                    )
                                )
                            }
                        }
                    }
                }

                detectedQualities = heights.sortedDescending()
                if (audios.isNotEmpty()) {
                    audioTracksList = audios
                }
                if (inStreamSubs.isNotEmpty()) {
                    subtitleTracksList = inStreamSubs
                }
            }

            override fun onPlayerError(playerError: PlaybackException) {
                android.util.Log.w("PlayerScreen", "ExoPlayer playback error: ${playerError.errorCodeName} - ${playerError.message}", playerError)
                viewModel.onPermanentStreamFailure()
            }
        }

        player.addListener(listener)
        onDispose {
            // Save final progress on exit
            val pos = player.currentPosition
            val dur = player.duration
            if (dur > 0 && pos > 5_000L) {
                val progressFraction = (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
                CoroutineScope(Dispatchers.IO).launch {
                    val db = BoomflixDatabase.getInstance(context)
                    if (progressFraction > 0.95f) {
                        db.continueWatchingDao().delete(id.toString())
                    } else {
                        db.continueWatchingDao().upsert(
                            ContinueWatchingEntity(
                                mediaId = id.toString(),
                                type = type,
                                title = title,
                                posterPath = posterPath ?: mediaDetails?.posterPath,
                                backdropPath = backdropPath ?: mediaDetails?.backdropPath,
                                season = if (season > 0) season else null,
                                episode = if (episode > 0) episode else null,
                                progress = progressFraction,
                                currentTime = pos,
                                duration = dur,
                                serverId = activeServerId,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }
            player.removeListener(listener)
            player.stop()
            player.clearMediaItems()
            player.release()
            viewModel.resetState()
        }
    }

    // High-precision playback position ticker for sub-frame subtitle synchronization
    LaunchedEffect(player, isPlaying) {
        while (true) {
            if (!isUserScrubbing) {
                currentPositionMs = player.currentPosition.coerceAtLeast(0L)
                val dur = player.duration.coerceAtLeast(0L)
                if (dur > 0L) totalDurationMs = dur
            }
            delay(if (isPlaying) 50L else 200L)
        }
    }

    // Background continue watching auto-save loop
    LaunchedEffect(player, activeServerId) {
        var lastSavedSec = 0L
        while (true) {
            if (!isUserScrubbing && player.playbackState == Player.STATE_READY) {
                val pos = player.currentPosition.coerceAtLeast(0L)
                val dur = player.duration.coerceAtLeast(0L)
                val curSec = pos / 1000L
                if (dur > 0 && Math.abs(curSec - lastSavedSec) >= 5L) {
                    lastSavedSec = curSec
                    val progressFraction = (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
                    CoroutineScope(Dispatchers.IO).launch {
                        val db = BoomflixDatabase.getInstance(context)
                        if (progressFraction > 0.95f) {
                            db.continueWatchingDao().delete(id.toString())
                        } else if (pos > 5_000L) {
                            db.continueWatchingDao().upsert(
                                ContinueWatchingEntity(
                                    mediaId = id.toString(),
                                    type = type,
                                    title = title,
                                    posterPath = posterPath ?: mediaDetails?.posterPath,
                                    backdropPath = backdropPath ?: mediaDetails?.backdropPath,
                                    season = if (season > 0) season else null,
                                    episode = if (episode > 0) episode else null,
                                    progress = progressFraction,
                                    currentTime = pos,
                                    duration = dur,
                                    serverId = activeServerId,
                                    updatedAt = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                }
            }
            delay(3000L)
        }
    }

    // Load stream when currentStream or subtitles change
    LaunchedEffect(currentStream, subtitles, isExtracting) {
        val stream = currentStream ?: return@LaunchedEffect
        if (isExtracting || stream.url.isBlank()) return@LaunchedEffect

        CdnHeaderInterceptor.registerStreamHeaders(stream.url, stream.headers)

        val mediaItemBuilder = MediaItem.Builder().setUri(stream.url)

        if (stream.type.equals("hls", ignoreCase = true) || stream.url.contains(".m3u8", ignoreCase = true)) {
            mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
        } else if (stream.type.equals("mp4", ignoreCase = true) || stream.url.contains(".mp4", ignoreCase = true)) {
            mediaItemBuilder.setMimeType(MimeTypes.VIDEO_MP4)
        }

        player.stop()
        player.setMediaItem(mediaItemBuilder.build())
        player.prepare()
        player.playWhenReady = true
    }

    // Fetch and parse active external subtitle for precision timeline sync
    LaunchedEffect(selectedSubtitleTrackId, subtitles, isSubtitlesOff) {
        if (isSubtitlesOff) {
            customCues = emptyList()
            return@LaunchedEffect
        }

        val targetSub = if (selectedSubtitleTrackId != null && selectedSubtitleTrackId!!.startsWith("ext_")) {
            subtitles.find { "ext_${it.lang}_${it.label}" == selectedSubtitleTrackId }
        } else if (selectedSubtitleTrackId == null && subtitles.isNotEmpty()) {
            subtitles.firstOrNull { it.lang.startsWith("en", ignoreCase = true) } ?: subtitles.firstOrNull()
        } else {
            null
        }

        if (targetSub != null && targetSub.url.isNotBlank()) {
            val cached = subtitleCache[targetSub.url]
            if (cached != null && cached.isNotEmpty()) {
                customCues = cached
            } else {
                withContext(Dispatchers.IO) {
                    try {
                        val req = okhttp3.Request.Builder()
                            .url(targetSub.url)
                            .addHeader("User-Agent", "BOOMFLIX-Android-App")
                            .build()
                        val resp = okHttpClient.newCall(req).execute()
                        if (resp.isSuccessful) {
                            val bodyString = resp.body?.string() ?: ""
                            val parsed = SubtitleParser.parse(bodyString)
                            withContext(Dispatchers.Main) {
                                subtitleCache[targetSub.url] = parsed
                                customCues = parsed
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("PlayerScreen", "Failed to parse subtitle: ${e.message}")
                    }
                }
            }
        } else {
            customCues = emptyList()
        }
    }

    val displayTitle = if (season > 0 && episode > 0) "$title S${season}E${episode}" else title
    val isPausedState = !isPlaying && !isBuffering && !isExtracting && currentStream != null

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (isPausedState) {
                            player.play()
                        } else {
                            showControls = !showControls
                        }
                    },
                    onDoubleTap = { offset ->
                        val screenWidth = size.width
                        if (offset.x < screenWidth * 0.35f) {
                            val newPos = (player.currentPosition - 10_000L).coerceAtLeast(0L)
                            currentPositionMs = newPos
                            player.seekTo(newPos)
                            resumeNotification = "-10s"
                        } else if (offset.x > screenWidth * 0.65f) {
                            val newPos = (player.currentPosition + 10_000L).coerceAtMost(player.duration.coerceAtLeast(0L))
                            currentPositionMs = newPos
                            player.seekTo(newPos)
                            resumeNotification = "+10s"
                        } else {
                            if (isPausedState) player.play() else showControls = !showControls
                        }
                    }
                )
            }
    ) {

        // Core Video Canvas
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    this.resizeMode = resizeMode
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    setKeepScreenOn(true)
                    subtitleView?.visibility = android.view.View.GONE
                }
            },
            update = { playerView ->
                playerView.resizeMode = resizeMode
                playerView.subtitleView?.visibility = android.view.View.GONE
            },
            modifier = Modifier.fillMaxSize()
        )

        // Real-time Subtitle Overlay with Live Timeline Sync Offset (-5s to +5s)
        val activeSubtitleText = remember(currentPositionMs, subtitleOffsetSeconds, customCues, inStreamCueText) {
            val effectiveTimeMs = currentPositionMs + (subtitleOffsetSeconds * 1000f).toLong()
            if (customCues.isNotEmpty()) {
                SubtitleParser.findActiveCue(customCues, effectiveTimeMs)
            } else {
                inStreamCueText
            }
        }
        if (!isSubtitlesOff && !activeSubtitleText.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        bottom = if (showControls || showPausedProgressArea) 100.dp else 36.dp,
                        start = 48.dp,
                        end = 48.dp
                    ),
                contentAlignment = Alignment.BottomCenter
            ) {
                Text(
                    text = activeSubtitleText,
                    color = Color.White,
                    fontSize = subtitleFontSize.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    lineHeight = (subtitleFontSize + 6).sp,
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color.Black,
                            offset = Offset(2f, 2f),
                            blurRadius = 4f
                        )
                    ),
                    modifier = Modifier
                        .background(Color(0xCC000000), RoundedCornerShape(4.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }

        // Notification Pills (Aspect ratio or Resume notification)
        val activePillText = resumeNotification ?: aspectModeNotification
        if (activePillText != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 20.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xCC000000),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x44FFFFFF))
                ) {
                    Text(
                        text = activePillText,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Extraction / Initial Loading Overlay (Netflix aesthetic)
        if (isExtracting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xF00A0A0A)),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    CircularProgressIndicator(
                        color = BoomflixRed,
                        strokeWidth = 3.5.dp,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (season > 0 && episode > 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Season $season : Episode $episode",
                            color = Color(0xFFB3B3B3),
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (!preferredServer.isNullOrBlank()) "Resuming stream..." else "Connecting to servers...",
                        color = Color(0xFF808080),
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Buffering / Server Switching Spinner
        if (!isExtracting && (isBuffering || isSwitchingServer)) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = BoomflixRed,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(46.dp)
                    )
                    if (isSwitchingServer) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Switching to $activeServerName...",
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Full Error State
        if (!isExtracting && error != null && currentStream == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xF50A0A0A)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = BoomflixRed,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Unable to play video",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error ?: "Server stream unreachable.",
                        color = Color(0xFFB3B3B3),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(
                            onClick = {
                                settingsTab = "servers"
                                showSettingsDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BoomflixGreyButton, contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Select Server", fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedButton(
                            onClick = onBack,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text("Go Back")
                        }
                    }
                }
            }
        }

        // PHOTO 1: Slide-in Paused Screen (comes with smooth slide transition when paused)
        AnimatedVisibility(
            visible = isPausedState && !showSettingsDialog,
            enter = slideInHorizontally(initialOffsetX = { -it / 2 }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { -it / 2 }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xF7050505),
                                Color(0xEB0A0A0A),
                                Color(0xBF0A0A0A),
                                Color(0x550A0A0A),
                                Color.Transparent
                            )
                        )
                    )
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        player.play()
                    }
                    .padding(horizontal = 28.dp, vertical = 16.dp)
            ) {
                // Top Action Bar for Paused Screen
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        BoomflixLogo(
                            iconSize = 22.dp,
                            fontSize = 17.sp
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Option / Button to access progress bar area while paused
                        Surface(
                            onClick = {
                                showPausedProgressArea = !showPausedProgressArea
                            },
                            shape = RoundedCornerShape(20.dp),
                            color = if (showPausedProgressArea) BoomflixChipSelected else Color(0x33FFFFFF)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LinearScale,
                                    contentDescription = "Progress Bar",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (showPausedProgressArea) "Hide Progress" else "Progress Bar",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Subtitles
                        IconButton(
                            onClick = {
                                settingsTab = "subtitles"
                                showSettingsDialog = true
                            },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ClosedCaption,
                                contentDescription = "Subtitles",
                                tint = if (!isSubtitlesOff) Color.White else Color(0xFF888888),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Settings
                        IconButton(
                            onClick = {
                                settingsTab = "quality"
                                showSettingsDialog = true
                            },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Cast
                        IconButton(
                            onClick = {
                                triggerCastIntent(context, currentStream?.url, currentStream?.type, title, currentPositionMs)
                            },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cast,
                                contentDescription = "Cast on TV",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Server selection badge
                        Surface(
                            onClick = {
                                settingsTab = "servers"
                                showSettingsDialog = true
                            },
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0x33FFFFFF)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Dns,
                                    contentDescription = "Servers",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = activeServerName,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.68f)
                        .padding(top = 44.dp, bottom = 12.dp)
                        .align(Alignment.CenterStart)
                ) {
                    // PAUSED header label
                    Text(
                        text = "PAUSED",
                        color = Color(0xFFB3B3B3),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.5.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Title Logo or Styled Title
                    val titleLogoUrl = mediaDetails?.logoUrl
                    if (!titleLogoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = titleLogoUrl,
                            contentDescription = title,
                            contentScale = ContentScale.Fit,
                            alignment = Alignment.CenterStart,
                            modifier = Modifier
                                .heightIn(min = 40.dp, max = 80.dp)
                                .fillMaxWidth(0.85f)
                        )
                    } else {
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp,
                            lineHeight = 44.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Tagline in italic
                    val tagline = mediaDetails?.tagline
                    if (!tagline.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = tagline,
                            color = Color(0xFFCCCCCC),
                            fontSize = 15.sp,
                            fontStyle = FontStyle.Italic,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Meta row: Year and Rating
                    val releaseYear = mediaDetails?.year?.ifBlank { year } ?: year
                    val rating = mediaDetails?.rating ?: "8.7"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (releaseYear.isNotBlank()) {
                            Text(
                                text = releaseYear,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "•",
                                color = Color(0xFF808080),
                                fontSize = 14.sp
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = rating,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Genre Pills
                    val genres = mediaDetails?.genres?.map { it.name }
                        ?: listOf(if (type == "tv") "Animation" else "Action", "Adventure")

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        genres.take(3).forEach { genreName ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF1E1E22),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333338))
                            ) {
                                Text(
                                    text = genreName,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Horizontal Accent Line
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(2.dp)
                            .background(Color(0xFF55555A))
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Overview Description
                    val overview = mediaDetails?.overview
                    if (!overview.isNullOrBlank()) {
                        Text(
                            text = overview,
                            color = Color(0xFFCCCCCC),
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Dedicated Progress Bar Area (Accessible via "Progress Bar" button when paused)
                AnimatedVisibility(
                    visible = showPausedProgressArea,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                ) {
                    val displayPositionMs = if (isUserScrubbing) {
                        scrubPositionMs.toLong()
                    } else {
                        currentPositionMs
                    }

                    Surface(
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                        color = Color(0xF2121216),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { /* Swallow click to prevent resuming */ }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 14.dp)
                        ) {
                            // Scrubber Slider Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = formatDuration(displayPositionMs),
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Slider(
                                    value = if (totalDurationMs > 0) {
                                        displayPositionMs.toFloat().coerceIn(0f, totalDurationMs.toFloat())
                                    } else {
                                        0f
                                    },
                                    onValueChange = { value ->
                                        isUserScrubbing = true
                                        scrubPositionMs = value
                                        currentPositionMs = value.toLong()
                                    },
                                    onValueChangeFinished = {
                                        val target = scrubPositionMs.toLong()
                                        currentPositionMs = target
                                        isUserScrubbing = false
                                        player.seekTo(target)
                                    },
                                    valueRange = 0f..(totalDurationMs.toFloat().coerceAtLeast(1f)),
                                    colors = SliderDefaults.colors(
                                        thumbColor = BoomflixRed,
                                        activeTrackColor = BoomflixRed,
                                        inactiveTrackColor = Color(0x55FFFFFF)
                                    ),
                                    modifier = Modifier.weight(1f)
                                )

                                Text(
                                    text = formatDuration(totalDurationMs),
                                    color = Color(0xFFB3B3B3),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Controls Row inside Progress Area: Left (Seek/Play), Right (Subtitles, Settings, Aspect Ratio)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left Controls: Seek -10s, Resume Play, Seek +10s
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            val newPos = (player.currentPosition - 10_000L).coerceAtLeast(0L)
                                            player.seekTo(newPos)
                                            currentPositionMs = newPos
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Replay10,
                                            contentDescription = "Rewind 10s",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            showPausedProgressArea = false
                                            player.play()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = BoomflixWhiteButton, contentColor = Color.Black),
                                        shape = RoundedCornerShape(20.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Resume",
                                            tint = Color.Black,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Resume", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }

                                    IconButton(
                                        onClick = {
                                            val newPos = (player.currentPosition + 10_000L).coerceAtMost(player.duration.coerceAtLeast(0L))
                                            player.seekTo(newPos)
                                            currentPositionMs = newPos
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Forward10,
                                            contentDescription = "Forward 10s",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                // Right Controls: Subtitles, Settings, Aspect Ratio
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Subtitles Quick Button
                                    IconButton(
                                        onClick = {
                                            settingsTab = "subtitles"
                                            showSettingsDialog = true
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ClosedCaption,
                                            contentDescription = "Subtitles",
                                            tint = if (!isSubtitlesOff) Color.White else Color(0xFF888888),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    // Settings Gear Icon (Quality, Audio, Subtitles, Speed, Servers)
                                    IconButton(
                                        onClick = {
                                            settingsTab = "quality"
                                            showSettingsDialog = true
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = "Settings",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    // Aspect Ratio / Fullscreen Toggle
                                    IconButton(
                                        onClick = {
                                            resizeMode = if (resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT) {
                                                aspectModeNotification = "Crop / Zoom (Fill Screen)"
                                                AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                            } else {
                                                aspectModeNotification = "Fit (Original Aspect Ratio)"
                                                AspectRatioFrameLayout.RESIZE_MODE_FIT
                                            }
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AspectRatio,
                                            contentDescription = "Aspect Ratio",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // PHOTO 2: Player Controls Overlay
        AnimatedVisibility(
            visible = showControls && !isExtracting && !isPausedState,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x73000000))
            ) {
                // Top Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xD9000000), Color.Transparent)
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Back Button
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        // Top Right: Cast & Server Button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Cast on TV
                            IconButton(
                                onClick = {
                                    triggerCastIntent(context, currentStream?.url, currentStream?.type, title, currentPositionMs)
                                },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cast,
                                    contentDescription = "Cast on TV",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // Server selection badge
                            Surface(
                                onClick = {
                                    settingsTab = "servers"
                                    showSettingsDialog = true
                                },
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0x33FFFFFF)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Dns,
                                        contentDescription = "Servers",
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = activeServerName,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // Lower Area: "You're Watching" block + Scrubber + Controls
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xF2000000))
                            )
                        )
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // "You're Watching" + Title + Badges
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(13.dp)
                                    .clip(RoundedCornerShape(1.5.dp))
                                    .background(BoomflixRed)
                            )
                            Text(
                                text = "You're Watching",
                                color = Color(0xFFB3B3B3),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = displayTitle,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Badges Row: [Series/Movie] [Genre] [Year]
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TagPill(if (type == "tv") "Series" else "Movie")
                            mediaDetails?.genres?.firstOrNull()?.name?.let { TagPill(it) }
                            val releaseYear = mediaDetails?.year?.ifBlank { year } ?: year
                            if (releaseYear.isNotBlank()) TagPill(releaseYear)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        val displayPositionMs = if (isUserScrubbing) {
                            scrubPositionMs.toLong()
                        } else {
                            currentPositionMs
                        }

                        // Scrubber Slider Bar with time labels
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = formatDuration(displayPositionMs),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Slider(
                                value = if (totalDurationMs > 0) {
                                    displayPositionMs.toFloat().coerceIn(0f, totalDurationMs.toFloat())
                                } else {
                                    0f
                                },
                                onValueChange = { value ->
                                    isUserScrubbing = true
                                    scrubPositionMs = value
                                    currentPositionMs = value.toLong()
                                },
                                onValueChangeFinished = {
                                    val target = scrubPositionMs.toLong()
                                    currentPositionMs = target
                                    isUserScrubbing = false
                                    player.seekTo(target)
                                },
                                valueRange = 0f..(totalDurationMs.toFloat().coerceAtLeast(1f)),
                                colors = SliderDefaults.colors(
                                    thumbColor = BoomflixRed,
                                    activeTrackColor = BoomflixRed,
                                    inactiveTrackColor = Color(0x55FFFFFF)
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            Text(
                                text = formatDuration(totalDurationMs),
                                color = Color(0xFFB3B3B3),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Bottom Action Controls (Photo 2)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left Controls Group
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Play / Pause
                                IconButton(
                                    onClick = {
                                        if (player.isPlaying) {
                                            player.pause()
                                            showControls = false
                                        } else {
                                            player.play()
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "Pause" else "Play",
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                // Rewind 10s
                                IconButton(
                                    onClick = {
                                        val newPos = (player.currentPosition - 10_000L).coerceAtLeast(0L)
                                        currentPositionMs = newPos
                                        player.seekTo(newPos)
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Replay10,
                                        contentDescription = "Rewind 10s",
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                // Forward 10s
                                IconButton(
                                    onClick = {
                                        val newPos = (player.currentPosition + 10_000L).coerceAtMost(player.duration.coerceAtLeast(0L))
                                        currentPositionMs = newPos
                                        player.seekTo(newPos)
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Forward10,
                                        contentDescription = "Forward 10s",
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                // Skip Next Episode (for TV)
                                if (type == "tv" && episode > 0) {
                                    IconButton(
                                        onClick = {
                                            viewModel.extractStreams(type, id, title, year, season, episode + 1, isAnime)
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SkipNext,
                                            contentDescription = "Next Episode",
                                            tint = Color.White,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }

                                // Volume / Mute
                                IconButton(
                                    onClick = {
                                        isMuted = !isMuted
                                        player.volume = if (isMuted) 0f else 1f
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                        contentDescription = if (isMuted) "Unmute" else "Mute",
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }

                            // Right Controls Group
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Subtitles Quick Button
                                IconButton(
                                    onClick = {
                                        settingsTab = "subtitles"
                                        showSettingsDialog = true
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ClosedCaption,
                                        contentDescription = "Subtitles",
                                        tint = if (!isSubtitlesOff) Color.White else Color(0xFF888888),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                // Settings Gear Icon (Opens Quality, Audio, Subtitles, Playback speed)
                                IconButton(
                                    onClick = {
                                        settingsTab = "quality"
                                        showSettingsDialog = true
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Settings",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                // Fullscreen / Aspect Ratio Toggle
                                IconButton(
                                    onClick = {
                                        resizeMode = if (resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT) {
                                            aspectModeNotification = "Crop / Zoom (Fill Screen)"
                                            AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                        } else {
                                            aspectModeNotification = "Fit (Original Aspect Ratio)"
                                            AspectRatioFrameLayout.RESIZE_MODE_FIT
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT) {
                                            Icons.Default.Fullscreen
                                        } else {
                                            Icons.Default.FullscreenExit
                                        },
                                        contentDescription = "Toggle Fullscreen Mode",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // UNIFIED SETTINGS MODAL: Quality, Audio, Subtitles, Playback speed, Servers
        if (showSettingsDialog) {
            Dialog(onDismissRequest = { showSettingsDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF141414),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A2A2A)),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        // Modal Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Player Settings",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { showSettingsDialog = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Category Tabs: Quality | Audio | Subtitles | Speed | Servers
                        ScrollableTabRow(
                            selectedTabIndex = when (settingsTab) {
                                "quality" -> 0
                                "audio" -> 1
                                "subtitles" -> 2
                                "speed" -> 3
                                "servers" -> 4
                                else -> 0
                            },
                            containerColor = Color.Transparent,
                            contentColor = Color.White,
                            edgePadding = 0.dp,
                            divider = {}
                        ) {
                            listOf(
                                "quality" to "Quality",
                                "audio" to "Audio",
                                "subtitles" to "Subtitles",
                                "speed" to "Speed",
                                "servers" to "Servers"
                            ).forEach { (tabKey, tabLabel) ->
                                val isSelected = settingsTab == tabKey
                                Tab(
                                    selected = isSelected,
                                    onClick = { settingsTab = tabKey },
                                    text = {
                                        Text(
                                            text = tabLabel,
                                            color = if (isSelected) Color.White else Color(0xFF999999),
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 13.sp
                                        )
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Box(modifier = Modifier.heightIn(max = 240.dp)) {
                            when (settingsTab) {
                                // 1. Video Quality
                                "quality" -> {
                                    val qualityOptions = if (detectedQualities.isNotEmpty()) {
                                        detectedQualities
                                    } else {
                                        listOf(1080, 720, 480, 360)
                                    }

                                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        item {
                                            QualityRow(
                                                label = "Auto (Recommended)",
                                                isSelected = selectedQualityHeight == null,
                                                onClick = {
                                                    selectedQualityHeight = null
                                                    player.trackSelectionParameters = player.trackSelectionParameters
                                                        .buildUpon()
                                                        .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                                                        .setMaxVideoSize(Int.MAX_VALUE, Int.MAX_VALUE)
                                                        .setMinVideoSize(0, 0)
                                                        .build()
                                                    showSettingsDialog = false
                                                }
                                            )
                                        }

                                        items(qualityOptions) { height ->
                                            QualityRow(
                                                label = "${height}p",
                                                isSelected = selectedQualityHeight == height,
                                                onClick = {
                                                    selectedQualityHeight = height
                                                    player.trackSelectionParameters = player.trackSelectionParameters
                                                        .buildUpon()
                                                        .setMaxVideoSize(Int.MAX_VALUE, height)
                                                        .setMinVideoSize(0, height)
                                                        .build()
                                                    showSettingsDialog = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // 2. Audio Tracks
                                "audio" -> {
                                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (audioTracksList.isEmpty()) {
                                            item {
                                                TrackSelectionRow(
                                                    label = "Default Audio",
                                                    isSelected = true,
                                                    onClick = {}
                                                )
                                            }
                                        } else {
                                            items(audioTracksList) { track ->
                                                TrackSelectionRow(
                                                    label = track.name,
                                                    isSelected = track.id == selectedAudioTrackId || (selectedAudioTrackId == "default" && track.isSelected),
                                                    onClick = {
                                                        selectedAudioTrackId = track.id
                                                        if (track.trackGroup != null) {
                                                            player.trackSelectionParameters = player.trackSelectionParameters
                                                                .buildUpon()
                                                                .setOverrideForType(
                                                                    TrackSelectionOverride(track.trackGroup, listOf(track.trackIndex))
                                                                )
                                                                .build()
                                                        } else if (track.lang.isNotBlank()) {
                                                            player.trackSelectionParameters = player.trackSelectionParameters
                                                                .buildUpon()
                                                                .setPreferredAudioLanguage(track.lang)
                                                                .build()
                                                        }
                                                        showSettingsDialog = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                // 3. Subtitles
                                "subtitles" -> {
                                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (!isSubtitlesOff) {
                                            // Sync to Timeline Action Button
                                            item {
                                                Surface(
                                                    onClick = {
                                                        subtitleOffsetSeconds = 0f
                                                        val currentPos = player.currentPosition
                                                        currentPositionMs = currentPos

                                                        // Micro-seek forces immediate hardware pipeline re-sync to eliminate delay
                                                        player.seekTo(currentPos)

                                                        aspectModeNotification = "Subtitles synced to timeline"
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = BoomflixGreyButton,
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF444444)),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(bottom = 4.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.Center
                                                    ) {
                                                        Icon(
                                                             imageVector = Icons.Default.Sync,
                                                             contentDescription = "Sync to Timeline",
                                                             tint = Color.White,
                                                             modifier = Modifier.size(18.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = "Sync to Timeline",
                                                            color = Color.White,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }

                                            // Subtitle Text Size Selector
                                            item {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = Color(0xFF18181A),
                                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0x22FFFFFF)),
                                                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            text = "Subtitle Size",
                                                            color = Color.White,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                            listOf("Normal" to 16, "Large" to 19, "Extra" to 22).forEach { (label, size) ->
                                                                val isSel = subtitleFontSize == size
                                                                Surface(
                                                                    onClick = { subtitleFontSize = size },
                                                                    shape = RoundedCornerShape(4.dp),
                                                                    color = if (isSel) BoomflixChipSelected else Color(0xFF28282C)
                                                                ) {
                                                                    Text(
                                                                        text = label,
                                                                        color = Color.White,
                                                                        fontSize = 11.sp,
                                                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // Off Option
                                        item {
                                            TrackSelectionRow(
                                                label = "Off",
                                                isSelected = isSubtitlesOff,
                                                onClick = {
                                                    isSubtitlesOff = true
                                                    selectedSubtitleTrackId = null
                                                    customCues = emptyList()
                                                    player.trackSelectionParameters = player.trackSelectionParameters
                                                        .buildUpon()
                                                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                                                        .build()
                                                    showSettingsDialog = false
                                                }
                                            )
                                        }

                                        // Extracted Subtitles from Vyla
                                        if (subtitles.isNotEmpty()) {
                                            items(subtitles) { sub ->
                                                val subId = "ext_${sub.lang}_${sub.label}"
                                                val isSelected = !isSubtitlesOff && (selectedSubtitleTrackId == subId || (selectedSubtitleTrackId == null && sub.lang.startsWith("en", ignoreCase = true)))
                                                TrackSelectionRow(
                                                    label = sub.label.ifBlank { getLanguageDisplayName(sub.lang) },
                                                    isSelected = isSelected,
                                                    onClick = {
                                                        isSubtitlesOff = false
                                                        selectedSubtitleTrackId = subId
                                                        player.trackSelectionParameters = player.trackSelectionParameters
                                                            .buildUpon()
                                                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                                            .setPreferredTextLanguage(sub.lang)
                                                            .build()
                                                        showSettingsDialog = false
                                                    }
                                                )
                                            }
                                        }

                                        // In-stream text tracks from video container
                                        if (subtitleTracksList.isNotEmpty()) {
                                            items(subtitleTracksList) { inSub ->
                                                val isSelected = !isSubtitlesOff && selectedSubtitleTrackId == inSub.id
                                                TrackSelectionRow(
                                                    label = inSub.name,
                                                    isSelected = isSelected,
                                                    onClick = {
                                                        isSubtitlesOff = false
                                                        selectedSubtitleTrackId = inSub.id
                                                        customCues = emptyList()
                                                        if (inSub.trackGroup != null) {
                                                            player.trackSelectionParameters = player.trackSelectionParameters
                                                                .buildUpon()
                                                                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                                                .setOverrideForType(
                                                                    TrackSelectionOverride(inSub.trackGroup, listOf(inSub.trackIndex))
                                                                )
                                                                .build()
                                                        }
                                                        showSettingsDialog = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                // 4. Playback Speed
                                "speed" -> {
                                    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(speeds) { speed ->
                                            val label = if (speed == 1.0f) "1.0x (Normal)" else "${speed}x"
                                            SpeedRow(
                                                label = label,
                                                isSelected = playbackSpeed == speed,
                                                onClick = {
                                                    playbackSpeed = speed
                                                    player.setPlaybackSpeed(speed)
                                                    showSettingsDialog = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // 5. Streaming Servers
                                "servers" -> {
                                    val serverItems = if (servers.isNotEmpty()) {
                                        servers
                                    } else {
                                        (1..20).map { i -> ServerItem("server_$i", "Server $i") }
                                    }

                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(3),
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(serverItems) { server ->
                                            val isActive = server.id == activeServerId
                                            ServerCard(
                                                server = server,
                                                isActive = isActive,
                                                onClick = {
                                                    viewModel.selectServer(server)
                                                    showSettingsDialog = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TagPill(text: String) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF1E1E22),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333338))
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun QualityRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) Color(0xFF2A1112) else Color(0xFF1E1E1E),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) BoomflixRed else Color(0xFF333333)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = if (isSelected) Color.White else Color(0xFFCCCCCC),
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = BoomflixRed,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun SpeedRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) Color(0xFF2A1112) else Color(0xFF1E1E1E),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) BoomflixRed else Color(0xFF333333)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = if (isSelected) Color.White else Color(0xFFCCCCCC),
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = BoomflixRed,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun TrackSelectionRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = if (isSelected) Color(0xFF2A1112) else Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = if (isSelected) Color.White else Color(0xFFB3B3B3),
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = BoomflixRed,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun ServerCard(
    server: ServerItem,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isActive) Color(0xFF2A1112) else Color(0xFF1E1E1E),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isActive) 1.5.dp else 1.dp,
            color = if (isActive) BoomflixRed else Color(0xFF333333)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = server.name,
                color = if (isActive) Color.White else Color(0xFFCCCCCC),
                fontSize = 13.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (isActive) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Active Server",
                    tint = BoomflixRed,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private fun triggerCastIntent(
    context: android.content.Context,
    streamUrl: String?,
    streamType: String?,
    title: String,
    positionMs: Long
) {
    if (streamUrl.isNullOrBlank()) {
        Toast.makeText(context, "No stream URL ready to cast", Toast.LENGTH_SHORT).show()
        return
    }

    try {
        val uri = Uri.parse(streamUrl)
        val mimeType = if (streamType?.equals("hls", ignoreCase = true) == true || streamUrl.contains(".m3u8", ignoreCase = true)) {
            "application/x-mpegurl"
        } else {
            "video/mp4"
        }

        val castIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            putExtra(Intent.EXTRA_TITLE, title)
            putExtra("title", title)
            putExtra("position", positionMs)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val chooser = Intent.createChooser(castIntent, "Cast to TV")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (_: Exception) {
        Toast.makeText(context, "No casting or video player apps found", Toast.LENGTH_SHORT).show()
    }
}

private fun formatDuration(millis: Long): String {
    if (millis <= 0) return "0:00"
    val totalSeconds = millis / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600

    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}

private fun getLanguageDisplayName(code: String): String {
    val clean = code.trim().lowercase()
    val map = mapOf(
        "en" to "English", "eng" to "English",
        "hi" to "Hindi", "hin" to "Hindi",
        "ja" to "Japanese", "jpn" to "Japanese",
        "es" to "Spanish", "spa" to "Spanish",
        "fr" to "French", "fra" to "French", "fre" to "French",
        "de" to "German", "deu" to "German", "ger" to "German",
        "it" to "Italian", "ita" to "Italian",
        "pt" to "Portuguese", "por" to "Portuguese",
        "ru" to "Russian", "rus" to "Russian",
        "ko" to "Korean", "kor" to "Korean",
        "zh" to "Chinese", "zho" to "Chinese", "chi" to "Chinese",
        "ar" to "Arabic", "ara" to "Arabic",
        "ta" to "Tamil", "tam" to "Tamil",
        "te" to "Telugu", "tel" to "Telugu",
        "ml" to "Malayalam", "mal" to "Malayalam",
        "tr" to "Turkish", "tur" to "Turkish",
        "vi" to "Vietnamese", "vie" to "Vietnamese",
        "id" to "Indonesian", "ind" to "Indonesian"
    )
    return map[clean] ?: try {
        val loc = java.util.Locale.forLanguageTag(clean)
        val name = loc.getDisplayLanguage(java.util.Locale.ENGLISH)
        if (name.isNotBlank()) name else code.replaceFirstChar { it.uppercase() }
    } catch (_: Exception) {
        code.replaceFirstChar { it.uppercase() }
    }
}
