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
    @Query("SELECT * FROM movies WHERE id = :movieId")
    suspend fun getMovieById(movieId: Int): MovieEntity?

    @Query("SELECT * FROM movies WHERE id IN (:ids)")
    suspend fun getMoviesByIds(ids: List<Int>): List<MovieEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovie(movie: MovieEntity)

    @Transaction
    suspend fun upsertMovieDetails(movie: MovieEntity): MovieEntity {
        upsertMovies(listOf(movie))
        return getMovieById(movie.id) ?: movie
    }

    /**
     * Applies the non-null library flags, inserting the movie first when it is not
     * cached yet. Watchlist and watched are mutually exclusive: setting one true
     * clears the other.
     */
    @Transaction
    suspend fun setLibraryFlag(
        movie: MovieEntity,
        isFavorite: Boolean? = null,
        isWatchlist: Boolean? = null,
        isWatched: Boolean? = null,
    ) {
        if (getMovieById(movie.id) == null) {
            insertMovie(
                movie.copy(
                    isFavorite = isFavorite ?: movie.isFavorite,
                    isWatchlist = (isWatchlist ?: movie.isWatchlist) && isWatched != true,
                    isWatched = (isWatched ?: movie.isWatched) && isWatchlist != true,
                ),
            )
            return
        }

        updateLocalFlags(
            movieId = movie.id,
            isFavorite = isFavorite,
            isWatchlist = isWatchlist ?: if (isWatched == true) false else null,
            isWatched = isWatched ?: if (isWatchlist == true) false else null,
        )
    }

    @Query(
        """UPDATE movies SET
            isFavorite = COALESCE(:isFavorite, isFavorite),
            isWatchlist = COALESCE(:isWatchlist, isWatchlist),
            isWatched = COALESCE(:isWatched, isWatched)
            WHERE id = :movieId""",
    )
    suspend fun updateLocalFlags(
        movieId: Int,
        isFavorite: Boolean?,
        isWatchlist: Boolean?,
        isWatched: Boolean?,
    )

    @Query("SELECT * FROM movies WHERE isFavorite = 1 OR isWatchlist = 1 OR isWatched = 1 ORDER BY title COLLATE NOCASE ASC, id ASC")
    fun getLibraryMoviesFlow(): Flow<List<MovieEntity>>

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
    suspend fun upsertMoviesRaw(movies: List<MovieEntity>)

    /** Upsert network data without ever overwriting local library flags. */
    @Transaction
    suspend fun upsertMovies(movies: List<MovieEntity>) {
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
    suspend fun upsertRemoteKey(remoteKey: RemoteKey)

    @Query("SELECT * FROM remote_keys WHERE type = :type")
    suspend fun getRemoteKey(type: String): RemoteKey?

    @Query("DELETE FROM remote_keys WHERE type = :type")
    suspend fun clearRemoteKeysByType(type: String)

    @Query("DELETE FROM remote_keys WHERE type LIKE 'SEARCH:%'")
    suspend fun clearSearchRemoteKeys()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchResults(results: List<MovieSearchResultEntity>)

    @Query("SELECT DISTINCT movieId FROM movie_search_results")
    suspend fun getAllSearchResultMovieIds(): List<Int>

    @Query("DELETE FROM movie_search_results")
    suspend fun clearSearchResults()

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGenres(genres: List<GenreEntity>)

    @Query("SELECT * FROM genres")
    suspend fun getGenres(): List<GenreEntity>
}
