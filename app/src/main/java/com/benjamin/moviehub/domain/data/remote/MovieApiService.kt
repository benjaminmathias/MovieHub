package com.benjamin.moviehub.domain.data.remote

import com.benjamin.moviehub.domain.data.MovieResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface MovieApiService {

    @GET("movie/popular")
    suspend fun getPopularMovies(
        @Query("api_key") apiKey: String,
        @Query("language") language : String = "fr-FR",
        @Query("page") page : Int = 1
    ) : MovieResponse
}