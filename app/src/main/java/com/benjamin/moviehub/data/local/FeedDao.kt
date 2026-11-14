package com.benjamin.moviehub.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Offline feed state: Home category and search result positions, plus their paging cursors. */
@Dao
interface FeedDao {
    // region Home categories

    @Query(
        """
        SELECT movies.* FROM movies
        INNER JOIN movie_categories ON movies.id = movie_categories.movieId
        WHERE movie_categories.category = :category
        ORDER BY movie_categories.pageOrder ASC, movies.id ASC
        """,
    )
    fun getCategoryMoviesPaging(category: String): PagingSource<Int, MovieEntity>

    @Query(
        """
        SELECT movies.* FROM movies
        INNER JOIN movie_categories ON movies.id = movie_categories.movieId
        WHERE movie_categories.category = :category
        ORDER BY movie_categories.pageOrder ASC, movies.id ASC
        LIMIT 1
        """,
    )
    fun getHeroMovieFlow(category: String): Flow<MovieEntity?>

    @Query("SELECT movieId FROM movie_categories WHERE category = :category ORDER BY pageOrder ASC")
    suspend fun getCategoryMovieIds(category: String): List<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategoryMovies(items: List<MovieCategoryEntity>)

    @Query("DELETE FROM movie_categories WHERE category = :category")
    suspend fun clearCategoryMovies(category: String)

    // endregion

    // region Search results

    @Query(
        """
        SELECT movies.* FROM movies
        INNER JOIN movie_search_results ON movies.id = movie_search_results.movieId
        WHERE movie_search_results.queryKey = :queryKey
        ORDER BY movie_search_results.pageOrder ASC, movies.id ASC
        """,
    )
    fun searchMoviesPaging(queryKey: String): PagingSource<Int, MovieEntity>

    @Query("SELECT movieId FROM movie_search_results WHERE queryKey = :queryKey ORDER BY pageOrder ASC")
    suspend fun getSearchResultMovieIds(queryKey: String): List<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchResults(results: List<MovieSearchResultEntity>)

    @Query("DELETE FROM movie_search_results WHERE queryKey = :queryKey")
    suspend fun clearSearchResults(queryKey: String)

    /**
     * Removes movies left behind by cleared searches while keeping the active results,
     * Library movies, and movies referenced by a Home category.
     */
    @Query(
        """
        DELETE FROM movies
        WHERE isFavorite = 0
          AND isWatchlist = 0
          AND isWatched = 0
          AND id IN (:previousResultIds)
          AND id NOT IN (:preserveMovieIds)
          AND id NOT IN (SELECT movieId FROM movie_search_results)
          AND id NOT IN (SELECT movieId FROM movie_categories)
        """,
    )
    suspend fun deleteSearchOrphans(
        previousResultIds: List<Int>,
        preserveMovieIds: List<Int>,
    )

    // endregion

    // region Remote keys

    @Query("SELECT * FROM remote_keys WHERE type = :type")
    suspend fun getRemoteKey(type: String): RemoteKey?

    @Upsert
    suspend fun upsertRemoteKey(remoteKey: RemoteKey)

    @Query("DELETE FROM remote_keys WHERE type = :type")
    suspend fun clearRemoteKeysByType(type: String)

    // endregion
}
