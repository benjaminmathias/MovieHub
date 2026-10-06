package com.benjamin.moviehub.data.repository

import com.benjamin.moviehub.data.local.GenreEntity
import com.benjamin.moviehub.data.local.MovieDao
import com.benjamin.moviehub.data.local.MovieDatabase
import com.benjamin.moviehub.data.local.MovieEntity
import com.benjamin.moviehub.data.mapper.toDomain
import com.benjamin.moviehub.data.remote.MovieApiService
import com.benjamin.moviehub.data.remote.MovieDto
import com.benjamin.moviehub.data.remote.MovieResponse
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.model.MovieCategory
import com.benjamin.moviehub.domain.model.MovieGenre
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class MovieRepositoryTest {
    @Test
    fun `get movie details falls back to local movie when api fails`() =
        runTest {
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
            coEvery { dao.getGenres() } returns emptyList()
            coEvery { apiService.getMovieDetails(9) } throws IOException("offline")

            val result = repository.getMovieDetails(9)

            assertEquals(localMovie.toDomain(), result)
            coVerify(exactly = 0) { dao.insertMovie(any()) }
        }

    @Test
    fun `set favorite inserts missing movie with relative image paths`() =
        runTest {
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

    @Test
    fun `get movie recommendations maps remote dtos to domain`() =
        runTest {
            val apiService = mockk<MovieApiService>()
            val database = mockk<MovieDatabase>()
            val dao = mockk<MovieDao>()
            val repository = MovieRepositoryImpl(apiService, database, dao)
            coEvery { apiService.getMovieRecommendations(9) } returns
                MovieResponse(
                    movies =
                        listOf(
                            MovieDto(
                                id = 9,
                                title = "Recommended",
                                description = "Overview",
                                posterPath = "/poster.jpg",
                                backdropPath = "/backdrop.jpg",
                                voteAverage = 7.5,
                                releaseDate = "2021-01-01",
                            ),
                        ),
                )
            coEvery { dao.getMoviesByIds(any()) } returns
                listOf(
                    MovieEntity(
                        id = 9,
                        title = "Cached",
                        overview = "",
                        posterPath = null,
                        backdropPath = null,
                        voteAverage = 1.0,
                        releaseDate = "",
                        isFavorite = true,
                        isWatchlist = true,
                        isWatched = true,
                    ),
                )
            coEvery { dao.getGenres() } returns emptyList()

            val result = repository.getMovieRecommendations(9)

            assertEquals(1, result.size)
            assertEquals(9, result.single().id)
            assertEquals("https://image.tmdb.org/t/p/w500/poster.jpg", result.single().posterPath)
            assertEquals("https://www.themoviedb.org/movie/9", result.single().webUrl)
            assertEquals(true, result.single().isFavorite)
            assertEquals(true, result.single().isWatchlist)
            assertEquals(true, result.single().isWatched)
            coVerify(exactly = 0) { dao.insertMovie(any()) }
        }

    @Test
    fun `get hero movie maps the first cached category movie`() =
        runTest {
            val apiService = mockk<MovieApiService>()
            val database = mockk<MovieDatabase>()
            val dao = mockk<MovieDao>()
            val repository = MovieRepositoryImpl(apiService, database, dao)
            val entity =
                MovieEntity(
                    id = 5,
                    title = "Hero",
                    overview = "Overview",
                    posterPath = "/poster.jpg",
                    backdropPath = "/backdrop.jpg",
                    voteAverage = 8.0,
                    releaseDate = "2024-01-01",
                )
            coEvery { dao.getHeroMovieFlow(MovieCategory.POPULAR.key) } returns flowOf(entity)
            coEvery { dao.getGenres() } returns emptyList()

            val result = repository.getHeroMovie(MovieCategory.POPULAR).first()

            assertEquals(5, result?.id)
            assertEquals("https://image.tmdb.org/t/p/w500/poster.jpg", result?.posterPath)
        }

    @Test
    fun `get movie genres falls back to the cached dictionary on a recoverable failure`() =
        runTest {
            val apiService = mockk<MovieApiService>()
            val database = mockk<MovieDatabase>()
            val dao = mockk<MovieDao>()
            val repository = MovieRepositoryImpl(apiService, database, dao)
            coEvery { apiService.getMovieGenres() } throws IOException("offline")
            coEvery { dao.getGenres() } returns
                listOf(
                    GenreEntity(id = 18, name = "Drame"),
                    GenreEntity(id = 28, name = "Action"),
                )

            val result = repository.getMovieGenres()

            assertEquals(
                listOf(MovieGenre(id = 18, name = "Drame"), MovieGenre(id = 28, name = "Action")),
                result,
            )
            coVerify(exactly = 0) { dao.upsertGenres(any()) }
        }

    @Test
    fun `get movie genres falls back to the cache on a 5xx failure`() =
        runTest {
            val apiService = mockk<MovieApiService>()
            val database = mockk<MovieDatabase>()
            val dao = mockk<MovieDao>()
            val repository = MovieRepositoryImpl(apiService, database, dao)
            coEvery { apiService.getMovieGenres() } throws
                HttpException(Response.error<Any>(503, "".toResponseBody()))
            coEvery { dao.getGenres() } returns listOf(GenreEntity(id = 28, name = "Action"))

            val result = repository.getMovieGenres()

            assertEquals(listOf(MovieGenre(id = 28, name = "Action")), result)
        }

    @Test
    fun `get movie genres keeps only valid cached names on a recoverable failure`() =
        runTest {
            val apiService = mockk<MovieApiService>()
            val database = mockk<MovieDatabase>()
            val dao = mockk<MovieDao>()
            val repository = MovieRepositoryImpl(apiService, database, dao)
            coEvery { apiService.getMovieGenres() } throws IOException("offline")
            coEvery { dao.getGenres() } returns
                listOf(
                    GenreEntity(id = 18, name = "Drame"),
                    GenreEntity(id = 99, name = "   "),
                )

            val result = repository.getMovieGenres()

            assertEquals(listOf(MovieGenre(id = 18, name = "Drame")), result)
        }

    @Test
    fun `get movie genres rethrows the original failure when the cache is empty`() =
        runTest {
            val apiService = mockk<MovieApiService>()
            val database = mockk<MovieDatabase>()
            val dao = mockk<MovieDao>()
            val repository = MovieRepositoryImpl(apiService, database, dao)
            val failure = IOException("offline")
            coEvery { apiService.getMovieGenres() } throws failure
            coEvery { dao.getGenres() } returns emptyList()

            val thrown =
                try {
                    repository.getMovieGenres()
                    null
                } catch (e: Exception) {
                    e
                }

            assertSame(failure, thrown)
        }

    @Test
    fun `get movie genres rethrows the original failure when every cached name is blank`() =
        runTest {
            val apiService = mockk<MovieApiService>()
            val database = mockk<MovieDatabase>()
            val dao = mockk<MovieDao>()
            val repository = MovieRepositoryImpl(apiService, database, dao)
            val failure = IOException("offline")
            coEvery { apiService.getMovieGenres() } throws failure
            coEvery { dao.getGenres() } returns listOf(GenreEntity(id = 99, name = "  "))

            val thrown =
                try {
                    repository.getMovieGenres()
                    null
                } catch (e: Exception) {
                    e
                }

            assertSame(failure, thrown)
        }

    @Test
    fun `get movie genres propagates a non recoverable failure without reading the cache`() =
        runTest {
            val apiService = mockk<MovieApiService>()
            val database = mockk<MovieDatabase>()
            val dao = mockk<MovieDao>()
            val repository = MovieRepositoryImpl(apiService, database, dao)
            val failure = HttpException(Response.error<Any>(404, "".toResponseBody()))
            coEvery { apiService.getMovieGenres() } throws failure

            val thrown =
                try {
                    repository.getMovieGenres()
                    null
                } catch (e: Exception) {
                    e
                }

            assertSame(failure, thrown)
            coVerify(exactly = 0) { dao.getGenres() }
        }

    @Test
    fun `get movie genres propagates cancellation without reading the cache`() =
        runTest {
            val apiService = mockk<MovieApiService>()
            val database = mockk<MovieDatabase>()
            val dao = mockk<MovieDao>()
            val repository = MovieRepositoryImpl(apiService, database, dao)
            val failure = CancellationException("cancelled")
            coEvery { apiService.getMovieGenres() } throws failure

            val thrown =
                try {
                    repository.getMovieGenres()
                    null
                } catch (e: Exception) {
                    e
                }

            assertSame(failure, thrown)
            coVerify(exactly = 0) { dao.getGenres() }
        }
}
