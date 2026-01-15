package com.benjamin.moviehub.domain.repository

import android.util.Log
import com.benjamin.moviehub.BuildConfig
import com.benjamin.moviehub.domain.data.remote.MovieApiService
import com.benjamin.moviehub.domain.model.Movie
import javax.inject.Inject

class MovieRepositoryImpl @Inject constructor(
    private val apiService: MovieApiService
) : MovieRepository {
    override suspend fun getPopularMovies(): List<Movie> {

        val response = apiService.getPopularMovies(apiKey = BuildConfig.TMDB_API_KEY)
        return response.movies.map { dto ->
            Movie(
                id = dto.id,
                title = dto.title,
                overview = dto.description,
                posterPath = "https://image.tmdb.org/t/p/w500${dto.posterPath}",
                backdropPath = "https://image.tmdb.org/t/p/w780${dto.backdropPath}",
                voteAverage = dto.voteAverage,
                releaseDate = ""
            )
        }
    }

    override suspend fun getMovieDetails(movieId: Int): Movie {
        val dto = apiService.getMovieDetails(
            movieId = movieId,
            apiKey = BuildConfig.TMDB_API_KEY,
        )

        Log.d("Movie :", dto.voteAverage.toString())

        return Movie(
            id = dto.id,
            title = dto.title,
            overview = dto.description,
            posterPath = "https://image.tmdb.org/t/p/w500${dto.posterPath}",
            backdropPath = "https://image.tmdb.org/t/p/w780${dto.backdropPath}",
            voteAverage = dto.voteAverage,
            releaseDate = ""
        )
    }
}