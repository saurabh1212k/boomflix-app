package com.example.boomflix.data.api

import com.example.boomflix.data.models.*
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApi {

    companion object {
        const val BASE_URL = "https://api.themoviedb.org/3/"
    }

    // Trending
    @GET("trending/{media_type}/{time_window}")
    suspend fun getTrending(
        @Path("media_type") mediaType: String = "all",
        @Path("time_window") timeWindow: String = "week",
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1
    ): TmdbResponse<MediaItem>

    // Discover movies
    @GET("discover/movie")
    suspend fun discoverMovies(
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1,
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("with_genres") genres: String? = null
    ): TmdbResponse<MediaItem>

    // Discover TV
    @GET("discover/tv")
    suspend fun discoverTv(
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1,
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("with_genres") genres: String? = null
    ): TmdbResponse<MediaItem>

    // Movie details
    @GET("movie/{id}")
    suspend fun getMovieDetails(
        @Path("id") id: Int,
        @Query("api_key") apiKey: String,
        @Query("append_to_response") append: String = "external_ids,images",
        @Query("include_image_language") includeImageLanguage: String = "en,null"
    ): MediaDetails

    // TV details
    @GET("tv/{id}")
    suspend fun getTvDetails(
        @Path("id") id: Int,
        @Query("api_key") apiKey: String,
        @Query("append_to_response") append: String = "external_ids,images",
        @Query("include_image_language") includeImageLanguage: String = "en,null"
    ): MediaDetails

    // Season details
    @GET("tv/{id}/season/{season}")
    suspend fun getSeasonDetails(
        @Path("id") id: Int,
        @Path("season") season: Int,
        @Query("api_key") apiKey: String
    ): SeasonDetails

    // Credits
    @GET("{type}/{id}/credits")
    suspend fun getCredits(
        @Path("type") type: String,
        @Path("id") id: Int,
        @Query("api_key") apiKey: String
    ): CreditsResponse

    // Search multi
    @GET("search/multi")
    suspend fun searchMulti(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("page") page: Int = 1
    ): TmdbResponse<MediaItem>

    // Top rated movies
    @GET("movie/top_rated")
    suspend fun getTopRatedMovies(
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1
    ): TmdbResponse<MediaItem>

    // Popular TV
    @GET("tv/popular")
    suspend fun getPopularTv(
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1
    ): TmdbResponse<MediaItem>

    // Similar
    @GET("{type}/{id}/similar")
    suspend fun getSimilar(
        @Path("type") type: String,
        @Path("id") id: Int,
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1
    ): TmdbResponse<MediaItem>

    // Movie genres list
    @GET("genre/movie/list")
    suspend fun getMovieGenres(
        @Query("api_key") apiKey: String
    ): GenreListResponse

    // TV genres list
    @GET("genre/tv/list")
    suspend fun getTvGenres(
        @Query("api_key") apiKey: String
    ): GenreListResponse
}

data class GenreListResponse(val genres: List<Genre> = emptyList())
