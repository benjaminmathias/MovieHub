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

    /** Fetch the movie from the API, preserving local library flags and runtime when cached. */
    suspend fun getMovieDetails(movieId: Int): Movie

    suspend fun setFavorite(
        movie: Movie,
        isFavorite: Boolean,
    )

    suspend fun setWatchlist(
        movie: Movie,
        isWatchlist: Boolean,
    )

    suspend fun setWatched(
        movie: Movie,
        isWatched: Boolean,
    )

    fun getLibraryMovies(): Flow<List<Movie>>

    /** Get movie credits from the API. Throws on network error; callers show details without credits. */
    suspend fun getMovieCredits(movieId: Int): MovieCredits

    /** Get movie recommendations from the API. Throws on network error; callers hide the section. */
    suspend fun getMovieRecommendations(movieId: Int): List<Movie>
}
