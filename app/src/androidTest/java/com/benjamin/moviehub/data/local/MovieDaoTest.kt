package com.benjamin.moviehub.data.local

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.benjamin.moviehub.domain.model.LibraryFlag
import com.benjamin.moviehub.domain.model.MovieCategory
import com.benjamin.moviehub.inMemoryDatabase
import com.benjamin.moviehub.insertMalformedMovieForTest
import com.benjamin.moviehub.movieEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
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
    fun setLibraryFlag_handlesEveryFlagForMissingAndExistingMovies() =
        runBlocking {
            val dao = database.movieDao()
            val staleInput = movieEntity(id = -1, isFavorite = false, isWatchlist = false, isWatched = true)
            val cases =
                listOf(
                    FlagCase(LibraryFlag.FAVORITE, true, false, true, false, true),
                    FlagCase(LibraryFlag.FAVORITE, false, false, false, false, true),
                    FlagCase(LibraryFlag.WATCHLIST, true, false, false, true, false),
                    FlagCase(LibraryFlag.WATCHLIST, false, false, false, false, true),
                    FlagCase(LibraryFlag.WATCHED, true, false, false, false, true),
                    FlagCase(LibraryFlag.WATCHED, false, false, false, false, false),
                    FlagCase(LibraryFlag.FAVORITE, true, true, true, true, false),
                    FlagCase(LibraryFlag.FAVORITE, false, true, false, true, false),
                    FlagCase(LibraryFlag.WATCHLIST, true, true, true, true, false),
                    FlagCase(LibraryFlag.WATCHLIST, false, true, true, false, false),
                    FlagCase(LibraryFlag.WATCHED, true, true, true, false, true),
                    FlagCase(LibraryFlag.WATCHED, false, true, true, true, false),
                )

            cases.forEachIndexed { index, case ->
                val movieId = 100 + index
                if (case.existing) {
                    dao.upsertMovies(listOf(movieEntity(movieId, isFavorite = true, isWatchlist = true)))
                }

                dao.setLibraryFlag(staleInput.copy(id = movieId), case.flag, case.value)

                val saved = dao.getMovieById(movieId)
                assertEquals("Favorite for $case", case.expectedFavorite, saved?.isFavorite)
                assertEquals("Watchlist for $case", case.expectedWatchlist, saved?.isWatchlist)
                assertEquals("Watched for $case", case.expectedWatched, saved?.isWatched)
            }
        }

    @Test
    fun clearingWatchlist_preservesPersistedWatchedDespiteStaleInput() =
        runBlocking {
            val dao = database.movieDao()
            val persisted = movieEntity(id = 43, isFavorite = true, isWatched = true)
            dao.upsertMovies(listOf(persisted))

            dao.setLibraryFlag(movieEntity(id = 43, isWatchlist = true), LibraryFlag.WATCHLIST, false)

            assertEquals(persisted, dao.getMovieById(43))
        }

    @Test
    fun upsertMovieDetails_preservesAllLocalFlagsAndRuntime() =
        runBlocking {
            val dao = database.movieDao()
            insertMalformedMovieForTest(
                database,
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

    private data class FlagCase(
        val flag: LibraryFlag,
        val value: Boolean,
        val existing: Boolean,
        val expectedFavorite: Boolean,
        val expectedWatchlist: Boolean,
        val expectedWatched: Boolean,
    )
}
