package com.benjamin.moviehub.domain.model

object MovieMocks {
    val dummyMovies = listOf(
        Movie(
            id = 1,
            title = "Inception",
            overview = "A thief enters the dreams of others to steal their secrets.",
            posterPath = "/4Y5ZXYnWBIV8Vpe8hcA0LH6hC80.jpg",
            voteAverage = 8.8,
            releaseDate = "2010-07-16"
        ),
        Movie(
            id = 2,
            title = "The Shawshank Redemption",
            overview = "Two imprisoned men bond over several years, finding solace and eventual redemption through acts of common decency",
            posterPath = "/4Y5ZXYnWBIV8Vpe8hcA0LH6hC80.jpg",
            voteAverage = 9.3,
            releaseDate = "1994-09-23"
        )
    )

}