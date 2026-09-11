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
    /** Get a paged feed for a home [category]. */
    fun getCategoryMovies(category: MovieCategory): Flow<PagingData<Movie>>

    /** Get paged search results for [query]. */
    fun searchMovies(query: String): Flow<PagingData<Movie>>

    /** Get a network-backed paged feed for the currently applied Discover filters. */
    fun getDiscoverMovies(filters: DiscoverFilters): Flow<PagingData<Movie>>

    /** Get TMDB's localized movie genres. */
    suspend fun getMovieGenres(): List<MovieGenre>

    /** First movie of a home [category], used by the hero banner. Emits null until it is cached. */
    fun getHeroMovie(category: MovieCategory = MovieCategory.POPULAR): Flow<Movie?>

    /**
     * Fetch the movie from the API, upsert it locally (preserving local favorite and runtime),
     * and fall back to the cached row when the network is unavailable.
     */
    suspend fun getMovieDetails(movieId: Int): Movie

    /** Persist the favorite status of [movie] in the db. */
    suspend fun toggleFavorite(
        movie: Movie,
        isFavorite: Boolean,
    )

    /** Get favorite movies from the db. */
    fun getFavoriteMovies(): Flow<List<Movie>>

    /** Observe the local favorite IDs used to enrich network-backed feeds. */
    fun getFavoriteMovieIds(): Flow<Set<Int>>

    /** Get movie credits from the API. Throws on network error; callers show details without credits. */
    suspend fun getMovieCredits(movieId: Int): MovieCredits

    /** Get movie recommendations from the API. Throws on network error; callers hide the section. */
    suspend fun getMovieRecommendations(movieId: Int): List<Movie>
}
