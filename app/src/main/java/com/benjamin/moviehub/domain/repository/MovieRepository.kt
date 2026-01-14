package com.benjamin.moviehub.domain.repository

import com.benjamin.moviehub.domain.model.Movie

interface MovieRepository {
    suspend fun getPopularMovies() : List<Movie>
}