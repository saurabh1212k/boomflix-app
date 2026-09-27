package com.example.boomflix

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Home : NavKey
@Serializable data object Movies : NavKey
@Serializable data object TvShows : NavKey
@Serializable data object Anime : NavKey
@Serializable data object MyList : NavKey
@Serializable data object Search : NavKey
@Serializable data class Details(val type: String, val id: Int) : NavKey
@Serializable data class Player(
    val type: String,
    val id: Int,
    val title: String,
    val year: String = "",
    val season: Int = -1,
    val episode: Int = -1,
    val isAnime: Boolean = false,
    val startPositionMs: Long = 0L,
    val preferredServer: String? = null,
    val posterPath: String? = null,
    val backdropPath: String? = null
) : NavKey
