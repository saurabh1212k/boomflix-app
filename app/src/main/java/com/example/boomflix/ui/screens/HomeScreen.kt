package com.example.boomflix.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.boomflix.ui.components.ContinueWatchingRow
import com.example.boomflix.ui.components.HeroBanner
import com.example.boomflix.ui.components.MediaRow
import com.example.boomflix.ui.components.Top10Row
import com.example.boomflix.ui.viewmodels.HomeViewModel
import com.example.boomflix.theme.BoomflixRed

@Composable
fun HomeScreen(
    onNavigateToDetails: (String, Int) -> Unit,
    onNavigateToSearch: () -> Unit,
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
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel()
) {
    val trendingThisWeek by viewModel.trendingThisWeek.collectAsState()
    val top10Movies by viewModel.top10Movies.collectAsState()
    val top10Tv by viewModel.top10Tv.collectAsState()
    val popularTv by viewModel.popularTv.collectAsState()
    val actionMovies by viewModel.actionMovies.collectAsState()
    val comedyMovies by viewModel.comedyMovies.collectAsState()
    val horrorMovies by viewModel.horrorMovies.collectAsState()
    val romanceMovies by viewModel.romanceMovies.collectAsState()
    val topRatedMovies by viewModel.topRatedMovies.collectAsState()
    val heroItem by viewModel.heroItem.collectAsState()
    val continueWatching by viewModel.continueWatching.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    if (isLoading && trendingThisWeek.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF0A0A0A)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(
                    color = BoomflixRed,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Loading BOOMFLIX...",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        return
    }

    if (error != null && trendingThisWeek.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF0A0A0A)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = error ?: "Connection error",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.loadContent() },
                    colors = ButtonDefaults.buttonColors(containerColor = BoomflixRed),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Retry", color = Color.White)
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
    ) {
        // 1. Hero Banner
        item {
            HeroBanner(
                item = heroItem,
                onPlayClick = {
                    heroItem?.let { item ->
                        val type = if (item.title != null) "movie" else "tv"
                        onNavigateToPlayer(
                            type,
                            item.id,
                            item.displayTitle,
                            item.year,
                            -1,
                            -1,
                            false,
                            0L,
                            null,
                            item.posterPath,
                            item.backdropPath
                        )
                    }
                },
                onInfoClick = {
                    heroItem?.let { item ->
                        val type = if (item.title != null) "movie" else "tv"
                        onNavigateToDetails(type, item.id)
                    }
                }
            )
        }

        // 2. Continue Watching (Netflix layout: immediately follows Hero Banner)
        if (continueWatching.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
                ContinueWatchingRow(
                    items = continueWatching,
                    onItemClick = { cwItem ->
                        val mediaIdInt = cwItem.mediaId.toIntOrNull() ?: 0
                        onNavigateToPlayer(
                            cwItem.type,
                            mediaIdInt,
                            cwItem.title,
                            "",
                            cwItem.season ?: -1,
                            cwItem.episode ?: -1,
                            false,
                            cwItem.currentTime,
                            cwItem.serverId,
                            cwItem.posterPath,
                            cwItem.backdropPath
                        )
                    },
                    onRemoveItem = { cwItem ->
                        viewModel.removeFromContinueWatching(cwItem.mediaId)
                    }
                )
                Spacer(modifier = Modifier.height(14.dp))
            }
        }

        // 3. Trending this week
        if (trendingThisWeek.isNotEmpty()) {
            item {
                MediaRow(
                    title = "Trending this week",
                    subtitle = "Top movies and TV shows everyone is watching right now",
                    items = trendingThisWeek,
                    onItemClick = { item ->
                        val type = if (item.title != null) "movie" else "tv"
                        onNavigateToDetails(type, item.id)
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // 4. Top 10 Movies Today
        if (top10Movies.isNotEmpty()) {
            item {
                Top10Row(
                    title = "Top 10 Movies Today",
                    items = top10Movies,
                    onItemClick = { item -> onNavigateToDetails("movie", item.id) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // 5. Top 10 TV Shows Today
        if (top10Tv.isNotEmpty()) {
            item {
                Top10Row(
                    title = "Top 10 TV Shows Today",
                    items = top10Tv,
                    onItemClick = { item -> onNavigateToDetails("tv", item.id) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // 6. Current & upcoming TV shows
        if (popularTv.isNotEmpty()) {
            item {
                MediaRow(
                    title = "Current & upcoming TV shows",
                    subtitle = "New episodes and premieres on the way",
                    items = popularTv,
                    onItemClick = { item -> onNavigateToDetails("tv", item.id) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // 7. Action & Adventure
        if (actionMovies.isNotEmpty()) {
            item {
                MediaRow(
                    title = "Action & Adventure",
                    items = actionMovies,
                    onItemClick = { item -> onNavigateToDetails("movie", item.id) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // 8. Comedies
        if (comedyMovies.isNotEmpty()) {
            item {
                MediaRow(
                    title = "Comedies",
                    items = comedyMovies,
                    onItemClick = { item -> onNavigateToDetails("movie", item.id) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // 9. Horror Movies
        if (horrorMovies.isNotEmpty()) {
            item {
                MediaRow(
                    title = "Horror Movies",
                    items = horrorMovies,
                    onItemClick = { item -> onNavigateToDetails("movie", item.id) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // 10. Romance
        if (romanceMovies.isNotEmpty()) {
            item {
                MediaRow(
                    title = "Romance",
                    items = romanceMovies,
                    onItemClick = { item -> onNavigateToDetails("movie", item.id) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // 11. Top Rated Movies
        if (topRatedMovies.isNotEmpty()) {
            item {
                MediaRow(
                    title = "Top Rated Movies",
                    subtitle = "Critically acclaimed movies of all time",
                    items = topRatedMovies,
                    onItemClick = { item -> onNavigateToDetails("movie", item.id) }
                )
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}
