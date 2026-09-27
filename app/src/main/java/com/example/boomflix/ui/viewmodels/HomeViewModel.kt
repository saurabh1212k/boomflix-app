package com.example.boomflix.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.boomflix.data.local.BoomflixDatabase
import com.example.boomflix.data.local.ContinueWatchingEntity
import com.example.boomflix.data.models.MediaItem
import com.example.boomflix.data.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MediaRepository()
    private val db = BoomflixDatabase.getInstance(application)

    val continueWatching: StateFlow<List<ContinueWatchingEntity>> = db.continueWatchingDao()
        .observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _trendingThisWeek = MutableStateFlow<List<MediaItem>>(emptyList())
    val trendingThisWeek: StateFlow<List<MediaItem>> = _trendingThisWeek

    private val _top10Movies = MutableStateFlow<List<MediaItem>>(emptyList())
    val top10Movies: StateFlow<List<MediaItem>> = _top10Movies

    private val _top10Tv = MutableStateFlow<List<MediaItem>>(emptyList())
    val top10Tv: StateFlow<List<MediaItem>> = _top10Tv

    private val _popularTv = MutableStateFlow<List<MediaItem>>(emptyList())
    val popularTv: StateFlow<List<MediaItem>> = _popularTv

    private val _actionMovies = MutableStateFlow<List<MediaItem>>(emptyList())
    val actionMovies: StateFlow<List<MediaItem>> = _actionMovies

    private val _comedyMovies = MutableStateFlow<List<MediaItem>>(emptyList())
    val comedyMovies: StateFlow<List<MediaItem>> = _comedyMovies

    private val _horrorMovies = MutableStateFlow<List<MediaItem>>(emptyList())
    val horrorMovies: StateFlow<List<MediaItem>> = _horrorMovies

    private val _romanceMovies = MutableStateFlow<List<MediaItem>>(emptyList())
    val romanceMovies: StateFlow<List<MediaItem>> = _romanceMovies

    private val _topRatedMovies = MutableStateFlow<List<MediaItem>>(emptyList())
    val topRatedMovies: StateFlow<List<MediaItem>> = _topRatedMovies

    private val _heroItem = MutableStateFlow<MediaItem?>(null)
    val heroItem: StateFlow<MediaItem?> = _heroItem

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        loadContent()
    }

    fun removeFromContinueWatching(mediaId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            db.continueWatchingDao().delete(mediaId)
        }
    }

    fun loadContent() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // Launch parallel async tasks for swift loading
                val trendingDef = async { repository.getTrending() }
                val top10MoviesDef = async { repository.getTrendingMovies() }
                val top10TvDef = async { repository.getTrendingTv() }
                val actionDef = async { repository.getActionMovies() }
                val comedyDef = async { repository.getComedyMovies() }
                val horrorDef = async { repository.getHorrorMovies() }
                val romanceDef = async { repository.getRomanceMovies() }
                val topRatedDef = async { repository.getTopRatedMovies() }
                val popTvDef = async { repository.getPopularTv() }

                val trending = trendingDef.await()
                _trendingThisWeek.value = trending
                _heroItem.value = trending.firstOrNull()

                val topMovies = top10MoviesDef.await()
                _top10Movies.value = if (topMovies.isNotEmpty()) topMovies else trending.filter { it.title != null }

                val topTv = top10TvDef.await()
                _top10Tv.value = topTv

                _actionMovies.value = actionDef.await()
                _comedyMovies.value = comedyDef.await()
                _horrorMovies.value = horrorDef.await()
                _romanceMovies.value = romanceDef.await()
                _topRatedMovies.value = topRatedDef.await()
                _popularTv.value = popTvDef.await()

                if (trending.isEmpty() && topMovies.isEmpty() && topTv.isEmpty()) {
                    _error.value = "Unable to load content. Please check connection and retry."
                }
            } catch (e: Exception) {
                _error.value = "Failed to load: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
