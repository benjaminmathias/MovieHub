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
        upsertMoviesRaw(
            movies.map { incoming ->
                val local = localById[incoming.id]
                val watched = local?.isWatched ?: incoming.isWatched
                incoming.copy(
                    isFavorite = local?.isFavorite ?: incoming.isFavorite,
                    isWatchlist = (local?.isWatchlist ?: incoming.isWatchlist) && !watched,
                    isWatched = watched,
                    runtimeMinutes = incoming.runtimeMinutes ?: local?.runtimeMinutes,
                )
            },
        )
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
     * Setting watchlist or watched true clears the other; setting one false keeps
     * the other flag unchanged.
     */
    @Transaction
    open suspend fun setLibraryFlag(
        movie: MovieEntity,
        flag: LibraryFlag,
        value: Boolean,
    ) {
        if (getMovieById(movie.id) == null) {
            val initialMovie =
                when (flag) {
                    LibraryFlag.FAVORITE -> movie.copy(isFavorite = value)
                    LibraryFlag.WATCHLIST -> movie.copy(isWatchlist = value, isWatched = movie.isWatched && !value)
                    LibraryFlag.WATCHED -> movie.copy(isWatchlist = movie.isWatchlist && !value, isWatched = value)
                }
            insertMovie(initialMovie)
            return
        }

        when (flag) {
            LibraryFlag.FAVORITE -> updateLocalFlags(movie.id, isFavorite = value, isWatchlist = null, isWatched = null)
            LibraryFlag.WATCHLIST ->
                updateLocalFlags(movie.id, isFavorite = null, isWatchlist = value, isWatched = if (value) false else null)
            LibraryFlag.WATCHED ->
                updateLocalFlags(movie.id, isFavorite = null, isWatchlist = if (value) false else null, isWatched = value)
        }
    }

    /** Toggle against the persisted value so rapid taps cannot reuse a stale UI flag. */
    @Transaction
    open suspend fun toggleFavorite(movie: MovieEntity) {
        val current = getMovieById(movie.id)
        if (current == null) {
            insertMovie(movie.copy(isFavorite = true))
        } else {
            updateLocalFlags(movie.id, !current.isFavorite, null, null)
        }
    }

    // endregion

    // region Internal writes

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertMovie(movie: MovieEntity)

    @Upsert
    protected abstract suspend fun upsertMoviesRaw(movies: List<MovieEntity>)

    @Query(
        """UPDATE movies SET
            isFavorite = COALESCE(:isFavorite, isFavorite),
            isWatchlist = COALESCE(:isWatchlist, isWatchlist),
            isWatched = COALESCE(:isWatched, isWatched)
            WHERE id = :movieId""",
    )
    protected abstract suspend fun updateLocalFlags(
        movieId: Int,
        isFavorite: Boolean?,
        isWatchlist: Boolean?,
        isWatched: Boolean?,
    )

    // endregion
}
