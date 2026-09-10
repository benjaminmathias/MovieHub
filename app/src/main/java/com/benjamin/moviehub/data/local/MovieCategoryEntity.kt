package com.benjamin.moviehub.data.local

import androidx.room.Entity
import androidx.room.Index

/**
 * Associates a cached [MovieEntity] with a home category and its position in
 * that category's feed.
 *
 * Membership and ordering live here so the same movie can belong to several
 * categories with a different order in each, independently from the canonical
 * movie row.
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
