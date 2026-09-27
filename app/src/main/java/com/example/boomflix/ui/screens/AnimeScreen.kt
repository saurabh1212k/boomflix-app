package com.example.boomflix.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.boomflix.ui.components.MediaCard
import com.example.boomflix.ui.viewmodels.AnimeViewModel
import com.example.boomflix.theme.BoomflixRed
import com.example.boomflix.theme.BoomflixBackground

@Composable
fun AnimeScreen(
    onNavigateToDetails: (String, Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AnimeViewModel = viewModel()
) {
    val anime by viewModel.anime.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BoomflixBackground)
    ) {
        if (isLoading && anime.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BoomflixRed)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 110.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(anime, key = { it.id }) { item ->
                    MediaCard(
                        item = item,
                        onClick = { onNavigateToDetails("tv", item.id) },
                        width = 130
                    )
                }
            }
        }
    }
}
