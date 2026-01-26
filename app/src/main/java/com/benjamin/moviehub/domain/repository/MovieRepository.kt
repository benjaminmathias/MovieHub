package com.benjamin.moviehub.domain.repository

import com.benjamin.moviehub.domain.model.Movie
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing movies data
 * Offline-first through Room database as a SSOT
 */
interface MovieRepository {
    /**
     * Get popular movies from the API and store them in the database the first time
     * Then pull from the database only if the API call fails
     */
    fun getPopularMovies(): Flow<List<Movie>>

    /**
     * Get movie details from the API and store it in the db the first time
     * Then pull from the db only if the API call fails or the movie already exist in it
     */
    suspend fun getMovieDetails(movieId: Int): Movie

    /**
     * Search movies by a query and return a list of movies
     */
    suspend fun getSearchedMovies(query: String): List<Movie>

    /**
     * Toggle the favorite status of a movie by saving in the db
     */
    suspend fun toggleFavorite(movie: Movie, isFavorite: Boolean)

    /**
     * Get favorite movies from the db
     */
    suspend fun getFavoriteMovies() : Flow<List<Movie>>
}