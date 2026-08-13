package com.benjamin.moviehub.data.local

import androidx.room.Entity

@Entity(
    tableName = "movie_search_results",
    primaryKeys = ["queryKey", "movieId"],
)
data class MovieSearchResultEntity(
    val queryKey: String,
    val movieId: Int,
    val pageOrder: Int,
)
