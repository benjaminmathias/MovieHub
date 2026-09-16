package com.benjamin.moviehub.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import java.util.Locale

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
    val isWatchlist: Boolean = false,
    val isWatched: Boolean = false,
    val runtimeMinutes: Int? = null,
)

/**
 * Associates a cached [MovieEntity] with a home category and its position in
 * that category's feed.
 *
 * The `(category, pageOrder)` index serves the category feed query
 * (`WHERE category = ? ORDER BY pageOrder`); the primary key cannot, since
 * `category` is not its leftmost column.
 */
@Entity(
    tableName = "movie_categories",
    primaryKeys = ["movieId", "category"],
    indices = [Index(value = ["category", "pageOrder"])],
)
data class MovieCategoryEntity(
    val movieId: Int,
    val category: String,
    val pageOrder: Int,
)

@Entity(
    tableName = "movie_search_results",
    primaryKeys = ["queryKey", "movieId"],
)
data class MovieSearchResultEntity(
    val queryKey: String,
    val movieId: Int,
    val pageOrder: Int,
)

/**
 * Pagination cursor for a single feed. TMDB paginates by page number, so one row
 * per feed (home category or normalized search query) is enough.
 */
@Entity(tableName = "remote_keys")
data class RemoteKey(
    @PrimaryKey val type: String,
    val nextKey: Int?,
)

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

class Converters {
    @TypeConverter
    fun fromGenreIds(genreIds: List<Int>): String = genreIds.joinToString(",")

    @TypeConverter
    fun toGenreIds(data: String): List<Int> =
        if (data.isEmpty()) {
            emptyList()
        } else {
            // mapNotNull: a corrupt row must never crash a Room read.
            data.split(",").mapNotNull { it.toIntOrNull() }
        }
}

object SearchQueryKey {
    fun normalize(query: String): String = query.trim().lowercase(Locale.ROOT)

    fun remoteKeyType(query: String): String = "SEARCH:${normalize(query)}"
}
