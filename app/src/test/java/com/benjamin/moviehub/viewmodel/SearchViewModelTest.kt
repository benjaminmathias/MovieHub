package com.benjamin.moviehub.viewmodel

import androidx.paging.PagingData
import com.benjamin.moviehub.domain.model.MovieCategory
import com.benjamin.moviehub.domain.repository.MovieRepository
import com.benjamin.moviehub.ui.search.SearchViewModel
import com.benjamin.moviehub.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
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
        coEvery { repository.getPagedMovies(any(), any()) } returns flowOf(PagingData.empty())
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

            coVerify { repository.getPagedMovies("Ava", MovieCategory.POPULAR) }
            coVerify(exactly = 0) { repository.getPagedMovies("A", any()) }
            coVerify(exactly = 0) { repository.getPagedMovies("Av", any()) }

            job.cancel()
        }

    @Test
    fun `blank query does not trigger a search`() =
        runTest {
            val job = launch { viewModel.searchResults.collect() }
            viewModel.onSearchQueryChanged("   ")
            advanceTimeBy(600)
            coVerify(exactly = 0) { repository.getPagedMovies("", any()) }
            job.cancel()
        }
}
