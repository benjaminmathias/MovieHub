package com.benjamin.moviehub.data.local

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.benjamin.moviehub.domain.model.MovieCategory
import com.benjamin.moviehub.inMemoryDatabase
import com.benjamin.moviehub.movieEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MovieDaoTest {
    private lateinit var database: MovieDatabase

    @Before
    fun setUp() {
        database = inMemoryDatabase()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun remoteKey_isStoredIndependentlyPerFeed() =
        runBlocking {
            val dao = database.movieDao()

            dao.upsertRemoteKey(RemoteKey(MovieCategory.POPULAR.key, nextKey = 2))
            dao.upsertRemoteKey(RemoteKey(MovieCategory.UPCOMING.key, nextKey = null))

            assertEquals(2, dao.getRemoteKey(MovieCategory.POPULAR.key)?.nextKey)
            assertEquals(null, dao.getRemoteKey(MovieCategory.UPCOMING.key)?.nextKey)
            assertEquals(null, dao.getRemoteKey("SEARCH:test"))

            dao.upsertRemoteKey(RemoteKey(MovieCategory.POPULAR.key, nextKey = 5))
            assertEquals(5, dao.getRemoteKey(MovieCategory.POPULAR.key)?.nextKey)
        }

    @Test
    fun toggleFavorite_usesPersistedValueForRapidActions() =
        runBlocking {
            val dao = database.movieDao()
            val staleMovie = movieEntity(id = 44)

            dao.toggleFavorite(staleMovie)
            dao.toggleFavorite(staleMovie)

            assertEquals(false, dao.getMovieById(44)?.isFavorite)
        }

    @Test
    fun libraryFlags_areIndependent() =
        runBlocking {
            val dao = database.movieDao()
            val movie = movieEntity(id = 43)
            dao.setLibraryFlag(movie, isFavorite = true)
            dao.setLibraryFlag(movie, isWatchlist = true)
            assertTrue(dao.getMovieById(43)?.isWatchlist == true)
            assertTrue(dao.getMovieById(43)?.isWatched == false)
            dao.setLibraryFlag(movie, isWatched = true)
            assertTrue(dao.getMovieById(43)?.let { it.isFavorite && !it.isWatchlist && it.isWatched } == true)
            dao.setLibraryFlag(movie, isWatchlist = false)
            assertTrue(dao.getMovieById(43)?.let { it.isFavorite && !it.isWatchlist && it.isWatched } == true)
            dao.setLibraryFlag(movie, isWatched = false)
            assertTrue(dao.getMovieById(43)?.let { it.isFavorite && !it.isWatchlist && !it.isWatched } == true)
        }

    @Test
    fun upsertMovieDetails_preservesAllLocalFlagsAndRuntime() =
        runBlocking {
            val dao = database.movieDao()
            dao.insertMovie(
                movieEntity(
                    id = 42,
                    isFavorite = true,
                    isWatchlist = true,
                    isWatched = true,
                    runtimeMinutes = 137,
                ),
            )

            val refreshed = movieEntity(id = 42).copy(title = "Refreshed", runtimeMinutes = null)

            val merged = dao.upsertMovieDetails(refreshed)

            assertEquals("Refreshed", merged.title)
            assertEquals(true, merged.isFavorite)
            assertEquals(false, merged.isWatchlist)
            assertEquals(true, merged.isWatched)
            assertEquals(137, merged.runtimeMinutes)
            assertEquals(merged, dao.getMovieById(42))
        }
}
