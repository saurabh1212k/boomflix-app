package com.example.boomflix.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.boomflix.data.models.MediaItem
import com.example.boomflix.data.repository.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AnimeViewModel : ViewModel() {
    private val repository = MediaRepository()

    private val _anime = MutableStateFlow<List<MediaItem>>(emptyList())
    val anime: StateFlow<List<MediaItem>> = _anime

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadAnime()
    }

    private fun loadAnime() {
        viewModelScope.launch {
            _isLoading.value = true
            // Genre 16 = Animation, filtered to Japanese anime
            _anime.value = repository.discoverTv(genres = "16")
            _isLoading.value = false
        }
    }
}
