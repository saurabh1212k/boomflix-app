package com.example.boomflix

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.boomflix.theme.BoomflixBackground
import com.example.boomflix.ui.components.BoomflixLogo
import com.example.boomflix.ui.components.BottomNavBar
import com.example.boomflix.ui.components.NotificationModal
import com.example.boomflix.ui.screens.*
import com.example.boomflix.updater.AppUpdateInfo
import com.example.boomflix.updater.UpdateCheckResult
import com.example.boomflix.updater.UpdateDialog
import com.example.boomflix.updater.UpdateManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class DeepLinkMedia(
    val type: String,
    val id: Int,
    val title: String,
    val seekPositionMs: Long = 0L,
    val preferredServer: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigation(
    initialDeepLink: DeepLinkMedia? = null
) {
    val backStack = rememberNavBackStack(Home)
    var currentRoute by remember { mutableStateOf("home") }
    var showNotificationModal by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val updateManager = remember { UpdateManager(context) }
    var activeUpdateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var updatePillNotification by remember { mutableStateOf<String?>(null) }
    var hasUpdateBadge by remember { mutableStateOf(false) }

    // Automatic silent check on app startup
    LaunchedEffect(Unit) {
        val result = updateManager.checkForUpdates()
        if (result is UpdateCheckResult.Available) {
            hasUpdateBadge = true
            activeUpdateInfo = result.updateInfo
        }
    }

    LaunchedEffect(initialDeepLink) {
        if (initialDeepLink != null && initialDeepLink.id > 0) {
            backStack.add(
                Player(
                    type = initialDeepLink.type,
                    id = initialDeepLink.id,
                    title = initialDeepLink.title,
                    startPositionMs = initialDeepLink.seekPositionMs,
                    preferredServer = initialDeepLink.preferredServer
                )
            )
        }
    }


    val popBack: () -> Unit = {
        if (backStack.lastOrNull() != Home) {
            backStack.removeLastOrNull()
        }
    }

    // Determine if we should show bottom nav (hide on player/details)
    val currentEntry = backStack.lastOrNull()
    val showBottomBar = currentEntry !is Player && currentEntry !is Details
    val showTopBar = currentEntry !is Player

    Scaffold(
        containerColor = BoomflixBackground,
        topBar = {
            if (showTopBar) {
                CenterAlignedTopAppBar(
                    title = {
                        BoomflixLogo(
                            iconSize = 26.dp,
                            fontSize = 20.sp
                        )
                    },
                    actions = {
                        // GitHub App Updater Check Button
                        IconButton(onClick = {
                            coroutineScope.launch {
                                updatePillNotification = "Checking for updates..."
                                when (val res = updateManager.checkForUpdates()) {
                                    is UpdateCheckResult.Available -> {
                                        hasUpdateBadge = true
                                        activeUpdateInfo = res.updateInfo
                                        updatePillNotification = null
                                    }
                                    is UpdateCheckResult.UpToDate -> {
                                        hasUpdateBadge = false
                                        updatePillNotification = "Boomflix is up to date (v${BuildConfig.VERSION_NAME})"
                                        delay(3000)
                                        updatePillNotification = null
                                    }
                                    is UpdateCheckResult.Error -> {
                                        updatePillNotification = res.message
                                        delay(3500)
                                        updatePillNotification = null
                                    }
                                }
                            }
                        }) {
                            Box {
                                Icon(
                                    imageVector = Icons.Default.SystemUpdate,
                                    contentDescription = "Check for Updates",
                                    tint = Color.White
                                )
                                if (hasUpdateBadge) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFE50914))
                                            .align(Alignment.TopEnd)
                                    )
                                }
                            }
                        }

                        IconButton(onClick = { showNotificationModal = true }) {
                            Box {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notifications",
                                    tint = Color.White
                                )
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE50914))
                                        .align(Alignment.TopEnd)
                                )
                            }
                        }
                        IconButton(onClick = {
                            if (currentEntry !is Search) {
                                backStack.add(Search)
                                currentRoute = "search"
                            }
                        }) {
                            Icon(Icons.Default.Search, "Search", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = BoomflixBackground
                    )
                )
            }
        },

        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        currentRoute = route
                        // Clear back stack to Home
                        while (backStack.lastOrNull() != Home && backStack.lastOrNull() != null) {
                            backStack.removeLastOrNull()
                        }
                        when (route) {
                            "home" -> { /* Already at Home */ }
                            "movies" -> backStack.add(Movies)
                            "tv" -> backStack.add(TvShows)
                            "anime" -> backStack.add(Anime)
                            "mylist" -> backStack.add(MyList)
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        if (showNotificationModal) {
            NotificationModal(onDismiss = { showNotificationModal = false })
        }

        NavDisplay(

            backStack = backStack,
            onBack = popBack,
            modifier = Modifier.padding(innerPadding),
            entryProvider = entryProvider {
                entry<Home> {
                    HomeScreen(
                        onNavigateToDetails = { type, id -> backStack.add(Details(type, id)) },
                        onNavigateToSearch = {
                            backStack.add(Search)
                            currentRoute = "search"
                        },
                        onNavigateToPlayer = { type, id, title, year, season, episode, isAnime, startPos, server, poster, backdrop ->
                            backStack.add(
                                Player(
                                    type = type,
                                    id = id,
                                    title = title,
                                    year = year,
                                    season = season,
                                    episode = episode,
                                    isAnime = isAnime,
                                    startPositionMs = startPos,
                                    preferredServer = server,
                                    posterPath = poster,
                                    backdropPath = backdrop
                                )
                            )
                        }
                    )
                }
                entry<Movies> {
                    MoviesScreen(
                        onNavigateToDetails = { type, id -> backStack.add(Details(type, id)) }
                    )
                }
                entry<TvShows> {
                    TvShowsScreen(
                        onNavigateToDetails = { type, id -> backStack.add(Details(type, id)) }
                    )
                }
                entry<Anime> {
                    AnimeScreen(
                        onNavigateToDetails = { type, id -> backStack.add(Details(type, id)) }
                    )
                }
                entry<MyList> {
                    MyListScreen(
                        onNavigateToDetails = { type, id -> backStack.add(Details(type, id)) }
                    )
                }
                entry<Search> {
                    SearchScreen(
                        onNavigateToDetails = { type, id -> backStack.add(Details(type, id)) },
                        onBack = popBack
                    )
                }
                entry<Details> { key ->
                    DetailsScreen(
                        type = key.type,
                        id = key.id,
                        onNavigateToPlayer = { type, id, title, year, season, episode, isAnime, startPos, server, poster, backdrop ->
                            backStack.add(
                                Player(
                                    type = type,
                                    id = id,
                                    title = title,
                                    year = year,
                                    season = season,
                                    episode = episode,
                                    isAnime = isAnime,
                                    startPositionMs = startPos,
                                    preferredServer = server,
                                    posterPath = poster,
                                    backdropPath = backdrop
                                )
                            )
                        },
                        onBack = popBack
                    )
                }
                entry<Player> { key ->
                    PlayerScreen(
                        type = key.type,
                        id = key.id,
                        title = key.title,
                        year = key.year,
                        season = key.season,
                        episode = key.episode,
                        isAnime = key.isAnime,
                        startPositionMs = key.startPositionMs,
                        preferredServer = key.preferredServer,
                        posterPath = key.posterPath,
                        backdropPath = key.backdropPath,
                        onBack = popBack
                    )
                }
            }
        )

        // GitHub App Update Modal
        if (activeUpdateInfo != null) {
            UpdateDialog(
                updateInfo = activeUpdateInfo!!,
                onDismiss = { activeUpdateInfo = null }
            )
        }

        // Floating Status Pill Notification
        if (updatePillNotification != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 70.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xEE1A1A1A),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x44FFFFFF))
                ) {
                    Text(
                        text = updatePillNotification ?: "",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}
