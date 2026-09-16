package com.benjamin.moviehub.data.paging

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.benjamin.moviehub.awaitItems
import com.benjamin.moviehub.data.local.MovieDatabase
import com.benjamin.moviehub.data.remote.FakeMovieApiService
import com.benjamin.moviehub.data.repository.MovieRepositoryImpl
import com.benjamin.moviehub.inMemoryDatabase
import com.benjamin.moviehub.movieEntity
import com.benjamin.moviehub.newMovieDiffer
import com.benjamin.moviehub.ui.discover.DiscoverViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class DiscoverFavoritePagingTest {
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
    fun discoverResults_followRoomFavoriteIds() =
        runBlocking {
            val dao = database.movieDao()
            dao.insertMovie(movieEntity(id = 2, isFavorite = true))
            assertEquals(listOf(2), dao.getLibraryMoviesFlow().first().map { it.id })

            val api = FakeMovieApiService()
            val differ = newMovieDiffer()
            val emissions = AtomicInteger()
            val viewModel = DiscoverViewModel(MovieRepositoryImpl(api, database, dao))
            val job =
                launch(Dispatchers.Main) {
                    viewModel.discoverResults.collectLatest {
                        emissions.incrementAndGet()
                        differ.submitData(it)
                    }
                }

            withTimeout(1_000) {
                while (emissions.get() == 0) delay(20)
            }
            var items = differ.awaitItems { snapshot -> snapshot.any { it.id == 2 } }
            assertTrue(items.single { it.id == 2 }.isFavorite)
            assertFalse(items.single { it.id == 1 }.isFavorite)

            dao.setLibraryFlag(movieEntity(id = 1), isFavorite = true)
            items = differ.awaitItems { snapshot -> snapshot.singleOrNull { it.id == 1 }?.isFavorite == true }
            assertTrue(items.single { it.id == 1 }.isFavorite)
            assertEquals(listOf(1), api.discoverPagesRequested)

            job.cancel()
        }
}
