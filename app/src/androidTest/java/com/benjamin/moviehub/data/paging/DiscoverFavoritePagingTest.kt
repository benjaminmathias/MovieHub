package com.benjamin.moviehub.data.paging

import androidx.paging.AsyncPagingDataDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.benjamin.moviehub.data.local.MovieDatabase
import com.benjamin.moviehub.data.local.MovieEntity
import com.benjamin.moviehub.data.remote.FakeMovieApiService
import com.benjamin.moviehub.data.repository.MovieRepositoryImpl
import com.benjamin.moviehub.domain.model.DiscoverFilters
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.ui.discover.DiscoverViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicInteger
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DiscoverFavoritePagingTest {
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
    fun discoverResults_followRoomFavoriteIds() =
        runBlocking {
            val dao = database.movieDao()
            dao.insertMovie(movieEntity(id = 2, isFavorite = true))
            assertEquals(listOf(2), dao.getLibraryMoviesFlow().first().map { it.id })

            val api = FakeMovieApiService()
            val differ = newDiffer()
            val emissions = AtomicInteger()
            val repository = MovieRepositoryImpl(api, database, dao)
            val viewModel = DiscoverViewModel(repository)
            val job =
                launch(Dispatchers.Main) {
                    viewModel
                        .discoverResults
                        .collectLatest {
                            emissions.incrementAndGet()
                            differ.submitData(it)
                        }
                }

            withTimeout(1_000) {
                while (emissions.get() == 0) delay(20)
            }
            var items = awaitItems(differ) { snapshot -> snapshot.any { it.id == 2 } }
            assertTrue(items.single { it.id == 2 }.isFavorite)
            assertFalse(items.single { it.id == 1 }.isFavorite)

            dao.setFavorite(movieEntity(id = 1), true)
            items = awaitItems(differ) { snapshot -> snapshot.singleOrNull { it.id == 1 }?.isFavorite == true }
            assertTrue(items.single { it.id == 1 }.isFavorite)
            assertEquals(listOf(1), api.discoverPagesRequested)

            job.cancel()
        }

    private suspend fun awaitItems(
        differ: AsyncPagingDataDiffer<Movie>,
        condition: (List<Movie>) -> Boolean,
    ): List<Movie> {
        val items =
            withTimeoutOrNull(10_000) {
            while (!condition(differ.snapshot().filterNotNull())) {
                delay(20)
            }
            differ.snapshot().filterNotNull()
        }
        return items ?: error("Timed out waiting for paging items: ${differ.snapshot().filterNotNull()}")
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
        isFavorite: Boolean = false,
    ) =
        MovieEntity(
            id = id,
            title = "Movie $id",
            overview = "Overview",
            posterPath = null,
            backdropPath = null,
            voteAverage = 7.0,
            releaseDate = "2020-01-01",
            isFavorite = isFavorite,
        )
}
