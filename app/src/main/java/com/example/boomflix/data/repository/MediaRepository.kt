package com.example.boomflix.data.repository

import com.example.boomflix.BuildConfig
import com.example.boomflix.data.api.NetworkModule
import com.example.boomflix.data.models.*

class MediaRepository {
    private val api = NetworkModule.tmdbApi
    private val apiKey = BuildConfig.TMDB_API_KEY

    suspend fun getTrending(mediaType: String = "all", timeWindow: String = "week", page: Int = 1): List<MediaItem> {
        return try {
            api.getTrending(mediaType, timeWindow, apiKey, page).results
        } catch (e: Exception) {
            android.util.Log.e("MediaRepository", "Failed to fetch trending: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun discoverMovies(page: Int = 1, genres: String? = null): List<MediaItem> {
        return try {
            api.discoverMovies(apiKey, page, genres = genres).results
        } catch (e: Exception) {
            android.util.Log.e("MediaRepository", "Failed to discover movies: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun discoverTv(page: Int = 1, genres: String? = null): List<MediaItem> {
        return try {
            api.discoverTv(apiKey, page, genres = genres).results
        } catch (e: Exception) {
            android.util.Log.e("MediaRepository", "Failed to discover TV: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getMovieDetails(id: Int): MediaDetails? {
        return try {
            api.getMovieDetails(id, apiKey)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getTvDetails(id: Int): MediaDetails? {
        return try {
            api.getTvDetails(id, apiKey)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getSeasonDetails(tvId: Int, season: Int): SeasonDetails? {
        return try {
            api.getSeasonDetails(tvId, season, apiKey)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getCredits(type: String, id: Int): List<CastMember> {
        return try {
            api.getCredits(type, id, apiKey).cast
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun searchMulti(query: String, page: Int = 1): List<MediaItem> {
        return try {
            api.searchMulti(apiKey, query, page).results.filter {
                it.mediaType == "movie" || it.mediaType == "tv"
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getTopRatedMovies(page: Int = 1): List<MediaItem> {
        return try {
            api.getTopRatedMovies(apiKey, page).results
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getPopularTv(page: Int = 1): List<MediaItem> {
        return try {
            api.getPopularTv(apiKey, page).results
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getSimilar(type: String, id: Int, page: Int = 1): List<MediaItem> {
        return try {
            api.getSimilar(type, id, apiKey, page).results
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getActionMovies(page: Int = 1): List<MediaItem> {
        return discoverMovies(page, genres = "28")
    }

    suspend fun getComedyMovies(page: Int = 1): List<MediaItem> {
        return discoverMovies(page, genres = "35")
    }

    suspend fun getHorrorMovies(page: Int = 1): List<MediaItem> {
        return discoverMovies(page, genres = "27")
    }

    suspend fun getRomanceMovies(page: Int = 1): List<MediaItem> {
        return discoverMovies(page, genres = "10749")
    }

    suspend fun getTrendingMovies(page: Int = 1): List<MediaItem> {
        return getTrending(mediaType = "movie", timeWindow = "day", page = page)
    }

    suspend fun getTrendingTv(page: Int = 1): List<MediaItem> {
        return getTrending(mediaType = "tv", timeWindow = "week", page = page)
    }

    suspend fun getMovieGenres(): List<Genre> {
        return try {
            api.getMovieGenres(apiKey).genres
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getTvGenres(): List<Genre> {
        return try {
            api.getTvGenres(apiKey).genres
        } catch (e: Exception) {
            emptyList()
        }
    }
}

