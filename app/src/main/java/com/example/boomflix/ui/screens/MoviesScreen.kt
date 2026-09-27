package com.example.boomflix.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.boomflix.ui.components.MediaCard
import com.example.boomflix.ui.viewmodels.MoviesViewModel
import com.example.boomflix.theme.BoomflixRed
import com.example.boomflix.theme.BoomflixBackground
import com.example.boomflix.theme.BoomflixChipSelected
import com.example.boomflix.theme.BoomflixChipUnselected

@Composable
fun MoviesScreen(
    onNavigateToDetails: (String, Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MoviesViewModel = viewModel()
) {
    val movies by viewModel.movies.collectAsState()
    val genres by viewModel.genres.collectAsState()
    val selectedGenreId by viewModel.selectedGenreId.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BoomflixBackground)
    ) {
        // Genre filter chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedGenreId == null,
                    onClick = { viewModel.selectGenre(null) },
                    label = { Text("All") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BoomflixChipSelected,
                        selectedLabelColor = Color.White,
                        containerColor = BoomflixChipUnselected,
                        labelColor = Color(0xFFB3B3B3)
                    )
                )
            }
            items(genres) { genre ->
                FilterChip(
                    selected = selectedGenreId == genre.id,
                    onClick = { viewModel.selectGenre(genre.id) },
                    label = { Text(genre.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BoomflixChipSelected,
                        selectedLabelColor = Color.White,
                        containerColor = BoomflixChipUnselected,
                        labelColor = Color(0xFFB3B3B3)
                    )
                )
            }
        }

        if (isLoading && movies.isEmpty()) {
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
                items(movies, key = { it.id }) { item ->
                    MediaCard(
                        item = item,
                        onClick = { onNavigateToDetails("movie", item.id) },
                        width = 130
                    )
                }
            }
        }
    }
}
