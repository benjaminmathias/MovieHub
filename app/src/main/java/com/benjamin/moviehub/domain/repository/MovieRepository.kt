package com.benjamin.moviehub.domain.repository

import androidx.paging.PagingData
import com.benjamin.moviehub.domain.model.DiscoverFilters
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.model.MovieCategory
import com.benjamin.moviehub.domain.model.MovieCredits
import com.benjamin.moviehub.domain.model.MovieGenre
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing movies data
 */
interface MovieRepository {
    /**
     * Get paged movies from the API, a home [category] by default or through [query].
     *
     * A non-blank [query] takes precedence and returns search results regardless of [category].
     */
    fun getPagedMovies(
        query: String? = null,
        category: MovieCategory = MovieCategory.POPULAR,
    ): Flow<PagingData<Movie>>

    /** Get a network-backed paged feed for the currently applied Discover filters. */
    fun getDiscoverMovies(filters: DiscoverFilters): Flow<PagingData<Movie>>

    /** Get TMDB's localized movie genres. */
    suspend fun getMovieGenres(): List<MovieGenre>

    /** First movie of a home [category], used by the hero banner. Emits null until it is cached. */
    fun getHeroMovie(category: MovieCategory = MovieCategory.POPULAR): Flow<Movie?>

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

    /** Get movie credits from the API. Throws on network error; callers show details without credits. */
    suspend fun getMovieCredits(movieId: Int): MovieCredits

    /** Get movie recommendations from the API. Throws on network error; callers hide the section. */
    suspend fun getMovieRecommendations(movieId: Int): List<Movie>
}
