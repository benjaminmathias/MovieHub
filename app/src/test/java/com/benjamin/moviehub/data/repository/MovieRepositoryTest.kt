package com.benjamin.moviehub.data.repository

import android.util.Log
import com.benjamin.moviehub.data.local.MovieDao
import com.benjamin.moviehub.data.local.MovieDatabase
import com.benjamin.moviehub.data.local.MovieEntity
import com.benjamin.moviehub.data.mapper.toDomain
import com.benjamin.moviehub.data.remote.ActorDto
import com.benjamin.moviehub.data.remote.CrewMemberDto
import com.benjamin.moviehub.data.remote.MovieCreditsDto
import com.benjamin.moviehub.data.remote.MovieApiService
import com.benjamin.moviehub.data.remote.MovieDto
import com.benjamin.moviehub.data.remote.MovieResponse
import androidx.room.withTransaction
import com.benjamin.moviehub.domain.model.Movie
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

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
            coEvery { apiService.getMovieDetails(9, any()) } throws IOException("offline")

            val result = repository.getMovieDetails(9)

            assertEquals(localMovie.toDomain(), result)
            coVerify(exactly = 0) { dao.insertMovie(any()) }
        }

    @Test
    fun `get movie credits maps cast and director`() =
        runBlocking {
            val apiService = mockk<MovieApiService>()
            val database = mockk<MovieDatabase>()
            val dao = mockk<MovieDao>()
            val repository = MovieRepositoryImpl(apiService, database, dao)

            coEvery { apiService.getMovieCredits(9, any()) } returns
                MovieCreditsDto(
                    cast = listOf(ActorDto(1, "Actor", "Role", null)),
                    crew = listOf(CrewMemberDto("Director", "Director")),
                )

            val result = repository.getMovieCredits(9).getOrThrow()

            assertEquals("Director", result.director)
            assertEquals("Actor", result.actors.single().name)
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

            coEvery { dao.setFavorite(capture(entitySlot), true) } just runs

            repository.toggleFavorite(movie, true)

            coVerify(exactly = 1) { dao.setFavorite(any(), true) }
            assertEquals(true, entitySlot.captured.isFavorite)
            assertEquals("/poster.jpg", entitySlot.captured.posterPath)
            assertEquals("/backdrop.jpg", entitySlot.captured.backdropPath)
            assertEquals(false, entitySlot.captured.isPopular)
            assertEquals(false, entitySlot.captured.isSearchResult)
            assertEquals(0, entitySlot.captured.pageOrder)
        }

    @Test
    fun `popular sync preserves cached runtime`() =
        runBlocking {
            val apiService = mockk<MovieApiService>()
            val database = mockk<MovieDatabase>()
            val dao = mockk<MovieDao>()
            val entities = slot<List<MovieEntity>>()
            val repository = MovieRepositoryImpl(apiService, database, dao)
            val dto = MovieDto(1, "Movie", "Overview", null, null, 7.0, runtimeMinutes = null)

            coEvery { apiService.getPopularMovies(apiKey = any(), page = 1) } returns MovieResponse(listOf(dto))
            coEvery { dao.getMoviesByIds(listOf(1)) } returns listOf(
                MovieEntity(1, "Movie", "Overview", null, null, 7.0, "", runtimeMinutes = 123),
            )
            coEvery { dao.clearRemoteKeysByType(any()) } just runs
            coEvery { dao.clearPopularMovies() } just runs
            coEvery { dao.insertAllKeys(any()) } just runs
            coEvery { dao.upsertMovies(capture(entities)) } just runs
            io.mockk.mockkStatic(Log::class)
            every { Log.e(any(), any(), any()) } returns 0
            io.mockk.mockkStatic("androidx.room.RoomDatabaseKt")
            io.mockk.mockkStatic("androidx.room.RoomDatabaseKt__RoomDatabase_androidKt")
            coEvery { database.withTransaction(any<suspend () -> Unit>()) } coAnswers {
                secondArg<suspend () -> Unit>().invoke()
            }

            repository.syncPopularMoviesCache()

            assertEquals(123, entities.captured.single().runtimeMinutes)
            io.mockk.unmockkStatic("androidx.room.RoomDatabaseKt")
            io.mockk.unmockkStatic("androidx.room.RoomDatabaseKt__RoomDatabase_androidKt")
            io.mockk.unmockkStatic(Log::class)
        }
}
