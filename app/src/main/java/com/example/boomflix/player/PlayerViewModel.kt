package com.example.boomflix.player

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.boomflix.data.models.MediaDetails
import com.example.boomflix.data.models.ServerItem
import com.example.boomflix.data.models.StreamInfo
import com.example.boomflix.data.models.StreamResult
import com.example.boomflix.data.models.SubtitleInfo
import com.example.boomflix.data.repository.MediaRepository
import com.example.boomflix.extractor.ExtractorBridge
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "PlayerViewModel"

        private val DEFAULT_MOVIE_SERVERS = listOf(
            ServerItem("rivestream", "Server 1"),
            ServerItem("movy", "Server 2"),
            ServerItem("frame", "Server 3"),
            ServerItem("vidzee", "Server 4"),
            ServerItem("pstream", "Server 5"),
            ServerItem("vixsrc", "Server 6"),
            ServerItem("peestream", "Server 7"),
            ServerItem("purstream", "Server 8"),
            ServerItem("cinejoy", "Server 9"),
            ServerItem("turbovid", "Server 10"),
            ServerItem("insertunit", "Server 11"),
            ServerItem("streamaggregator", "Server 12"),
            ServerItem("vidapi", "Server 13"),
            ServerItem("vidnest", "Server 14"),
            ServerItem("lookmovie", "Server 15"),
            ServerItem("soapertv", "Server 16"),
            ServerItem("4khdhub", "Server 17"),
            ServerItem("vuflix", "Server 18"),
            ServerItem("animetsu", "Server 19"),
            ServerItem("hianime", "Server 20")
        )

        private val DEFAULT_ANIME_SERVERS = listOf(
            ServerItem("animetsu", "Server 1"),
            ServerItem("hianime", "Server 2"),
            ServerItem("zunime", "Server 3"),
            ServerItem("123anime", "Server 4"),
            ServerItem("anihq", "Server 5"),
            ServerItem("anineko", "Server 6"),
            ServerItem("vidnest", "Server 7"),
            ServerItem("rivestream", "Server 8"),
            ServerItem("movy", "Server 9"),
            ServerItem("frame", "Server 10"),
            ServerItem("vidzee", "Server 11"),
            ServerItem("pstream", "Server 12"),
            ServerItem("vixsrc", "Server 13"),
            ServerItem("peestream", "Server 14"),
            ServerItem("purstream", "Server 15"),
            ServerItem("cinejoy", "Server 16"),
            ServerItem("turbovid", "Server 17"),
            ServerItem("insertunit", "Server 18"),
            ServerItem("streamaggregator", "Server 19"),
            ServerItem("vidapi", "Server 20")
        )
    }

    private val extractorBridge = ExtractorBridge(application)
    private val mediaRepository = MediaRepository()

    private val _mediaDetails = MutableStateFlow<MediaDetails?>(null)
    val mediaDetails: StateFlow<MediaDetails?> = _mediaDetails

    private val _currentStream = MutableStateFlow<StreamInfo?>(null)
    val currentStream: StateFlow<StreamInfo?> = _currentStream


    private val _activeServerId = MutableStateFlow<String?>(null)
    val activeServerId: StateFlow<String?> = _activeServerId

    private val _activeServerName = MutableStateFlow("Server 1")
    val activeServerName: StateFlow<String> = _activeServerName

    private val _servers = MutableStateFlow<List<ServerItem>>(DEFAULT_MOVIE_SERVERS)
    val servers: StateFlow<List<ServerItem>> = _servers

    private val _subtitles = MutableStateFlow<List<SubtitleInfo>>(emptyList())
    val subtitles: StateFlow<List<SubtitleInfo>> = _subtitles

    private val _isExtracting = MutableStateFlow(true)
    val isExtracting: StateFlow<Boolean> = _isExtracting

    private val _isSwitchingServer = MutableStateFlow(false)
    val isSwitchingServer: StateFlow<Boolean> = _isSwitchingServer

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val serverCache = mutableMapOf<String, StreamResult>()
    private val failedServerIds = mutableSetOf<String>()

    private var extractJob: Job? = null
    private var serverJob: Job? = null

    private var currentType = "movie"
    private var currentId = 0
    private var currentTitle = ""
    private var currentYear = ""
    private var currentSeason = -1
    private var currentEpisode = -1
    private var currentIsAnime = false

    init {
        extractorBridge.initialize()
    }

    fun resetState() {
        extractJob?.cancel()
        serverJob?.cancel()
        extractJob = null
        serverJob = null
        extractorBridge.cancelPending()
        _currentStream.value = null
        _subtitles.value = emptyList()
        _mediaDetails.value = null
        _error.value = null
        _isExtracting.value = true
        _isSwitchingServer.value = false
        _activeServerId.value = null
        _activeServerName.value = "Server 1"
        serverCache.clear()
        failedServerIds.clear()
    }

    fun extractStreams(
        type: String,
        id: Int,
        title: String,
        year: String = "",
        season: Int = -1,
        episode: Int = -1,
        isAnime: Boolean = false,
        preferredServer: String? = null
    ) {
        resetState()

        currentType = type
        currentId = id
        currentTitle = title
        currentYear = year
        currentSeason = season
        currentEpisode = episode
        currentIsAnime = isAnime
        val mediaKey = "$type:$id:$season:$episode"

        _servers.value = if (isAnime) DEFAULT_ANIME_SERVERS else DEFAULT_MOVIE_SERVERS

        extractJob = viewModelScope.launch {
            _isExtracting.value = true
            _error.value = null

            // Load media metadata for paused screen
            launch {
                try {
                    val details = if (type == "movie") {
                        mediaRepository.getMovieDetails(id)
                    } else {
                        mediaRepository.getTvDetails(id)
                    }
                    if (mediaKey == "$currentType:$currentId:$currentSeason:$currentEpisode") {
                        _mediaDetails.value = details
                    }
                } catch (_: Exception) {}
            }

            // Load dynamic servers in background
            launch {
                val serverList = extractorBridge.getServers(isAnime)
                if (serverList.isNotEmpty() && mediaKey == "$currentType:$currentId:$currentSeason:$currentEpisode") {
                    _servers.value = serverList
                    _activeServerId.value?.let { updateServerDisplayName(it) }
                }
            }

            // 1. If preferredServer is specified from continue watching, try resuming that exact stream first
            if (!preferredServer.isNullOrBlank()) {
                try {
                    Log.d(TAG, "Attempting resume on preferred server: $preferredServer for $mediaKey")
                    val singleResult = extractorBridge.extractServer(
                        serverKey = preferredServer,
                        type = type,
                        id = id,
                        title = title,
                        year = year,
                        season = season,
                        episode = episode
                    )
                    if (mediaKey != "$currentType:$currentId:$currentSeason:$currentEpisode") {
                        Log.w(TAG, "Aborting preferredServer result because media changed")
                        return@launch
                    }
                    if (singleResult.streams.isNotEmpty()) {
                        serverCache[singleResult.server] = singleResult
                        _subtitles.value = singleResult.subtitles
                        _activeServerId.value = singleResult.server
                        updateServerDisplayName(singleResult.server)
                        _currentStream.value = singleResult.streams[0]
                        Log.d(TAG, "Successfully resumed on preferred server: $preferredServer")
                        _isExtracting.value = false
                        return@launch
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Preferred server $preferredServer failed, falling back to all servers", e)
                }
            }

            // 2. Standard prioritized multi-server extraction
            try {
                val result = extractorBridge.extract(type, id, title, year, season, episode, isAnime)
                if (mediaKey != "$currentType:$currentId:$currentSeason:$currentEpisode") {
                    Log.w(TAG, "Aborting extract result because media changed")
                    return@launch
                }
                serverCache[result.server] = result
                _subtitles.value = result.subtitles

                if (result.streams.isNotEmpty()) {
                    _activeServerId.value = result.server
                    updateServerDisplayName(result.server)
                    _currentStream.value = result.streams[0]
                    Log.d(TAG, "Selected initial server: ${result.server} with ${result.streams.size} streams")
                } else {
                    _error.value = "No playable streams found"
                }
            } catch (e: Exception) {
                if (mediaKey == "$currentType:$currentId:$currentSeason:$currentEpisode") {
                    Log.e(TAG, "Extraction failed", e)
                    _error.value = "Unable to connect to servers: ${e.message}"
                }
            } finally {
                if (mediaKey == "$currentType:$currentId:$currentSeason:$currentEpisode") {
                    _isExtracting.value = false
                }
            }
        }
    }

    fun selectServer(server: ServerItem) {
        if (server.id == _activeServerId.value && _currentStream.value != null) {
            return
        }

        extractJob?.cancel()
        serverJob?.cancel()

        val mediaKey = "$currentType:$currentId:$currentSeason:$currentEpisode"

        serverJob = viewModelScope.launch {
            _isSwitchingServer.value = true
            _activeServerId.value = server.id
            _activeServerName.value = server.name
            _error.value = null

            val cached = serverCache[server.id]
            if (cached != null && cached.streams.isNotEmpty()) {
                _currentStream.value = cached.streams[0]
                _subtitles.value = cached.subtitles
                _isSwitchingServer.value = false
                Log.d(TAG, "Switched to cached server ${server.name}")
                return@launch
            }

            try {
                val result = extractorBridge.extractServer(
                    serverKey = server.id,
                    type = currentType,
                    id = currentId,
                    title = currentTitle,
                    year = currentYear,
                    season = currentSeason,
                    episode = currentEpisode
                )
                if (mediaKey != "$currentType:$currentId:$currentSeason:$currentEpisode") {
                    return@launch
                }
                serverCache[server.id] = result
                if (result.streams.isNotEmpty()) {
                    _currentStream.value = result.streams[0]
                    _subtitles.value = result.subtitles
                    _error.value = null
                    Log.d(TAG, "Successfully extracted from ${server.name}")
                } else {
                    failedServerIds.add(server.id)
                    _error.value = "Server unavailable. Please choose another."
                }
            } catch (e: Exception) {
                if (mediaKey == "$currentType:$currentId:$currentSeason:$currentEpisode") {
                    Log.e(TAG, "Error switching to ${server.name}", e)
                    failedServerIds.add(server.id)
                    _error.value = "${server.name} failed to load."
                }
            } finally {
                if (mediaKey == "$currentType:$currentId:$currentSeason:$currentEpisode") {
                    _isSwitchingServer.value = false
                }
            }
        }
    }

    fun onPermanentStreamFailure() {
        val currentServer = _activeServerId.value ?: return
        failedServerIds.add(currentServer)
        Log.w(TAG, "Permanent stream error on server: $currentServer. Auto-switching.")
        selectNextAvailableServer()
    }

    private fun selectNextAvailableServer() {
        val serverList = _servers.value
        if (serverList.isEmpty()) return

        val currentIndex = serverList.indexOfFirst { it.id == _activeServerId.value }
        for (i in 1..serverList.size) {
            val nextIndex = (currentIndex + i) % serverList.size
            val candidate = serverList[nextIndex]
            if (candidate.id !in failedServerIds) {
                selectServer(candidate)
                return
            }
        }
        _error.value = "All servers currently unavailable."
    }

    private fun updateServerDisplayName(serverId: String) {
        val found = _servers.value.firstOrNull { it.id == serverId }
        if (found != null) {
            _activeServerName.value = found.name
        } else {
            val idx = _servers.value.indexOfFirst { it.id == serverId }
            if (idx >= 0) {
                _activeServerName.value = "Server ${idx + 1}"
            } else {
                _activeServerName.value = "Server 1"
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        resetState()
        extractorBridge.destroy()
    }
}
