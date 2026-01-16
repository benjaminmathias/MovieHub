package com.benjamin.moviehub.domain.repository

import android.util.Log
import com.benjamin.moviehub.BuildConfig
import com.benjamin.moviehub.domain.data.remote.MovieApiService
import com.benjamin.moviehub.domain.data.toDomain
import com.benjamin.moviehub.domain.model.Movie
import javax.inject.Inject

class MovieRepositoryImpl @Inject constructor(
    private val apiService: MovieApiService
) : MovieRepository {

    override suspend fun getPopularMovies(): List<Movie> {
        val response = apiService.getPopularMovies(apiKey = BuildConfig.TMDB_API_KEY)
        return response.movies.map { it.toDomain() }
    }

    override suspend fun getMovieDetails(movieId: Int): Movie {
        val dto = apiService.getMovieDetails(
            movieId = movieId,
            apiKey = BuildConfig.TMDB_API_KEY,
        )
        return dto.toDomain()
    }

    override suspend fun getSearchedMovies(query: String): List<Movie> {
        val query = apiService.searchMovies(
            apiKey = BuildConfig.TMDB_API_KEY,
            query = query
        )
        return query.movies.map { it.toDomain() }
    }
}