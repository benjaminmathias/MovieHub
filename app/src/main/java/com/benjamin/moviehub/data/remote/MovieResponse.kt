package com.benjamin.moviehub.data.remote

import com.google.gson.annotations.SerializedName

data class MovieResponse(
    @SerializedName("results") val movies: List<MovieDto> = emptyList(),
    @SerializedName("total_pages") val totalPages: Int? = null,
)

data class MovieGenresResponse(
    @SerializedName("genres") val genres: List<GenreDto> = emptyList(),
)

/**
 * TMDB exposes at most this many pages. Requesting beyond it is rejected by the API as an
 * invalid page, so pagination must stop here even when the reported total is higher.
 */
private const val TMDB_MAX_PAGE = 500

fun MovieResponse.isEndOfPagination(
    page: Int,
    pageSize: Int,
): Boolean {
    if (page >= TMDB_MAX_PAGE) return true
    return totalPages?.let { page >= it }
        ?: (movies.isEmpty() || movies.size < pageSize)
}

data class GenreDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String? = null,
)

data class MovieDto(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String? = null,
    @SerializedName("overview") val description: String? = null,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("vote_average") val voteAverage: Double,
    @SerializedName("release_date") val releaseDate: String? = "",
    @SerializedName("runtime") val runtimeMinutes: Int? = null,
    // Shape used by list endpoints (/popular)
    @SerializedName("genre_ids")
    val genreIds: List<Int>? = null,
    // Shape used by the detail endpoint (/movie/{id})
    @SerializedName("genres")
    val genres: List<GenreDto>? = null,
    @SerializedName("vote_count") val voteCount: Int? = null,
)

data class MovieCreditsDto(
    @SerializedName("cast") val cast: List<ActorDto> = emptyList(),
    @SerializedName("crew") val crew: List<CrewMemberDto> = emptyList(),
)

data class CrewMemberDto(
    @SerializedName("name") val name: String? = null,
    @SerializedName("job") val job: String? = null,
)

data class ActorDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String? = null,
    @SerializedName("character") val character: String? = null,
    @SerializedName("profile_path") val profilePath: String?,
)
