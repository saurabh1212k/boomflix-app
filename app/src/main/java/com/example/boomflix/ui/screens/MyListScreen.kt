package com.example.boomflix.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.boomflix.data.auth.AuthManager
import com.example.boomflix.data.local.BoomflixDatabase
import com.example.boomflix.data.local.MyListEntity
import com.example.boomflix.data.turso.TursoSyncManager
import com.example.boomflix.theme.BoomflixBackground
import com.example.boomflix.theme.BoomflixCardSurface
import com.example.boomflix.theme.BoomflixRed
import kotlinx.coroutines.launch

@Composable
fun MyListScreen(
    onNavigateToDetails: (String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val database = remember { BoomflixDatabase.getInstance(context) }
    val authManager = remember { AuthManager.getInstance(context) }
    val syncManager = remember { TursoSyncManager.getInstance(context) }
    val scope = rememberCoroutineScope()

    val myListItems by database.myListDao().observeAll().collectAsState(initial = emptyList())
    val currentUser by authManager.currentUser.collectAsState()

    // Automatically sync latest list from Turso cloud when opening My List
    LaunchedEffect(currentUser?.uid) {
        val uid = currentUser?.uid
        if (!uid.isNullOrBlank()) {
            syncManager.syncFromCloud(uid)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BoomflixBackground)
    ) {
        // Top header bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "My List",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${myListItems.size} ${if (myListItems.size == 1) "item" else "items"}",
                    color = Color(0xFF888888),
                    fontSize = 12.sp
                )
            }
        }

        if (myListItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkBorder,
                        contentDescription = null,
                        tint = Color(0xFF555555),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Your List is Empty",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Explore movies and TV shows and tap '+ My List' to add them here. Your saved titles sync to your Turso cloud database.",
                        color = Color(0xFF888888),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 105.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(myListItems, key = { it.mediaId }) { item ->
                    MyListItemCard(
                        item = item,
                        onClick = {
                            val idInt = item.mediaId.toIntOrNull() ?: 0
                            if (idInt > 0) {
                                onNavigateToDetails(item.type, idInt)
                            }
                        },
                        onRemove = {
                            scope.launch {
                                database.myListDao().delete(item.mediaId)
                                val uid = currentUser?.uid
                                if (!uid.isNullOrBlank()) {
                                    syncManager.pushWatchlistToCloud(uid)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MyListItemCard(
    item: MyListEntity,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(115.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(6.dp))
                .background(BoomflixCardSurface)
        ) {
            AsyncImage(
                model = item.displayPosterUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Remove button overlay at top right
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xBB000000))
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove from List",
                    tint = Color(0xFFFF8080),
                    modifier = Modifier.size(14.dp)
                )
            }

            // Year and Rating tag overlay at bottom
            if (!item.year.isNullOrBlank() || !item.rating.isNullOrBlank()) {
                Surface(
                    color = Color(0xCC000000),
                    shape = RoundedCornerShape(topStart = 4.dp),
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!item.rating.isNullOrBlank()) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = item.rating,
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (!item.year.isNullOrBlank()) {
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                        }
                        if (!item.year.isNullOrBlank()) {
                            Text(
                                text = item.year,
                                color = Color(0xFFCCCCCC),
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = item.title,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
