package com.benjamin.moviehub.domain.repository

import com.benjamin.moviehub.domain.model.LibraryFlag
import com.benjamin.moviehub.domain.model.Movie
import kotlinx.coroutines.flow.Flow

/** Repository contract for the persisted personal movie library. */
interface LibraryRepository {
    suspend fun setFavorite(
        movie: Movie,
        isFavorite: Boolean,
    )

    /** Toggle a favorite from its current persisted state. */
    suspend fun toggleFavorite(movie: Movie)

    suspend fun setWatchlist(
        movie: Movie,
        isWatchlist: Boolean,
    )

    suspend fun setWatched(
        movie: Movie,
        isWatched: Boolean,
    )

    fun getLibraryMovies(): Flow<List<Movie>>
}

/** Persists [flag] for [movie] through the matching repository setter. */
suspend fun LibraryRepository.setLibraryFlag(
    movie: Movie,
    flag: LibraryFlag,
    value: Boolean,
) = when (flag) {
    LibraryFlag.FAVORITE -> setFavorite(movie, value)
    LibraryFlag.WATCHLIST -> setWatchlist(movie, value)
    LibraryFlag.WATCHED -> setWatched(movie, value)
}
