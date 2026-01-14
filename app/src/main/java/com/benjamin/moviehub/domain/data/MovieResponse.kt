package com.benjamin.moviehub.domain.data

import com.google.gson.annotations.SerializedName

data class MovieResponse (
    @SerializedName("results")
    val movies: List<MovieDto>
)

data class MovieDto(
    val id: Int,
    val title: String,

    @SerializedName("overview")
    val description: String,

    @SerializedName("poster_path")
    val posterPath : String?,

    @SerializedName("vote_average")
    val rating: Double,
)