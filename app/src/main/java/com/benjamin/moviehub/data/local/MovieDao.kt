package com.benjamin.moviehub.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.benjamin.moviehub.domain.model.LibraryFlag
import kotlinx.coroutines.flow.Flow

@Dao
abstract class MovieDao {
    @Query("SELECT * FROM movies WHERE id = :movieId")
    abstract suspend fun getMovieById(movieId: Int): MovieEntity?

    @Query("SELECT * FROM movies WHERE id IN (:ids)")
    abstract suspend fun getMoviesByIds(ids: List<Int>): List<MovieEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertMovie(movie: MovieEntity)

    @Transaction
    open suspend fun upsertMovieDetails(movie: MovieEntity): MovieEntity {
        upsertMovies(listOf(movie))
        return getMovieById(movie.id) ?: movie
    }

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

    @Query("SELECT * FROM movies WHERE isFavorite = 1 OR isWatchlist = 1 OR isWatched = 1 ORDER BY title COLLATE NOCASE ASC, id ASC")
    abstract fun getLibraryMoviesFlow(): Flow<List<MovieEntity>>

    @Query(
        """
        SELECT movies.* FROM movies
        INNER JOIN movie_categories ON movies.id = movie_categories.movieId
        WHERE movie_categories.category = :category
        ORDER BY movie_categories.pageOrder ASC, movies.id ASC
        """,
    )
    abstract fun getCategoryMoviesPaging(category: String): PagingSource<Int, MovieEntity>

    @Query("SELECT movieId FROM movie_categories WHERE category = :category ORDER BY pageOrder ASC")
    abstract suspend fun getCategoryMovieIds(category: String): List<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertCategoryMovies(items: List<MovieCategoryEntity>)

    @Query("DELETE FROM movie_categories WHERE category = :category")
    abstract suspend fun clearCategoryMovies(category: String)

    @Query(
        """
        SELECT movies.* FROM movies
        INNER JOIN movie_categories ON movies.id = movie_categories.movieId
        WHERE movie_categories.category = :category
        ORDER BY movie_categories.pageOrder ASC, movies.id ASC
        LIMIT 1
        """,
    )
    abstract fun getHeroMovieFlow(category: String): Flow<MovieEntity?>

    @Query(
        """
        SELECT movies.* FROM movies
        INNER JOIN movie_search_results ON movies.id = movie_search_results.movieId
        WHERE movie_search_results.queryKey = :queryKey
        ORDER BY movie_search_results.pageOrder ASC, movies.id ASC
        """,
    )
    abstract fun searchMoviesPaging(queryKey: String): PagingSource<Int, MovieEntity>

    @Upsert
    protected abstract suspend fun upsertMoviesRaw(movies: List<MovieEntity>)

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

    @Upsert
    abstract suspend fun upsertRemoteKey(remoteKey: RemoteKey)

    @Query("SELECT * FROM remote_keys WHERE type = :type")
    abstract suspend fun getRemoteKey(type: String): RemoteKey?

    @Query("DELETE FROM remote_keys WHERE type = :type")
    abstract suspend fun clearRemoteKeysByType(type: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertSearchResults(results: List<MovieSearchResultEntity>)

    @Query("DELETE FROM movie_search_results WHERE queryKey = :queryKey")
    abstract suspend fun clearSearchResults(queryKey: String)

    @Query("SELECT movieId FROM movie_search_results WHERE queryKey = :queryKey ORDER BY pageOrder ASC")
    abstract suspend fun getSearchResultMovieIds(queryKey: String): List<Int>

    /**
     * Removes movies left behind by cleared searches while keeping the active results,
     * favorites, and movies referenced by a Home category.
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
    abstract suspend fun deleteSearchOrphans(
        previousResultIds: List<Int>,
        preserveMovieIds: List<Int>,
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertGenres(genres: List<GenreEntity>)

    @Query("SELECT * FROM genres")
    abstract suspend fun getGenres(): List<GenreEntity>
}
