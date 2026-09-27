package com.example.boomflix.data.models

import com.google.gson.annotations.SerializedName

data class TmdbResponse<T>(
    val page: Int = 1,
    val results: List<T> = emptyList(),
    @SerializedName("total_pages") val totalPages: Int = 1,
    @SerializedName("total_results") val totalResults: Int = 0
)

data class MediaItem(
    val id: Int,
    val title: String? = null,
    val name: String? = null,
    val overview: String? = null,
    @SerializedName("poster_path") val posterPath: String? = null,
    @SerializedName("backdrop_path") val backdropPath: String? = null,
    @SerializedName("release_date") val releaseDate: String? = null,
    @SerializedName("first_air_date") val firstAirDate: String? = null,
    @SerializedName("last_air_date") val lastAirDate: String? = null,
    @SerializedName("vote_average") val voteAverage: Double = 0.0,
    @SerializedName("vote_count") val voteCount: Int = 0,
    @SerializedName("genre_ids") val genreIds: List<Int> = emptyList(),
    @SerializedName("media_type") val mediaType: String? = null,
    @SerializedName("original_language") val originalLanguage: String? = null,
    val popularity: Double = 0.0,
    val adult: Boolean = false
) {
    val displayTitle: String get() = title ?: name ?: "Unknown"
    val displayDate: String get() = releaseDate ?: firstAirDate ?: ""
    val year: String get() = displayDate.take(4)
    val posterUrl: String? get() = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
    val backdropUrl: String? get() = backdropPath?.let { "https://image.tmdb.org/t/p/original$it" }
    val rating: String get() = String.format("%.1f", voteAverage)
}

data class MediaDetails(
    val id: Int,
    val title: String? = null,
    val name: String? = null,
    val overview: String? = null,
    @SerializedName("poster_path") val posterPath: String? = null,
    @SerializedName("backdrop_path") val backdropPath: String? = null,
    @SerializedName("release_date") val releaseDate: String? = null,
    @SerializedName("first_air_date") val firstAirDate: String? = null,
    @SerializedName("last_air_date") val lastAirDate: String? = null,
    @SerializedName("vote_average") val voteAverage: Double = 0.0,
    @SerializedName("vote_count") val voteCount: Int = 0,
    val runtime: Int? = null,
    @SerializedName("episode_run_time") val episodeRunTime: List<Int>? = null,
    val status: String? = null,
    val tagline: String? = null,
    val genres: List<Genre> = emptyList(),
    @SerializedName("number_of_seasons") val numberOfSeasons: Int? = null,
    @SerializedName("number_of_episodes") val numberOfEpisodes: Int? = null,
    @SerializedName("external_ids") val externalIds: ExternalIds? = null,
    @SerializedName("last_episode_to_air") val lastEpisodeToAir: Episode? = null,
    @SerializedName("next_episode_to_air") val nextEpisodeToAir: Episode? = null,
    @SerializedName("in_production") val inProduction: Boolean? = null,
    val seasons: List<Season>? = null,
    val images: TmdbImages? = null,
    val videos: VideoResponse? = null
) {
    val displayTitle: String get() = title ?: name ?: "Unknown"
    val displayDate: String get() = releaseDate ?: firstAirDate ?: ""
    val year: String get() = displayDate.take(4)
    val posterUrl: String? get() = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
    val backdropUrl: String? get() = backdropPath?.let { "https://image.tmdb.org/t/p/original$it" }
    val rating: String get() = String.format("%.1f", voteAverage)
    val trailerKey: String? get() {
        val list = videos?.results ?: return null
        val ytTrailer = list.firstOrNull { it.site.equals("YouTube", ignoreCase = true) && it.type.equals("Trailer", ignoreCase = true) }
        val ytTeaser = list.firstOrNull { it.site.equals("YouTube", ignoreCase = true) }
        return (ytTrailer ?: ytTeaser)?.key
    }
    val logoUrl: String? get() {
        val logoList = images?.logos ?: return null
        val englishLogo = logoList.firstOrNull { it.iso6391 == "en" }
        val fallbackLogo = logoList.firstOrNull()
        val path = (englishLogo ?: fallbackLogo)?.filePath ?: return null
        return "https://image.tmdb.org/t/p/w500$path"
    }
    val displayRuntime: String get() {
        val mins = runtime ?: episodeRunTime?.firstOrNull() ?: return ""
        return if (mins >= 60) "${mins / 60}h ${mins % 60}m" else "${mins}m"
    }
}

data class VideoResponse(
    val results: List<VideoItem> = emptyList()
)

data class VideoItem(
    val id: String = "",
    val key: String = "",
    val name: String = "",
    val site: String = "",
    val type: String = ""
)

data class TmdbImages(
    val logos: List<TmdbImageItem>? = null
)

data class TmdbImageItem(
    @SerializedName("file_path") val filePath: String,
    @SerializedName("iso_639_1") val iso6391: String? = null
)

data class Genre(val id: Int, val name: String)

data class ExternalIds(
    @SerializedName("imdb_id") val imdbId: String? = null,
    @SerializedName("tvdb_id") val tvdbId: Int? = null
)

data class Season(
    val id: Int,
    @SerializedName("season_number") val seasonNumber: Int,
    val name: String? = null,
    @SerializedName("episode_count") val episodeCount: Int = 0,
    @SerializedName("poster_path") val posterPath: String? = null,
    @SerializedName("air_date") val airDate: String? = null
)

data class SeasonDetails(
    val id: Int,
    @SerializedName("season_number") val seasonNumber: Int,
    val episodes: List<Episode> = emptyList()
)

data class Episode(
    val id: Int,
    @SerializedName("episode_number") val episodeNumber: Int,
    val name: String? = null,
    val overview: String? = null,
    @SerializedName("still_path") val stillPath: String? = null,
    val runtime: Int? = null,
    @SerializedName("air_date") val airDate: String? = null,
    @SerializedName("vote_average") val voteAverage: Double = 0.0
) {
    val stillUrl: String? get() = stillPath?.let { "https://image.tmdb.org/t/p/w500$it" }
}

data class CreditsResponse(
    val id: Int,
    val cast: List<CastMember> = emptyList()
)

data class CastMember(
    val id: Int,
    val name: String,
    val character: String? = null,
    @SerializedName("profile_path") val profilePath: String? = null,
    val order: Int = 0
) {
    val profileUrl: String? get() = profilePath?.let { "https://image.tmdb.org/t/p/w185$it" }
}
