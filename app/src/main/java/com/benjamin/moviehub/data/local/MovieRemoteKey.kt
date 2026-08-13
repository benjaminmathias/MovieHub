package com.benjamin.moviehub.data.local

import androidx.room.Entity

@Entity(
    tableName = "remote_keys",
    primaryKeys = ["movieId", "type"],
)
data class MovieRemoteKey(
    val movieId: Int,
    val prevKey: Int?,
    val nextKey: Int?,
    val type: String, // "POPULAR" or "SEARCH"
)
