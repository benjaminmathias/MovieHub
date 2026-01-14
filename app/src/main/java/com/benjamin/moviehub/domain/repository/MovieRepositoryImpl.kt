package com.benjamin.moviehub.domain.repository

import com.benjamin.moviehub.BuildConfig
import com.benjamin.moviehub.domain.data.remote.MovieApiService
import com.benjamin.moviehub.domain.model.Movie
import javax.inject.Inject

class MovieRepositoryImpl @Inject constructor(
    private val apiService: MovieApiService
) : MovieRepository {
    override suspend fun getPopularMovies(): List<Movie> {

        println("DEBUG_API: Ma clé est [${BuildConfig.TMDB_API_KEY}]")
        val response = apiService.getPopularMovies(apiKey = BuildConfig.TMDB_API_KEY)
        return response.movies.map { dto ->
            Movie(
                id = dto.id,
                title = dto.title,
                overview = dto.description,
                posterPath = "https://image.tmdb.org/t/p/w500${dto.posterPath}",
                voteAverage = dto.rating,
                releaseDate = ""
            )
        }
    }
}