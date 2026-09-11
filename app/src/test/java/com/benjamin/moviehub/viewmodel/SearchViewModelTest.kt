package com.benjamin.moviehub.viewmodel

import androidx.paging.PagingData
import com.benjamin.moviehub.domain.repository.MovieRepository
import com.benjamin.moviehub.ui.search.SearchViewModel
import com.benjamin.moviehub.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: MovieRepository = mockk()
    private lateinit var viewModel: SearchViewModel

    @Before
    fun setup() {
        coEvery { repository.searchMovies(any()) } returns flowOf(PagingData.empty())
        viewModel = SearchViewModel(repository)
    }

    @Test
    fun `search query should be debounced`() =
        runTest {
            val job = launch { viewModel.searchResults.collect() }

            viewModel.onSearchQueryChanged("A")
            advanceTimeBy(100)
            viewModel.onSearchQueryChanged("Av")
            advanceTimeBy(100)
            viewModel.onSearchQueryChanged("Ava")
            advanceTimeBy(600)

            coVerify { repository.searchMovies("Ava") }
            coVerify(exactly = 0) { repository.searchMovies("A") }
            coVerify(exactly = 0) { repository.searchMovies("Av") }

            job.cancel()
        }

    @Test
    fun `blank query does not trigger a search`() =
        runTest {
            val job = launch { viewModel.searchResults.collect() }
            viewModel.onSearchQueryChanged("   ")
            advanceTimeBy(600)
            coVerify(exactly = 0) { repository.searchMovies("") }
            job.cancel()
        }

    @Test
    fun `clearing the query cancels the active search`() =
        runTest {
            val started = CompletableDeferred<Unit>()
            val cancelled = CompletableDeferred<Unit>()
            coEvery { repository.searchMovies("Avengers") } returns
                flow {
                    started.complete(Unit)
                    try {
                        awaitCancellation()
                    } finally {
                        cancelled.complete(Unit)
                    }
                }

            val job = launch { viewModel.searchResults.collect() }
            viewModel.onSearchQueryChanged("Avengers")
            advanceTimeBy(600)
            started.await()

            viewModel.onSearchQueryChanged("")
            advanceTimeBy(600)
            runCurrent()

            assertTrue(cancelled.isCompleted)
            coVerify(exactly = 0) { repository.searchMovies("") }
            job.cancel()
        }
}
