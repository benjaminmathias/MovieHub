package com.benjamin.moviehub.data.remote

import com.google.gson.annotations.SerializedName

data class MovieResponse(
    @SerializedName("results") val movies: List<MovieDto>
)

data class MovieDto(
    @SerializedName("id") val id: Int,

    @SerializedName("title") val title: String,

    @SerializedName("overview") val description: String,

    @SerializedName("poster_path") val posterPath: String?,

    @SerializedName("backdrop_path") val backdropPath: String?,

    @SerializedName("vote_average") val voteAverage: Double,

    @SerializedName("release_date") val releaseDate: String? = "",

    @SerializedName("genre_ids") val genreIds: List<Int>? = null
)

data class MovieCreditsDto(
    @SerializedName("cast") val cast: List<ActorDto>
)

data class ActorDto(
    val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("character") val character: String,
    @SerializedName("profile_path") val profilePath: String?
)