package com.example.boomflix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.boomflix.data.local.ContinueWatchingEntity
import com.example.boomflix.theme.BoomflixRed

@Composable
fun ContinueWatchingRow(
    items: List<ContinueWatchingEntity>,
    onItemClick: (ContinueWatchingEntity) -> Unit,
    onRemoveItem: (ContinueWatchingEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        // Section Header with Netflix Red Accent Line
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(BoomflixRed)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Continue Watching",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 18.sp
            )
        }

        // Horizontal Carousel
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(items, key = { it.mediaId }) { item ->
                ContinueWatchingCard(
                    item = item,
                    onClick = { onItemClick(item) },
                    onRemove = { onRemoveItem(item) }
                )
            }
        }
    }
}

@Composable
fun ContinueWatchingCard(
    item: ContinueWatchingEntity,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    cardWidth: Int = 220
) {
    val remainingSec = ((item.duration - item.currentTime) / 1000L).coerceAtLeast(0L)
    val remainingText = if (remainingSec > 60) {
        "${remainingSec / 60}m left"
    } else if (remainingSec > 0) {
        "${remainingSec}s left"
    } else {
        "${(item.progress * 100).toInt()}% watched"
    }

    val subtitle = if (item.season != null && item.episode != null && item.season > 0 && item.episode > 0) {
        "S${item.season}:E${item.episode} · $remainingText"
    } else {
        remainingText
    }

    Column(
        modifier = modifier
            .width(cardWidth.dp)
            .clickable(onClick = onClick)
    ) {
        // Thumbnail with 16:9 ratio
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Backdrop / Poster Image
                AsyncImage(
                    model = item.displayBackdropUrl ?: item.displayPosterUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Dark gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x33000000),
                                    Color(0x11000000),
                                    Color(0x88000000)
                                )
                            )
                        )
                )

                // Center Play Icon Button
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0x99000000))
                        .border(1.dp, Color(0x66FFFFFF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Top Right Remove Button
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xAA000000))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove from Continue Watching",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Bottom Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.5.dp)
                        .align(Alignment.BottomCenter)
                        .background(Color(0x55FFFFFF))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = item.progress.coerceIn(0.01f, 1f))
                            .background(BoomflixRed)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Title and remaining time metadata
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(2.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFB3B3B3),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Resume indicator pill
            Text(
                text = "Resume",
                color = BoomflixRed,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
    }
}
