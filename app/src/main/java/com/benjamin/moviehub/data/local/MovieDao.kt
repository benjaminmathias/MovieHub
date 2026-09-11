package com.benjamin.moviehub.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieDao {
    // --- FILMS : ACTIONS UNITAIRES ---
    @Query("SELECT * FROM movies WHERE id = :movieId")
    suspend fun getMovieById(movieId: Int): MovieEntity?

    @Query("SELECT * FROM movies WHERE id IN (:ids)")
    suspend fun getMoviesByIds(ids: List<Int>): List<MovieEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovie(movie: MovieEntity)

    @Transaction
    suspend fun upsertMovieDetails(movie: MovieEntity): MovieEntity {
        val localMovie = getMovieById(movie.id)
        val mergedMovie =
            movie.copy(
                isFavorite = localMovie?.isFavorite ?: movie.isFavorite,
                runtimeMinutes = movie.runtimeMinutes ?: localMovie?.runtimeMinutes,
            )

        upsertMovies(listOf(mergedMovie))
        return mergedMovie
    }

    @Transaction
    suspend fun setFavorite(
        movie: MovieEntity,
        isFavorite: Boolean,
    ) {
        if (getMovieById(movie.id) == null) {
            insertMovie(movie.copy(isFavorite = isFavorite))
        } else {
            updateFavoriteStatus(movie.id, isFavorite)
        }
    }

    @Query("UPDATE movies SET isFavorite = :isFavorite WHERE id = :movieId")
    suspend fun updateFavoriteStatus(
        movieId: Int,
        isFavorite: Boolean,
    )

    // --- FILMS : LISTES & FLOWS ---
    @Query("SELECT * FROM movies WHERE isFavorite = 1 ORDER BY title COLLATE NOCASE ASC, id ASC")
    fun getFavoriteMoviesFlow(): Flow<List<MovieEntity>>

    @Query("SELECT id FROM movies WHERE isFavorite = 1 ORDER BY id ASC")
    fun getFavoriteMovieIdsFlow(): Flow<List<Int>>

    // --- CATEGORIES (ASSOCIATION + PAGINATION) ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategoryMovies(items: List<MovieCategoryEntity>)

    @Query("DELETE FROM movie_categories WHERE category = :category")
    suspend fun clearCategoryMovies(category: String)

    @Query(
        """
        SELECT movies.* FROM movies
        INNER JOIN movie_categories ON movies.id = movie_categories.movieId
        WHERE movie_categories.category = :category
        ORDER BY movie_categories.pageOrder ASC, movies.id ASC
        """,
    )
    fun getCategoryMoviesPaging(category: String): PagingSource<Int, MovieEntity>

    @Query("SELECT movieId FROM movie_categories WHERE category = :category ORDER BY pageOrder ASC")
    suspend fun getCategoryMovieIds(category: String): List<Int>

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

    @Query(
        """
        SELECT movies.* FROM movies
        INNER JOIN movie_search_results ON movies.id = movie_search_results.movieId
        WHERE movie_search_results.queryKey = :queryKey
        ORDER BY movie_search_results.pageOrder ASC, movies.id ASC
        """,
    )
    fun searchMoviesPaging(queryKey: String): PagingSource<Int, MovieEntity>

    @Upsert
    suspend fun upsertMovies(movies: List<MovieEntity>)

    // --- CLÉS DE PAGINATION (UNE PAR FEED) ---
    @Upsert
    suspend fun upsertRemoteKey(remoteKey: RemoteKey)

    @Query("SELECT * FROM remote_keys WHERE type = :type")
    suspend fun getRemoteKey(type: String): RemoteKey?

    @Query("DELETE FROM remote_keys WHERE type = :type")
    suspend fun clearRemoteKeysByType(type: String)

    // --- RECHERCHE ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchResults(results: List<MovieSearchResultEntity>)

    @Query("SELECT DISTINCT movieId FROM movie_search_results")
    suspend fun getAllSearchResultMovieIds(): List<Int>

    @Query("DELETE FROM movie_search_results")
    suspend fun clearSearchResults()

    @Query("DELETE FROM remote_keys WHERE type LIKE 'SEARCH:%'")
    suspend fun clearSearchRemoteKeys()

    @Query("SELECT movieId FROM movie_search_results WHERE queryKey = :queryKey ORDER BY pageOrder ASC")
    suspend fun getSearchResultMovieIds(queryKey: String): List<Int>

    /**
     * Removes movies left behind by cleared searches while keeping the active results,
     * favorites, and movies referenced by a Home category.
     */
    @Query(
        """
        DELETE FROM movies
        WHERE isFavorite = 0
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
}
