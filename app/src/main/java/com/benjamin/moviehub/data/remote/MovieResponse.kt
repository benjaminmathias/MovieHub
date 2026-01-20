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

    @SerializedName("release_date") val releaseDate: String? = ""
)