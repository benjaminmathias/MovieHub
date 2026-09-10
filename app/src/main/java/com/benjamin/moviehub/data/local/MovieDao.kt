package com.benjamin.moviehub.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
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

    @Update
    suspend fun updateMovie(movie: MovieEntity)

    @Transaction
    suspend fun upsertMovieDetails(movie: MovieEntity): MovieEntity {
        val localMovie = getMovieById(movie.id)
        val mergedMovie =
            movie.copy(
                isFavorite = localMovie?.isFavorite ?: movie.isFavorite,
                isSearchResult = localMovie?.isSearchResult ?: movie.isSearchResult,
                runtimeMinutes = movie.runtimeMinutes ?: localMovie?.runtimeMinutes,
            )

        if (localMovie == null) {
            insertMovie(mergedMovie)
        } else {
            updateMovie(mergedMovie)
        }
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMovies(movies: List<MovieEntity>)

    // --- GESTION DES CLÉS (REMOTE KEYS) ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllKeys(remoteKey: List<MovieRemoteKey>)

    @Query("SELECT * FROM remote_keys WHERE movieId = :movieId AND type = :type")
    suspend fun getRemoteKeysForMovieId(
        movieId: Int,
        type: String,
    ): MovieRemoteKey?

    @Query("SELECT COUNT(*) FROM remote_keys WHERE type = :type")
    suspend fun getRemoteKeysCountByType(type: String): Int

    // --- MAINTENANCE ---
    @Query("DELETE FROM remote_keys WHERE type = :type")
    suspend fun clearRemoteKeysByType(type: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchResults(results: List<MovieSearchResultEntity>)

    @Query("DELETE FROM movie_search_results WHERE queryKey = :queryKey")
    suspend fun clearSearchResults(queryKey: String)

    @Query(
        """
        DELETE FROM movies
        WHERE isSearchResult = 1
          AND isFavorite = 0
          AND id NOT IN (:preserveMovieIds)
          AND id NOT IN (SELECT movieId FROM movie_search_results)
          AND id NOT IN (SELECT movieId FROM movie_categories)
        """,
    )
    suspend fun clearOrphanSearchMovies(preserveMovieIds: List<Int>)

    @Query("SELECT movieId FROM movie_search_results WHERE queryKey = :queryKey ORDER BY pageOrder ASC")
    suspend fun getSearchResultMovieIds(queryKey: String): List<Int>
}
