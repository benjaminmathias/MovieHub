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
    val isWatchlist: Boolean = false,
    val isWatched: Boolean = false,
    val runtimeMinutes: Int? = null,
    val voteCount: Int? = null,
    /** Small poster variant used by the compact home rows. */
    val posterPathSmall: String? = null,
)
