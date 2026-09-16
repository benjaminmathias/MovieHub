package com.benjamin.moviehub.data.paging

import androidx.paging.AsyncPagingDataDiffer
import androidx.paging.ExperimentalPagingApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.benjamin.moviehub.awaitItems
import com.benjamin.moviehub.data.local.MovieCategoryEntity
import com.benjamin.moviehub.data.local.MovieDatabase
import com.benjamin.moviehub.data.local.RemoteKey
import com.benjamin.moviehub.data.remote.FakeMovieApiService
import com.benjamin.moviehub.data.repository.MovieRepositoryImpl
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.model.MovieCategory
import com.benjamin.moviehub.inMemoryDatabase
import com.benjamin.moviehub.movieEntity
import com.benjamin.moviehub.newMovieDiffer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalPagingApi::class)
class ColdStartCategoryPagingTest {
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
    fun emptyCategory_receivesFirstPageThroughRealPager() =
        runBlocking {
            val api = FakeMovieApiService()
            val differ = newMovieDiffer()

            val job = collectCategory(MovieCategory.UPCOMING, api, differ)
            val items = differ.awaitItems()
            job.cancel()

            assertTrue("Expected the upcoming page to load, got $items", items.isNotEmpty())
            assertEquals("Film Prochainement 1", items.first().title)
            // A cold start loads the first page exactly once, without an initialize retry.
            assertEquals(listOf(1), api.upcomingPagesRequested)
        }

    @Test
    fun cachedCategory_isRefreshedInBackgroundOnColdStart() =
        runBlocking {
            seedCachedUpcoming()
            val api = FakeMovieApiService()
            val differ = newMovieDiffer()

            val job = collectCategory(MovieCategory.UPCOMING, api, differ)
            // Even though the category is cached, stale-while-revalidate refreshes it once.
            awaitRequest { api.upcomingPagesRequested.contains(1) }
            val items = differ.awaitItems { snapshot -> snapshot.any { it.title == "Film Prochainement 1" } }
            job.cancel()

            assertEquals(listOf(1), api.upcomingPagesRequested)
            assertEquals("Film Prochainement 1", items.first().title)
        }

    @Test
    fun failedRefresh_keepsCachedCategoryUsable() =
        runBlocking {
            seedCachedUpcoming()
            val api = FakeMovieApiService(failRequests = true)
            val differ = newMovieDiffer()

            val job = collectCategory(MovieCategory.UPCOMING, api, differ)
            awaitRequest { api.upcomingPagesRequested.isNotEmpty() }
            // The failed refresh must not clear the cached rows.
            assertEquals(listOf(500), database.movieDao().getCategoryMovieIds(MovieCategory.UPCOMING.key))
            val items = differ.awaitItems()
            job.cancel()

            assertEquals(listOf(500), items.map { it.id })
        }

    private fun CoroutineScope.collectCategory(
        category: MovieCategory,
        api: FakeMovieApiService,
        differ: AsyncPagingDataDiffer<Movie>,
    ): Job =
        launch(Dispatchers.Main) {
            MovieRepositoryImpl(api, database, database.movieDao())
                .getCategoryMovies(category)
                .collect { differ.submitData(it) }
        }

    private suspend fun seedCachedUpcoming() {
        val dao = database.movieDao()
        dao.upsertMovies(listOf(movieEntity(id = 500, title = "Cached Upcoming")))
        dao.insertCategoryMovies(
            listOf(MovieCategoryEntity(movieId = 500, category = MovieCategory.UPCOMING.key, pageOrder = 0)),
        )
        dao.upsertRemoteKey(RemoteKey(type = MovieCategory.UPCOMING.key, nextKey = null))
    }

    private suspend fun awaitRequest(condition: () -> Boolean) {
        withTimeout(10_000) {
            while (!condition()) delay(20)
        }
    }
}
