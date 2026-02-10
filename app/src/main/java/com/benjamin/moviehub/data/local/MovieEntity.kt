package com.benjamin.moviehub.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "movies")
data class MovieEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val voteAverage: Double,
    val releaseDate: String,
    val genreIds: List<Int> = emptyList(),
    val isFavorite: Boolean = false,
    val isPopular: Boolean = false,
    val isSearchResult: Boolean = false,
    val pageOrder: Int = 0
)