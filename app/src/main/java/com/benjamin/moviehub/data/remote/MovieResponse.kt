package com.benjamin.moviehub.data.remote

import com.google.gson.annotations.SerializedName

data class MovieResponse(
    @SerializedName("results") val movies: List<MovieDto>,
)

data class GenreDto(
    val id: Int,
    val name: String,
)

data class MovieDto(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("overview") val description: String,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("vote_average") val voteAverage: Double,
    @SerializedName("release_date") val releaseDate: String? = "",
    @SerializedName("runtime") val runtimeMinutes: Int? = null,
    // Format utilisé par la Liste (/popular)
    @SerializedName("genre_ids")
    val genreIds: List<Int>? = null,
    // Format utilisé par le Détail (/movie/{id})
    @SerializedName("genres")
    val genres: List<GenreDto>? = null,
    @SerializedName("original_title") val originalTitle: String? = null,
    @SerializedName("original_language") val originalLanguage: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("vote_count") val voteCount: Int? = null,
    @SerializedName("budget") val budget: Long? = null,
    @SerializedName("revenue") val revenue: Long? = null,
    @SerializedName("production_countries")
    val productionCountries: List<ProductionCountryDto>? = null,
)

data class MovieCreditsDto(
    @SerializedName("cast") val cast: List<ActorDto> = emptyList(),
    @SerializedName("crew") val crew: List<CrewMemberDto> = emptyList(),
)

data class ProductionCountryDto(
    @SerializedName("name") val name: String,
)

data class CrewMemberDto(
    @SerializedName("name") val name: String,
    @SerializedName("job") val job: String,
)

data class ActorDto(
    val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("character") val character: String,
    @SerializedName("profile_path") val profilePath: String?,
)
