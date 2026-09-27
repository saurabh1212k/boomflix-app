package com.example.boomflix.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.boomflix.data.models.Genre
import com.example.boomflix.data.models.MediaItem
import com.example.boomflix.data.repository.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MoviesViewModel : ViewModel() {
    private val repository = MediaRepository()

    private val _movies = MutableStateFlow<List<MediaItem>>(emptyList())
    val movies: StateFlow<List<MediaItem>> = _movies

    private val _genres = MutableStateFlow<List<Genre>>(emptyList())
    val genres: StateFlow<List<Genre>> = _genres

    private val _selectedGenreId = MutableStateFlow<Int?>(null)
    val selectedGenreId: StateFlow<Int?> = _selectedGenreId

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadGenres()
        loadMovies()
    }

    private fun loadGenres() {
        viewModelScope.launch {
            _genres.value = repository.getMovieGenres()
        }
    }

    fun selectGenre(genreId: Int?) {
        _selectedGenreId.value = genreId
        loadMovies()
    }

    private fun loadMovies() {
        viewModelScope.launch {
            _isLoading.value = true
            _movies.value = repository.discoverMovies(
                genres = _selectedGenreId.value?.toString()
            )
            _isLoading.value = false
        }
    }
}
