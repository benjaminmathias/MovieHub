package com.benjamin.moviehub.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "remote_keys")
data class MovieRemoteKey(
    @PrimaryKey val movieId: Int,
    val prevKey: Int?,
    val nextKey: Int?,
    val type: String // "POPULAR" or "SEARCH"
)