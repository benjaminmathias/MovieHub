package com.benjamin.moviehub.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.benjamin.moviehub.domain.model.LibraryFlag
import kotlinx.coroutines.flow.Flow

/** Cached movie rows and the local Library flags stored on them. */
@Dao
abstract class MovieDao {
    // region Reads

    @Query("SELECT * FROM movies WHERE id = :movieId")
    abstract suspend fun getMovieById(movieId: Int): MovieEntity?

    @Query("SELECT * FROM movies WHERE id IN (:ids)")
    abstract suspend fun getMoviesByIds(ids: List<Int>): List<MovieEntity>

    @Query("SELECT * FROM movies WHERE isFavorite = 1 OR isWatchlist = 1 OR isWatched = 1 ORDER BY title COLLATE NOCASE ASC, id ASC")
    abstract fun getLibraryMoviesFlow(): Flow<List<MovieEntity>>

    // endregion

    // region Network writes

    /** Upsert network data without ever overwriting local library flags. */
    @Transaction
    open suspend fun upsertMovies(movies: List<MovieEntity>) {
        if (movies.isEmpty()) return
        val localById = getMoviesByIds(movies.map { it.id }).associateBy { it.id }
        upsertMoviesRaw(movies.map { incoming -> incoming.mergedWithLocal(localById[incoming.id]) })
    }

    @Transaction
    open suspend fun upsertMovieDetails(movie: MovieEntity): MovieEntity {
        upsertMovies(listOf(movie))
        return getMovieById(movie.id) ?: movie
    }

    // endregion

    // region Library flags

    /**
     * Updates one library flag, inserting the movie first when it is not cached.
     * The persisted row, not the caller's copy, is the base of the update.
     */
    @Transaction
    open suspend fun setLibraryFlag(
        movie: MovieEntity,
        flag: LibraryFlag,
        value: Boolean,
    ) {
        val current = getMovieById(movie.id)
        if (current == null) {
            insertMovie(movie.withLibraryFlag(flag, value))
            return
        }
        val updated = current.withLibraryFlag(flag, value)
        updateLibraryFlags(movie.id, updated.isFavorite, updated.isWatchlist, updated.isWatched)
    }

    /** Toggle against the persisted value so rapid taps cannot reuse a stale UI flag. */
    @Transaction
    open suspend fun toggleFavorite(movie: MovieEntity) {
        val isFavorite = getMovieById(movie.id)?.isFavorite ?: false
        setLibraryFlag(movie, LibraryFlag.FAVORITE, !isFavorite)
    }

    // endregion

    // region Internal writes

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertMovie(movie: MovieEntity)

    @Upsert
    protected abstract suspend fun upsertMoviesRaw(movies: List<MovieEntity>)

    @Query("UPDATE movies SET isFavorite = :isFavorite, isWatchlist = :isWatchlist, isWatched = :isWatched WHERE id = :movieId")
    protected abstract suspend fun updateLibraryFlags(
        movieId: Int,
        isFavorite: Boolean,
        isWatchlist: Boolean,
        isWatched: Boolean,
    )

    // endregion
}
