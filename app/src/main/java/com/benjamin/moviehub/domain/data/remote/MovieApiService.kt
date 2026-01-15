package com.benjamin.moviehub.domain.data.remote

import com.benjamin.moviehub.domain.data.MovieDto
import com.benjamin.moviehub.domain.data.MovieResponse
import com.benjamin.moviehub.domain.model.Movie
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MovieApiService {

    @GET("movie/popular")
    suspend fun getPopularMovies(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "fr-FR",
        @Query("page") page: Int = 1
    ): MovieResponse

    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "fr-FR"
    ): MovieDto
}