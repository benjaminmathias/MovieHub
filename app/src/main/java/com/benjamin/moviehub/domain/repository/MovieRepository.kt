package com.benjamin.moviehub.domain.repository

import androidx.paging.PagingData
import com.benjamin.moviehub.domain.model.DiscoverFilters
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.model.MovieCategory
import com.benjamin.moviehub.domain.model.MovieCredits
import com.benjamin.moviehub.domain.model.MovieGenre
import kotlinx.coroutines.flow.Flow

/**
 * Repository contract for movie data.
 *
 * Paged feeds are exposed as [androidx.paging.PagingData], which deliberately ties this
 * domain contract to AndroidX Paging. Accepted tradeoff for this app: paging is the
 * repository boundary itself, and a domain wrapper would add indirection without a
 * second consumer. Revisit if the domain ever needs to be framework-free.
 */
interface MovieRepository {
    /** Observe cached category pages while refreshing them from the API. Failed refreshes retain cached rows. */
    fun getCategoryMovies(category: MovieCategory): Flow<PagingData<Movie>>

    /** Observe cached results for the normalized query while refreshing them from the API. */
    fun searchMovies(query: String): Flow<PagingData<Movie>>

    /** Load API pages for the applied filters, enriched with local library flags. Pages are not persisted. */
    fun getDiscoverMovies(filters: DiscoverFilters): Flow<PagingData<Movie>>

    /**
     * Fetch and cache TMDB's localized genres. On IO failures or HTTP 408, 429 or 5xx,
     * return usable cached genres. Rethrow the failure when no usable cache exists.
     */
    suspend fun getMovieGenres(): List<MovieGenre>

    /** Observe the first cached movie of a home [category]. Emits null when the category has no cached movie. */
    fun getHeroMovie(category: MovieCategory = MovieCategory.POPULAR): Flow<Movie?>

    /**
     * Fetch and cache API details, preserving local library flags and cached runtime when omitted by the API.
     * On IO failures or HTTP 408, 429 or 5xx, return the cached movie if present; otherwise rethrow.
     */
    suspend fun getMovieDetails(movieId: Int): Movie

    /** Fetch credits from the API without caching. Failures propagate to the caller. */
    suspend fun getMovieCredits(movieId: Int): MovieCredits

    /** Fetch API recommendations with local library flags, without caching the results. Failures propagate. */
    suspend fun getMovieRecommendations(movieId: Int): List<Movie>
}
