package com.example.boomflix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.boomflix.data.models.MediaItem
import com.example.boomflix.theme.BoomflixRed

@Composable
fun Top10Row(
    title: String,
    items: List<MediaItem>,
    onItemClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    onTitleClick: (() -> Unit)? = null
) {
    val top10Items = items.take(10)
    if (top10Items.isEmpty()) return

    Column(modifier = modifier) {
        // Netflix-style row header matching website Top10Row.tsx
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .then(
                    if (onTitleClick != null) Modifier.clickable { onTitleClick() }
                    else Modifier
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Aesthetic vertical red accent pill
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(BoomflixRed)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(verticalArrangement = Arrangement.Center) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 18.sp
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "View more",
                        tint = Color(0xFF999999),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Today's most watched titles",
                    color = Color(0xFF808080),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(top10Items, key = { _, item -> item.id }) { index, item ->
                val rank = index + 1
                val isTen = rank == 10

                Box(
                    modifier = Modifier
                        .height(200.dp)
                        .width(if (isTen) 175.dp else 155.dp)
                        .clickable { onItemClick(item) }
                ) {
                    // Netflix Giant Rank Number
                    // Layer 1: Crisp white outer outline stroke
                    Text(
                        text = "$rank",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Black,
                            fontSize = if (isTen) 100.sp else 115.sp,
                            color = Color.White,
                            drawStyle = Stroke(width = 6f, join = StrokeJoin.Round)
                        ),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .offset(x = 0.dp, y = 14.dp)
                    )

                    // Layer 2: Inner dark fill
                    Text(
                        text = "$rank",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Black,
                            fontSize = if (isTen) 100.sp else 115.sp,
                            color = Color(0xFF141418)
                        ),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .offset(x = 0.dp, y = 14.dp)
                    )

                    // Elevated Movie Poster Card (Shifted to the Right)
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(115.dp)
                            .aspectRatio(2f / 3f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E1E1E))
                            .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                    ) {
                        AsyncImage(
                            model = item.posterUrl,
                            contentDescription = item.displayTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}
