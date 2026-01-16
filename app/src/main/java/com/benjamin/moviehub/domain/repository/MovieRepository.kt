package com.benjamin.moviehub.domain.repository

import com.benjamin.moviehub.domain.model.Movie

interface MovieRepository {
    suspend fun getPopularMovies() : List<Movie>

    suspend fun getMovieDetails(movieId : Int): Movie

    suspend fun getSearchedMovies(query : String) : List<Movie>
}