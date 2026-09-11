package com.benjamin.moviehub.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Pagination cursor for a single feed. TMDB paginates by page number, so one row
 * per feed (home category or normalized search query) is enough.
 */
@Entity(tableName = "remote_keys")
data class RemoteKey(
    @PrimaryKey val type: String,
    val nextKey: Int?,
)
