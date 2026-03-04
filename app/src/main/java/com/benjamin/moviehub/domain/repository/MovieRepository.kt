package com.benjamin.moviehub.domain.repository

import androidx.paging.PagingData
import com.benjamin.moviehub.domain.model.Actor
import com.benjamin.moviehub.domain.model.Movie
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing movies data
 */
interface MovieRepository {
    /**
     * Get paged movies from the API, default ones or through query
     */
    fun getPagedMovies(query: String? = null): Flow<PagingData<Movie>>

    /**
     * Get movie details from the API and store it in the db the first time
     * Then pull from the db only if the API call fails or the movie already exist in it
     */
    suspend fun getMovieDetails(movieId: Int): Movie

    /**
     * Toggle the favorite status of a movie by saving in the db
     */
    suspend fun toggleFavorite(
        movie: Movie,
        isFavorite: Boolean,
    )

    /**
     * Get favorite movies from the db
     */
    fun getFavoriteMovies(): Flow<List<Movie>>

    /**
     * Get movie actors from the API
     */
    suspend fun getMovieActors(movieId: Int): Result<List<Actor>>

    suspend fun syncPopularMoviesCache()
}
