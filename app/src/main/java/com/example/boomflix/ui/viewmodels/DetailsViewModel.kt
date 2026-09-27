package com.example.boomflix.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.boomflix.data.models.*
import com.example.boomflix.data.repository.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DetailsViewModel : ViewModel() {
    private val repository = MediaRepository()

    private val _details = MutableStateFlow<MediaDetails?>(null)
    val details: StateFlow<MediaDetails?> = _details

    private val _cast = MutableStateFlow<List<CastMember>>(emptyList())
    val cast: StateFlow<List<CastMember>> = _cast

    private val _episodes = MutableStateFlow<List<Episode>>(emptyList())
    val episodes: StateFlow<List<Episode>> = _episodes

    private val _selectedSeason = MutableStateFlow(1)
    val selectedSeason: StateFlow<Int> = _selectedSeason

    private val _similar = MutableStateFlow<List<MediaItem>>(emptyList())
    val similar: StateFlow<List<MediaItem>> = _similar

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadDetails(type: String, id: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _details.value = if (type == "movie") {
                repository.getMovieDetails(id)
            } else {
                repository.getTvDetails(id)
            }
            _isLoading.value = false

            // Load cast
            launch { _cast.value = repository.getCredits(type, id).take(10) }

            // Load similar
            launch { _similar.value = repository.getSimilar(type, id) }

            // Load first season episodes for TV
            if (type == "tv") {
                loadSeason(id, 1)
            }
        }
    }

    fun selectSeason(tvId: Int, seasonNumber: Int) {
        _selectedSeason.value = seasonNumber
        loadSeason(tvId, seasonNumber)
    }

    private fun loadSeason(tvId: Int, seasonNumber: Int) {
        viewModelScope.launch {
            val seasonDetails = repository.getSeasonDetails(tvId, seasonNumber)
            _episodes.value = seasonDetails?.episodes ?: emptyList()
        }
    }
}
