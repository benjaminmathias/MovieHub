package com.benjamin.moviehub.data.repository

import com.benjamin.moviehub.data.local.GenreEntity
import com.benjamin.moviehub.data.local.MovieDao
import com.benjamin.moviehub.data.local.MovieEntity
import com.benjamin.moviehub.domain.model.LibraryFlag
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
    private val baseMovie =
        Movie(
            id = 7,
            title = "Base movie",
            overview = "Overview",
            posterPath = "https://image.tmdb.org/t/p/w500/poster.jpg",
            backdropPath = "https://image.tmdb.org/t/p/w780/backdrop.jpg",
            voteAverage = 7.0,
            releaseDate = "2024-01-01",
            webUrl = "https://www.themoviedb.org/movie/7",
            isFavorite = true,
            isWatchlist = true,
            isWatched = false,
            genreIds = persistentListOf(18),
            genres = persistentListOf("Drame"),
            runtimeMinutes = 120,
        )

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
    fun `set flag writes relative image paths and keeps toggleFavorite working`() =
        runTest {
            val dao = mockk<MovieDao>()
            val entitySlot = slot<MovieEntity>()
            val repository = LibraryRepositoryImpl(dao)
            val movie = baseMovie.copy(isFavorite = false, isWatchlist = false)

            coEvery { dao.setLibraryFlag(capture(entitySlot), isFavorite = true) } just runs
            coEvery { dao.toggleFavorite(any()) } just runs

            repository.setLibraryFlag(movie, LibraryFlag.FAVORITE, true)
            repository.toggleFavorite(movie)

            coVerify(exactly = 1) { dao.setLibraryFlag(any(), isFavorite = true) }
            coVerify(exactly = 1) { dao.toggleFavorite(any()) }
            assertEquals(true, entitySlot.captured.isFavorite)
            assertEquals("/poster.jpg", entitySlot.captured.posterPath)
            assertEquals("/backdrop.jpg", entitySlot.captured.backdropPath)
        }

    @Test
    fun `favorite true routes the favorite value and preserves other flags`() =
        runTest {
            val dao = mockk<MovieDao>()
            val entitySlot = slot<MovieEntity>()
            coEvery { dao.setLibraryFlag(capture(entitySlot), isFavorite = true) } just runs

            LibraryRepositoryImpl(dao).setLibraryFlag(baseMovie.copy(isFavorite = false), LibraryFlag.FAVORITE, true)

            coVerify(exactly = 1) { dao.setLibraryFlag(any(), isFavorite = true) }
            val entity = entitySlot.captured
            assertEquals(true, entity.isFavorite)
            assertEquals(true, entity.isWatchlist)
            assertEquals(false, entity.isWatched)
            assertPreservedFields(entity)
        }

    @Test
    fun `favorite false routes the favorite value and preserves other flags`() =
        runTest {
            val dao = mockk<MovieDao>()
            val entitySlot = slot<MovieEntity>()
            coEvery { dao.setLibraryFlag(capture(entitySlot), isFavorite = false) } just runs

            LibraryRepositoryImpl(dao).setLibraryFlag(baseMovie, LibraryFlag.FAVORITE, false)

            coVerify(exactly = 1) { dao.setLibraryFlag(any(), isFavorite = false) }
            val entity = entitySlot.captured
            assertEquals(false, entity.isFavorite)
            assertEquals(true, entity.isWatchlist)
            assertEquals(false, entity.isWatched)
            assertPreservedFields(entity)
        }

    @Test
    fun `watchlist true routes the watchlist value and preserves other flags`() =
        runTest {
            val dao = mockk<MovieDao>()
            val entitySlot = slot<MovieEntity>()
            coEvery { dao.setLibraryFlag(capture(entitySlot), isWatchlist = true) } just runs

            LibraryRepositoryImpl(dao).setLibraryFlag(baseMovie.copy(isWatchlist = false), LibraryFlag.WATCHLIST, true)

            coVerify(exactly = 1) { dao.setLibraryFlag(any(), isWatchlist = true) }
            val entity = entitySlot.captured
            assertEquals(true, entity.isFavorite)
            assertEquals(true, entity.isWatchlist)
            assertEquals(false, entity.isWatched)
            assertPreservedFields(entity)
        }

    @Test
    fun `watchlist false routes the watchlist value and preserves other flags`() =
        runTest {
            val dao = mockk<MovieDao>()
            val entitySlot = slot<MovieEntity>()
            coEvery { dao.setLibraryFlag(capture(entitySlot), isWatchlist = false) } just runs

            LibraryRepositoryImpl(dao).setLibraryFlag(baseMovie, LibraryFlag.WATCHLIST, false)

            coVerify(exactly = 1) { dao.setLibraryFlag(any(), isWatchlist = false) }
            val entity = entitySlot.captured
            assertEquals(true, entity.isFavorite)
            assertEquals(false, entity.isWatchlist)
            assertEquals(false, entity.isWatched)
            assertPreservedFields(entity)
        }

    @Test
    fun `watched true routes the watched value and preserves other flags`() =
        runTest {
            val dao = mockk<MovieDao>()
            val entitySlot = slot<MovieEntity>()
            coEvery { dao.setLibraryFlag(capture(entitySlot), isWatched = true) } just runs

            LibraryRepositoryImpl(dao).setLibraryFlag(baseMovie, LibraryFlag.WATCHED, true)

            coVerify(exactly = 1) { dao.setLibraryFlag(any(), isWatched = true) }
            val entity = entitySlot.captured
            assertEquals(true, entity.isFavorite)
            assertEquals(true, entity.isWatchlist)
            assertEquals(true, entity.isWatched)
            assertPreservedFields(entity)
        }

    @Test
    fun `watched false routes the watched value and preserves other flags`() =
        runTest {
            val dao = mockk<MovieDao>()
            val entitySlot = slot<MovieEntity>()
            coEvery { dao.setLibraryFlag(capture(entitySlot), isWatched = false) } just runs

            LibraryRepositoryImpl(dao).setLibraryFlag(baseMovie.copy(isWatched = true), LibraryFlag.WATCHED, false)

            coVerify(exactly = 1) { dao.setLibraryFlag(any(), isWatched = false) }
            val entity = entitySlot.captured
            assertEquals(true, entity.isFavorite)
            assertEquals(true, entity.isWatchlist)
            assertEquals(false, entity.isWatched)
            assertPreservedFields(entity)
        }

    private fun assertPreservedFields(entity: MovieEntity) {
        assertEquals(baseMovie.id, entity.id)
        assertEquals("Base movie", entity.title)
        assertEquals("Overview", entity.overview)
        assertEquals(7.0, entity.voteAverage, 0.0)
        assertEquals("2024-01-01", entity.releaseDate)
        assertEquals(listOf(18), entity.genreIds)
        assertEquals(120, entity.runtimeMinutes)
        assertEquals("/poster.jpg", entity.posterPath)
        assertEquals("/backdrop.jpg", entity.backdropPath)
    }
}
