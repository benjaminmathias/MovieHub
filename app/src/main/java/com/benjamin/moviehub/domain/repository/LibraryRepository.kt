package com.benjamin.moviehub.domain.repository

import com.benjamin.moviehub.domain.model.LibraryFlag
import com.benjamin.moviehub.domain.model.Movie
import kotlinx.coroutines.flow.Flow

interface LibraryRepository {
    /**
     * Persist one flag, inserting the movie if absent. Other flags use the persisted state.
     * Setting watchlist or watched to true clears the other flag.
     */
    suspend fun setLibraryFlag(
        movie: Movie,
        flag: LibraryFlag,
        value: Boolean,
    )

    /** Toggle a favorite from its current persisted state. */
    suspend fun toggleFavorite(movie: Movie)

    /** Observe locally saved favorites, watchlist and watched movies, ordered by title then ID. */
    fun getLibraryMovies(): Flow<List<Movie>>
}
