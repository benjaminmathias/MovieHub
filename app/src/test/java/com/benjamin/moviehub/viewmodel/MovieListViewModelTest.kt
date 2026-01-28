package com.benjamin.moviehub.viewmodel

import androidx.paging.PagingData
import app.cash.turbine.test
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.repository.MovieRepository
import com.benjamin.moviehub.ui.movie_list.MovieListUiState
import com.benjamin.moviehub.ui.movie_list.MovieListViewModel
import com.benjamin.moviehub.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MovieListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: MovieRepository = mockk()
    private lateinit var viewModel: MovieListViewModel

    @Before
    fun setup() {
        coEvery { repository.getPagedMovies(any()) } returns emptyFlow()
        coEvery { repository.getFavoriteMovies() } returns emptyFlow()
        viewModel = MovieListViewModel(repository)
    }

    @Test
    fun `search query should wait for debounce before calling repo`() = runTest {
        // Given
        val query = "Ava"

        every { repository.getPagedMovies(any()) } returns flowOf(PagingData.empty())
        every { repository.getFavoriteMovies() } returns flowOf(emptyList())

        viewModel = MovieListViewModel(repository)

        viewModel.uiState.test {
            assert(awaitItem() is MovieListUiState.Loading)

            // When
            viewModel.onSearchQueryChanged(query)

            // Then :
            advanceTimeBy(400)
            expectNoEvents()

            advanceTimeBy(200)

            val successState = awaitItem() as MovieListUiState.Success
            assert(successState.searchQuery == query)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `repository failure should emit error state`() = runTest {
        // Given
        val errorMessage = "Database Error"
        every { repository.getFavoriteMovies() } returns flow { throw Exception(errorMessage) }
        every { repository.getPagedMovies(any()) } returns flowOf(PagingData.empty())

        viewModel = MovieListViewModel(repository)

        assert(viewModel.uiState.value is MovieListUiState.Loading)

        viewModel.uiState.test {
            val firstItem = awaitItem()

            if (firstItem is MovieListUiState.Loading) {
                advanceTimeBy(600)
                val secondItem = awaitItem()
                assert(secondItem is MovieListUiState.Error)
            } else {
                assert(firstItem is MovieListUiState.Error)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `favorites update should trigger a new state emission`() = runTest {
        val favoritesFlow = MutableStateFlow<List<Movie>>(emptyList())
        val movieMock = mockk<Movie>(relaxed = true) { every { id } returns 1 }

        every { repository.getFavoriteMovies() } returns favoritesFlow
        every { repository.getPagedMovies(any()) } returns flowOf(PagingData.from(listOf(movieMock)))

        viewModel = MovieListViewModel(repository)

        viewModel.uiState.test {
            assert(awaitItem() is MovieListUiState.Loading)

            advanceTimeBy(600)
            val initialState = awaitItem() as MovieListUiState.Success
            assert(initialState.searchQuery.isEmpty())

            // When
            favoritesFlow.value = listOf(movieMock)

            // Then
            val updatedState = awaitItem() as MovieListUiState.Success

            assert(updatedState !== initialState)

            assert(updatedState.searchQuery == initialState.searchQuery)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
