package com.benjamin.moviehub.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieDao {

    // --- FILMS : ACTIONS UNITAIRES ---
    @Query("SELECT * FROM movies WHERE id = :movieId")
    suspend fun getMovieById(movieId: Int): MovieEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovie(movie: MovieEntity) // Pour ton toggleFavorite

    @Query("UPDATE movies SET isFavorite = :isFavorite WHERE id = :movieId")
    suspend fun updateFavoriteStatus(movieId: Int, isFavorite: Boolean)

    // --- FILMS : LISTES & FLOWS ---
    @Query("SELECT id FROM movies WHERE isFavorite = 1")
    suspend fun getFavoriteMovieIds(): List<Int>

    @Query("SELECT * FROM movies WHERE isFavorite = 1")
    fun getFavoriteMoviesFlow(): Flow<List<MovieEntity>>

    // --- PAGINATION (SOURCES) ---
    @Query("SELECT * FROM movies WHERE isPopular = 1 ORDER BY pageOrder ASC")
    fun getPopularMoviesPaging(): PagingSource<Int, MovieEntity>

    @Query("SELECT * FROM movies WHERE isSearchResult = 1 AND title LIKE '%'|| :query ||'%' ORDER BY pageOrder ASC")
    fun searchMoviesPaging(query: String): PagingSource<Int, MovieEntity>

    // --- LOGIQUE DE SYNCHRONISATION (UPSERT) ---
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(movies: List<MovieEntity>): List<Long>

    @Query("UPDATE movies SET isPopular = 1 WHERE id = :movieId")
    suspend fun markAsPopular(movieId: Int)

    @Query("UPDATE movies SET isSearchResult = 1 WHERE id = :movieId")
    suspend fun markAsSearchResult(movieId: Int)

    @Transaction
    suspend fun upsertMovies(movies: List<MovieEntity>, isSearch: Boolean) {
        movies.forEach { movie ->
            val id = insertOrIgnore(listOf(movie)).first()
            if (id == -1L) {
                if (isSearch) markAsSearchResult(movie.id) else markAsPopular(movie.id)
            }
        }
    }

    // --- GESTION DES CLÉS (REMOTE KEYS) ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllKeys(remoteKey: List<MovieRemoteKey>)

    @Query("SELECT * FROM remote_keys WHERE movieId = :movieId AND type = :type")
    suspend fun getRemoteKeysForMovieId(movieId: Int, type: String): MovieRemoteKey?

    @Query("SELECT COUNT(*) FROM remote_keys WHERE type = :type")
    suspend fun getRemoteKeysCountByType(type: String): Int

    // --- MAINTENANCE ---
    @Query("DELETE FROM remote_keys WHERE type = :type")
    suspend fun clearRemoteKeysByType(type: String)

    @Query("DELETE FROM movies WHERE isPopular = 1 AND isFavorite = 0")
    suspend fun clearPopularMovies()

    @Query("UPDATE movies SET isSearchResult = 0")
    suspend fun clearSearchResults()

    @Query("UPDATE movies SET pageOrder = -1 WHERE isPopular = 1")
    suspend fun resetPopularPageOrders()
}