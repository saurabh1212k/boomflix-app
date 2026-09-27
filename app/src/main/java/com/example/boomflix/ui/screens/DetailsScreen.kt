package com.example.boomflix.ui.screens

import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.example.boomflix.data.local.BoomflixDatabase
import com.example.boomflix.data.local.ContinueWatchingEntity
import com.example.boomflix.data.models.CastMember
import com.example.boomflix.data.models.Episode
import com.example.boomflix.theme.BoomflixBackground
import com.example.boomflix.theme.BoomflixChipSelected
import com.example.boomflix.theme.BoomflixChipUnselected
import com.example.boomflix.theme.BoomflixGreyButton
import com.example.boomflix.theme.BoomflixWhiteButton
import com.example.boomflix.ui.components.MediaRow
import com.example.boomflix.ui.viewmodels.DetailsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    type: String,
    id: Int,
    onNavigateToPlayer: (
        type: String,
        id: Int,
        title: String,
        year: String,
        season: Int,
        episode: Int,
        isAnime: Boolean,
        startPositionMs: Long,
        preferredServer: String?,
        posterPath: String?,
        backdropPath: String?
    ) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DetailsViewModel = viewModel()
) {
    val details by viewModel.details.collectAsState()
    val cast by viewModel.cast.collectAsState()
    val episodes by viewModel.episodes.collectAsState()
    val selectedSeason by viewModel.selectedSeason.collectAsState()
    val similar by viewModel.similar.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val context = LocalContext.current
    val database = remember { BoomflixDatabase.getInstance(context) }
    var savedProgress by remember { mutableStateOf<ContinueWatchingEntity?>(null) }
    var showTrailerDialog by remember { mutableStateOf(false) }

    LaunchedEffect(type, id) {
        viewModel.loadDetails(type, id)
        savedProgress = database.continueWatchingDao().getById(id.toString())
    }

    if (isLoading) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(BoomflixBackground),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    val media = details ?: return

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BoomflixBackground)
    ) {
        // Backdrop with gradient
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                AsyncImage(
                    model = media.backdropUrl ?: media.posterUrl,
                    contentDescription = media.displayTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xCC141414), BoomflixBackground),
                                startY = 120f
                            )
                        )
                )
                // Back button
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopStart)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        "Back",
                        tint = Color.White
                    )
                }
            }
        }

        // Title and metadata
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = media.displayTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (media.year.isNotBlank()) {
                        Text(media.year, color = Color.Gray, fontSize = 14.sp)
                    }
                    if (media.displayRuntime.isNotBlank()) {
                        Text(media.displayRuntime, color = Color.Gray, fontSize = 14.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, "Rating", tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(media.rating, color = Color.White, fontSize = 14.sp)
                    }
                }
            }
        }

        // Action Buttons: Play (White) and Trailer (Grey)
        item {
            val hasProgress = savedProgress != null && (savedProgress?.currentTime ?: 0L) > 5000L
            val playButtonLabel = if (hasProgress) {
                val sec = (savedProgress?.currentTime ?: 0L) / 1000L
                val m = sec / 60
                val s = sec % 60
                String.format("Resume (%02d:%02d)", m, s)
            } else {
                "Play"
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // High contrast white play button
                Button(
                    onClick = {
                        val resumePos = if (hasProgress) (savedProgress?.currentTime ?: 0L) else 0L
                        val resumeServer = savedProgress?.serverId
                        val targetSeason = if (type == "tv") (savedProgress?.season ?: selectedSeason) else -1
                        val targetEpisode = if (type == "tv") (savedProgress?.episode ?: episodes.firstOrNull()?.episodeNumber ?: 1) else -1

                        onNavigateToPlayer(
                            type,
                            id,
                            media.displayTitle,
                            media.year,
                            targetSeason,
                            targetEpisode,
                            false,
                            resumePos,
                            resumeServer,
                            media.posterPath,
                            media.backdropPath
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BoomflixWhiteButton,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, "Play", tint = Color.Black, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(playButtonLabel, fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 15.sp)
                }

                // Greyish Trailer Button
                if (media.trailerKey != null) {
                    Button(
                        onClick = { showTrailerDialog = true },
                        modifier = Modifier.height(46.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BoomflixGreyButton,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(Icons.Default.Movie, "Trailer", tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Trailer", fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 14.sp)
                    }
                }
            }
        }

        // Overview
        item {
            if (!media.overview.isNullOrBlank()) {
                Text(
                    text = media.overview,
                    color = Color(0xFFB3B3B3),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        // Genres
        item {
            if (media.genres.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    items(media.genres) { genre ->
                        SuggestionChip(
                            onClick = {},
                            label = { Text(genre.name, fontSize = 12.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = Color(0xFF242426),
                                labelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        // Cast
        if (cast.isNotEmpty()) {
            item {
                Text(
                    "Cast",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(cast) { member ->
                        CastCard(member)
                    }
                }
            }
        }

        // Season selector + Episodes (TV only)
        if (type == "tv" && media.numberOfSeasons != null && media.numberOfSeasons > 0) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Episodes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Season tabs with greyish buttons
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(media.numberOfSeasons) { index ->
                        val seasonNum = index + 1
                        FilterChip(
                            selected = selectedSeason == seasonNum,
                            onClick = { viewModel.selectSeason(id, seasonNum) },
                            label = { Text("S$seasonNum") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BoomflixChipSelected,
                                selectedLabelColor = Color.White,
                                containerColor = BoomflixChipUnselected,
                                labelColor = Color(0xFFCCCCCC)
                            )
                        )
                    }
                }
            }

            // Episode cards
            items(episodes.size) { index ->
                val ep = episodes[index]
                EpisodeCard(
                    episode = ep,
                    onClick = {
                        onNavigateToPlayer(
                            type,
                            id,
                            media.displayTitle,
                            media.year,
                            selectedSeason,
                            ep.episodeNumber,
                            false,
                            0L,
                            null,
                            media.posterPath,
                            ep.stillUrl ?: media.backdropPath
                        )
                    }
                )
            }
        }

        // Similar content
        item {
            Spacer(modifier = Modifier.height(16.dp))
            MediaRow(
                title = "More Like This",
                items = similar,
                onItemClick = { /* Already on details */ }
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Official Trailer Dialog
    if (showTrailerDialog && media.trailerKey != null) {
        Dialog(onDismissRequest = { showTrailerDialog = false }) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = BoomflixBackground,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Official Trailer",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                try {
                                    val ytIntent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://www.youtube.com/watch?v=${media.trailerKey}")
                                    )
                                    context.startActivity(ytIntent)
                                } catch (_: Exception) {}
                            }) {
                                Icon(
                                    Icons.Default.OpenInNew,
                                    "Open in YouTube",
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(onClick = { showTrailerDialog = false }) {
                                Icon(
                                    Icons.Default.Close,
                                    "Close",
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.mediaPlaybackRequiresUserGesture = false
                                webViewClient = WebViewClient()
                                loadUrl("https://www.youtube-nocookie.com/embed/${media.trailerKey}?autoplay=1&playsinline=1")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CastCard(member: CastMember) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        AsyncImage(
            model = member.profileUrl,
            contentDescription = member.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(Color(0xFF2A2A2A))
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = member.name,
            color = Color.White,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (member.character != null) {
            Text(
                text = member.character,
                color = Color.Gray,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EpisodeCard(episode: Episode, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Episode thumbnail
        Card(
            modifier = Modifier
                .width(130.dp)
                .aspectRatio(16f / 9f),
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E20))
        ) {
            Box(contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = episode.stillUrl,
                    contentDescription = episode.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Icon(
                    Icons.Default.PlayArrow,
                    "Play",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${episode.episodeNumber}. ${episode.name ?: "Episode ${episode.episodeNumber}"}",
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (episode.runtime != null) {
                Text(
                    text = "${episode.runtime}m",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
            if (!episode.overview.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = episode.overview,
                    color = Color(0xFF808080),
                    fontSize = 12.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
