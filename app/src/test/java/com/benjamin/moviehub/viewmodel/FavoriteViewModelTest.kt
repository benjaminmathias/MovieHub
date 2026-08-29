package com.benjamin.moviehub.viewmodel

import app.cash.turbine.test
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.repository.MovieRepository
import com.benjamin.moviehub.ui.favorites.FavoriteViewModel
import com.benjamin.moviehub.ui.favorites.MovieFavoriteListUiState
import com.benjamin.moviehub.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
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

    private val movie =
        Movie(1, "Movie", "", null, null, 0.0, "", null, true, emptyList(), emptyList())

    @Test
    fun `favorite list exposes empty success state`() =
        runTest {
            every { repository.getFavoriteMovies() } returns flowOf(emptyList())
            val viewModel = FavoriteViewModel(repository)
            viewModel.uiState.test {
                val state = awaitItem() as MovieFavoriteListUiState.Success
                assertEquals(emptyList<Movie>(), state.movies)
                assertEquals(true, state.emptyMessage != null)
            }
        }

    @Test
    fun `favorite list exposes loading then non-empty state and reacts to room emissions`() =
        runTest {
            val source = MutableSharedFlow<List<Movie>>(replay = 0)
            every { repository.getFavoriteMovies() } returns source
            val viewModel = FavoriteViewModel(repository)
            viewModel.uiState.test {
                assert(awaitItem() is MovieFavoriteListUiState.Loading)
                source.emit(emptyList())
                assert(awaitItem() is MovieFavoriteListUiState.Success)
                source.emit(listOf(movie))
                val state = awaitItem() as MovieFavoriteListUiState.Success
                assertEquals(listOf(movie), state.movies)
            }
        }

    @Test
    fun `favorite errors become error state`() =
        runTest {
            every { repository.getFavoriteMovies() } returns flow { throw IllegalStateException() }
            val viewModel = FavoriteViewModel(repository)
            viewModel.uiState.test {
                assert(awaitItem() is MovieFavoriteListUiState.Error)
            }
        }

    @Test
    fun `toggle favorite delegates inverse state`() =
        runTest {
            coEvery { repository.toggleFavorite(movie, false) } returns Unit
            every { repository.getFavoriteMovies() } returns MutableSharedFlow()
            val viewModel = FavoriteViewModel(repository)
            viewModel.onToggleFavorite(movie)
            advanceUntilIdle()
            coVerify { repository.toggleFavorite(movie, false) }
        }
}
