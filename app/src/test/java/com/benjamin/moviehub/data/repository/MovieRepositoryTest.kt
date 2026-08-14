package com.benjamin.moviehub.data.repository

import com.benjamin.moviehub.data.local.MovieDao
import com.benjamin.moviehub.data.local.MovieDatabase
import com.benjamin.moviehub.data.local.MovieEntity
import com.benjamin.moviehub.data.mapper.toDomain
import com.benjamin.moviehub.data.remote.MovieApiService
import com.benjamin.moviehub.domain.model.Movie
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class MovieRepositoryTest {
    @Test
    fun `get movie details falls back to local movie when api fails`() =
        runBlocking {
            val apiService = mockk<MovieApiService>()
            val database = mockk<MovieDatabase>()
            val dao = mockk<MovieDao>()
            val repository = MovieRepositoryImpl(apiService, database, dao)
            val localMovie =
                MovieEntity(
                    id = 9,
                    title = "Cached",
                    overview = "Local overview",
                    posterPath = "/poster.jpg",
                    backdropPath = "/backdrop.jpg",
                    voteAverage = 7.5,
                    releaseDate = "2024-01-01",
                    genreIds = listOf(18),
                    isFavorite = true,
                )

            coEvery { dao.getMovieById(9) } returns localMovie
            coEvery { apiService.getMovieDetails(9, any()) } throws IllegalStateException("offline")

            val result = repository.getMovieDetails(9)

            assertEquals(localMovie.toDomain(), result)
            coVerify(exactly = 0) { dao.insertMovie(any()) }
        }

    @Test
    fun `toggle favorite inserts missing movie with relative image paths`() =
        runBlocking {
            val apiService = mockk<MovieApiService>()
            val database = mockk<MovieDatabase>()
            val dao = mockk<MovieDao>()
            val entitySlot = slot<MovieEntity>()
            val repository = MovieRepositoryImpl(apiService, database, dao)
            val movie =
                Movie(
                    id = 7,
                    title = "Missing Movie",
                    overview = "Overview",
                    posterPath = "https://image.tmdb.org/t/p/w500/poster.jpg",
                    backdropPath = "https://image.tmdb.org/t/p/w780/backdrop.jpg",
                    voteAverage = 7.0,
                    releaseDate = "2024-01-01",
                    webUrl = "https://www.themoviedb.org/movie/7",
                    isFavorite = false,
                    genreIds = emptyList(),
                    genres = emptyList(),
                )

            coEvery { dao.getMovieById(7) } returns null
            coEvery { dao.insertMovie(capture(entitySlot)) } just runs

            repository.toggleFavorite(movie, true)

            coVerify(exactly = 1) { dao.insertMovie(any()) }
            assertEquals(true, entitySlot.captured.isFavorite)
            assertEquals("/poster.jpg", entitySlot.captured.posterPath)
            assertEquals("/backdrop.jpg", entitySlot.captured.backdropPath)
            assertEquals(false, entitySlot.captured.isPopular)
            assertEquals(false, entitySlot.captured.isSearchResult)
            assertEquals(0, entitySlot.captured.pageOrder)
        }
}
