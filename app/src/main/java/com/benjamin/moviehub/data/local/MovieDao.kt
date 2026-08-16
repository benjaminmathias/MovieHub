package com.benjamin.moviehub.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieDao {
    // --- FILMS : ACTIONS UNITAIRES ---
    @Query("SELECT * FROM movies WHERE id = :movieId")
    suspend fun getMovieById(movieId: Int): MovieEntity?

    @Query("SELECT * FROM movies WHERE id IN (:ids)")
    suspend fun getMoviesByIds(ids: List<Int>): List<MovieEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovie(movie: MovieEntity) // Pour ton toggleFavorite

    @Query("UPDATE movies SET isFavorite = :isFavorite WHERE id = :movieId")
    suspend fun updateFavoriteStatus(
        movieId: Int,
        isFavorite: Boolean,
    )

    // --- FILMS : LISTES & FLOWS ---
    @Query("SELECT id FROM movies WHERE isFavorite = 1")
    suspend fun getFavoriteMovieIds(): List<Int>

    @Query("SELECT * FROM movies WHERE isFavorite = 1")
    fun getFavoriteMoviesFlow(): Flow<List<MovieEntity>>

    // --- PAGINATION (SOURCES) ---
    @Query("SELECT * FROM movies WHERE isPopular = 1 ORDER BY pageOrder ASC")
    fun getPopularMoviesPaging(): PagingSource<Int, MovieEntity>

    @Query(
        """
        SELECT movies.* FROM movies
        INNER JOIN movie_search_results ON movies.id = movie_search_results.movieId
        WHERE movie_search_results.queryKey = :queryKey
        ORDER BY movie_search_results.pageOrder ASC
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

    @Query("SELECT movieId FROM movie_search_results WHERE queryKey = :queryKey ORDER BY pageOrder ASC")
    suspend fun getSearchResultMovieIds(queryKey: String): List<Int>

    @Query("UPDATE movies SET isPopular = 0, pageOrder = -1 WHERE isPopular = 1")
    suspend fun clearPopularMovies()
}
