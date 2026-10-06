package com.benjamin.moviehub.data.repository

import com.benjamin.moviehub.data.local.GenreEntity
import com.benjamin.moviehub.data.local.MovieDao
import com.benjamin.moviehub.data.local.MovieEntity
import com.benjamin.moviehub.domain.model.Movie
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryRepositoryTest {
    @Test
    fun `library maps local genres and flags and emits removal`() =
        runTest {
            val dao = mockk<MovieDao>()
            val localMovie =
                MovieEntity(
                    id = 9,
                    title = "Saved movie",
                    overview = "Overview",
                    posterPath = "/poster.jpg",
                    backdropPath = null,
                    voteAverage = 7.5,
                    releaseDate = "2024-01-01",
                    genreIds = listOf(18),
                    isFavorite = true,
                    isWatchlist = true,
                    runtimeMinutes = 120,
                )
            every { dao.getLibraryMoviesFlow() } returns flowOf(listOf(localMovie), emptyList())
            coEvery { dao.getGenres() } returns listOf(GenreEntity(id = 18, name = "Drame"))

            val snapshots = LibraryRepositoryImpl(dao).getLibraryMovies().toList()
            val movie = snapshots.first().single()

            assertEquals(listOf("Drame"), movie.genres)
            assertEquals(true, movie.isFavorite)
            assertEquals(true, movie.isWatchlist)
            assertEquals(false, movie.isWatched)
            assertEquals(120, movie.runtimeMinutes)
            assertEquals(emptyList<Movie>(), snapshots.last())
        }

    @Test
    fun `set favorite inserts missing movie with relative image paths`() =
        runTest {
            val dao = mockk<MovieDao>()
            val entitySlot = slot<MovieEntity>()
            val repository = LibraryRepositoryImpl(dao)
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
                    genreIds = persistentListOf(),
                    genres = persistentListOf(),
                )

            coEvery { dao.setLibraryFlag(capture(entitySlot), isFavorite = true) } just runs
            coEvery { dao.toggleFavorite(any()) } just runs

            repository.setFavorite(movie, true)
            repository.toggleFavorite(movie)

            coVerify(exactly = 1) { dao.setLibraryFlag(any(), isFavorite = true) }
            coVerify(exactly = 1) { dao.toggleFavorite(any()) }
            assertEquals(true, entitySlot.captured.isFavorite)
            assertEquals("/poster.jpg", entitySlot.captured.posterPath)
            assertEquals("/backdrop.jpg", entitySlot.captured.backdropPath)
        }
}
