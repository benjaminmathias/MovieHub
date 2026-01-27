package com.benjamin.moviehub.viewmodel

import com.benjamin.moviehub.domain.repository.MovieRepository
import com.benjamin.moviehub.ui.movie_list.MovieListViewModel
import com.benjamin.moviehub.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Before
import org.junit.Rule

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

    /*@Test
    fun `search failure should emit error state`() = runTest {
        // Given : repo throws an exception
        coEvery { repository.getPagedMovies(any()) } throws Exception("Network Error")
        coEvery { repository.getFavoriteMovies() } returns emptyFlow()

        viewModel.uiState.test {
            awaitItem()
            // When
            viewModel.onSearchQueryChanged("Avatar")
            advanceTimeBy(501)
            // Then
            val errorItem = awaitItem()
            assert(errorItem is MovieListUiState.Error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `favorites update should trigger a new state emission`() = runTest {
        // Given
        val favoritesFlow = MutableStateFlow<List<Movie>>(emptyList())
        every { repository.getFavoriteMovies() } returns favoritesFlow
        every { repository.getPagedMovies(any()) } returns emptyFlow()

        viewModel.uiState.test {
            awaitItem()

            // When
            favoritesFlow.value = listOf(mockk(relaxed = true))

            // Then
            val item = awaitItem()
            assert(item is MovieListUiState.Success)

            cancelAndIgnoreRemainingEvents()
        }
    }*/
}