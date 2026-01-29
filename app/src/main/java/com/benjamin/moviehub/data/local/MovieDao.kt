package com.benjamin.moviehub.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieDao {

    @Query("SELECT * FROM movies WHERE isPopular = 1")
    fun getPopularMovies(): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE isFavorite = 1")
    fun getFavoriteMovies(): Flow<List<MovieEntity>>

    @Query("SELECT id FROM movies WHERE isFavorite = 1")
    suspend fun getFavoriteMovieIds(): List<Int>

    @Query("SELECT * FROM movies WHERE id = :movieId")
    suspend fun getMovieById(movieId: Int): MovieEntity?

    @Query("UPDATE movies SET isFavorite = :isFavorite WHERE id = :movieId")
    suspend fun updateFavoriteStatus(movieId: Int, isFavorite: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovies(movies: List<MovieEntity>)

    @Delete
    suspend fun deleteMovie(movie: MovieEntity)

    /*@Query("DELETE FROM movies WHERE isFavorite = 0")
    suspend fun clearNonFavoriteMovies()*/

    // PagingSource depuis Room
    @Query("SELECT * FROM movies WHERE isPopular = 1 ORDER BY pageOrder ASC")
    fun getPopularMoviesPaging(): PagingSource<Int, MovieEntity>

    @Query("DELETE FROM movies WHERE isPopular = 1 AND isFavorite = 0")
    suspend fun clearPopularMovies()

    //RemoteKeys for Paging
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllKeys(remoteKey: List<MovieRemoteKey>)

    @Query("SELECT * FROM remote_keys WHERE movieId = :movieId")
    suspend fun getRemoteKeysForMovieId(movieId: Int): MovieRemoteKey?

    @Query("DELETE from remote_keys")
    suspend fun clearRemotesKeys()

    @Query ("SELECT COUNT(*) FROM remote_keys")
    suspend fun getRemoteKeysCount(): Int
}