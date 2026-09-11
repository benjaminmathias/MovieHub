package com.benjamin.moviehub.data.paging

import androidx.paging.AsyncPagingDataDiffer
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.benjamin.moviehub.data.local.MovieCategoryEntity
import com.benjamin.moviehub.data.local.MovieDatabase
import com.benjamin.moviehub.data.local.MovieEntity
import com.benjamin.moviehub.data.local.MovieRemoteKey
import com.benjamin.moviehub.data.mapper.toEntity
import com.benjamin.moviehub.data.remote.FakeMovieApiService
import com.benjamin.moviehub.data.remote.movieDto
import com.benjamin.moviehub.data.repository.MovieRepositoryImpl
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.model.MovieCategory
import kotlinx.coroutines.Dispatchers
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
        database =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    MovieDatabase::class.java,
                ).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun emptyCategory_receivesFirstPageThroughRealPager() =
        runBlocking {
            val api = FakeMovieApiService()
            val repository = MovieRepositoryImpl(api, database, database.movieDao())
            val differ = newDiffer()

            val job =
                launch(Dispatchers.Main) {
                    repository
                        .getCategoryMovies(MovieCategory.UPCOMING)
                        .collect { differ.submitData(it) }
                }

            val items = awaitItems(differ)
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
            val repository = MovieRepositoryImpl(api, database, database.movieDao())
            val differ = newDiffer()

            val job =
                launch(Dispatchers.Main) {
                    repository
                        .getCategoryMovies(MovieCategory.UPCOMING)
                        .collect { differ.submitData(it) }
                }

            // Even though the category is cached, stale-while-revalidate refreshes it once.
            awaitRequest { api.upcomingPagesRequested.contains(1) }
            val items = awaitItems(differ) { snapshot -> snapshot.any { it.title == "Film Prochainement 1" } }
            job.cancel()

            assertEquals(listOf(1), api.upcomingPagesRequested)
            assertEquals("Film Prochainement 1", items.first().title)
        }

    @Test
    fun failedRefresh_keepsCachedCategoryUsable() =
        runBlocking {
            seedCachedUpcoming()

            val api = FakeMovieApiService(failRequests = true)
            val repository = MovieRepositoryImpl(api, database, database.movieDao())
            val differ = newDiffer()

            val job =
                launch(Dispatchers.Main) {
                    repository
                        .getCategoryMovies(MovieCategory.UPCOMING)
                        .collect { differ.submitData(it) }
                }

            awaitRequest { api.upcomingPagesRequested.isNotEmpty() }
            // The failed refresh must not clear the cached rows.
            assertEquals(listOf(500), database.movieDao().getCategoryMovieIds(MovieCategory.UPCOMING.key))
            val items = awaitItems(differ)
            job.cancel()

            assertEquals(listOf(500), items.map { it.id })
        }

    private suspend fun seedCachedUpcoming() {
        val dao = database.movieDao()
        dao.upsertMovies(listOf(movieEntity(id = 500, title = "Cached Upcoming")))
        dao.insertCategoryMovies(
            listOf(
                MovieCategoryEntity(
                    movieId = 500,
                    category = MovieCategory.UPCOMING.key,
                    pageOrder = 0,
                ),
            ),
        )
        dao.insertAllKeys(
            listOf(MovieRemoteKey(500, prevKey = null, nextKey = null, type = MovieCategory.UPCOMING.key)),
        )
    }

    private suspend fun awaitItems(
        differ: AsyncPagingDataDiffer<Movie>,
        condition: (List<Movie>) -> Boolean = { it.isNotEmpty() },
    ): List<Movie> =
        withTimeout(10_000) {
            while (!condition(differ.snapshot().filterNotNull())) {
                delay(20)
            }
            differ.snapshot().filterNotNull()
        }

    private suspend fun awaitRequest(condition: () -> Boolean) {
        withTimeout(10_000) {
            while (!condition()) {
                delay(20)
            }
        }
    }

    private fun newDiffer(): AsyncPagingDataDiffer<Movie> =
        AsyncPagingDataDiffer(
            diffCallback =
                object : DiffUtil.ItemCallback<Movie>() {
                    override fun areItemsTheSame(
                        oldItem: Movie,
                        newItem: Movie,
                    ): Boolean = oldItem.id == newItem.id

                    override fun areContentsTheSame(
                        oldItem: Movie,
                        newItem: Movie,
                    ): Boolean = oldItem == newItem
                },
            updateCallback =
                object : ListUpdateCallback {
                    override fun onInserted(
                        position: Int,
                        count: Int,
                    ) = Unit

                    override fun onRemoved(
                        position: Int,
                        count: Int,
                    ) = Unit

                    override fun onMoved(
                        fromPosition: Int,
                        toPosition: Int,
                    ) = Unit

                    override fun onChanged(
                        position: Int,
                        count: Int,
                        payload: Any?,
                    ) = Unit
                },
            mainDispatcher = Dispatchers.Main,
        )

    private fun movieEntity(
        id: Int,
        title: String,
    ): MovieEntity = movieDto(id = id, title = title).toEntity()
}
