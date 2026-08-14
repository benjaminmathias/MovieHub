package com.benjamin.moviehub.viewmodel

import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.repository.MovieRepository
import com.benjamin.moviehub.ui.favorites.FavoriteViewModel
import com.benjamin.moviehub.ui.favorites.MovieFavoriteListUiState
import com.benjamin.moviehub.util.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FavoriteViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: MovieRepository = mockk()

    @Test
    fun `favorite list exposes empty success state`() =
        runTest {
            every { repository.getFavoriteMovies() } returns flowOf(emptyList())
            val viewModel = FavoriteViewModel(repository)
            advanceUntilIdle()

            val state = viewModel.uiState.value as MovieFavoriteListUiState.Success
            assertEquals(emptyList<Movie>(), state.movies)
            assertEquals(true, state.emptyMessage != null)
        }

    @Test
    fun `refresh cancels previous collection instead of running it concurrently`() =
        runTest {
            var activeCollections = 0
            val movie =
                Movie(
                    1,
                    "Movie",
                    "",
                    null,
                    null,
                    0.0,
                    "",
                    null,
                    true,
                    emptyList(),
                    emptyList(),
                )
            every { repository.getFavoriteMovies() } returns
                kotlinx.coroutines.flow.flow {
                    activeCollections++
                    try {
                        emit(listOf(movie))
                        kotlinx.coroutines.awaitCancellation()
                    } finally {
                        activeCollections--
                    }
                }
            val viewModel = FavoriteViewModel(repository)
            advanceUntilIdle()

            viewModel.refreshFavorite()
            advanceUntilIdle()

            assertEquals(1, activeCollections)
        }
}
