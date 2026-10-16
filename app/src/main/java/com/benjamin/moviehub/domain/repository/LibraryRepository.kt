package com.benjamin.moviehub.domain.repository

import com.benjamin.moviehub.domain.model.LibraryFlag
import com.benjamin.moviehub.domain.model.Movie
import kotlinx.coroutines.flow.Flow

interface LibraryRepository {
    suspend fun setLibraryFlag(
        movie: Movie,
        flag: LibraryFlag,
        value: Boolean,
    )

    /** Toggle a favorite from its current persisted state. */
    suspend fun toggleFavorite(movie: Movie)

    fun getLibraryMovies(): Flow<List<Movie>>
}
