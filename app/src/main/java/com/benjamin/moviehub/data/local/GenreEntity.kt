package com.benjamin.moviehub.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Localized TMDB genre dictionary, persisted so movie genre names stay consistent
 * across every screen and survive restarts. Resolved from `genre/movie/list`; the
 * static mapper fallback only covers the window before the first refresh.
 */
@Entity(tableName = "genres")
data class GenreEntity(
    @PrimaryKey val id: Int,
    val name: String,
)
