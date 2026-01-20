package com.benjamin.moviehub.domain.repository

import com.benjamin.moviehub.domain.model.Movie
import kotlinx.coroutines.flow.Flow

interface MovieRepository {
    fun getPopularMovies() : Flow<List<Movie>>

    suspend fun getMovieDetails(movieId : Int): Movie

    suspend fun getSearchedMovies(query : String) : List<Movie>

    suspend fun toggleFavorite(movieId: Int, isFavorite: Boolean)
}