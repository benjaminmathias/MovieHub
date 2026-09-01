package com.benjamin.moviehub.domain.model

data class Movie(
    val id: Int,
    val title: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val voteAverage: Double,
    val releaseDate: String,
    val webUrl: String?,
    val isFavorite: Boolean,
    val genreIds: List<Int>,
    val genres: List<String>,
    val runtimeMinutes: Int? = null,
    val originalTitle: String? = null,
    val originalLanguage: String? = null,
    val status: String? = null,
    val voteCount: Int? = null,
    val budget: Long? = null,
    val revenue: Long? = null,
    val productionCountries: List<String> = emptyList(),
)
