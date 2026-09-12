package com.benjamin.moviehub.viewmodel

import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.repository.MovieRepository
import com.benjamin.moviehub.ui.library.LibraryTab
import com.benjamin.moviehub.ui.library.LibraryUiState
import com.benjamin.moviehub.ui.library.LibraryViewModel
import com.benjamin.moviehub.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: MovieRepository = mockk()

    @Test
    fun `library emits all flagged movies reactively`() =
        runTest {
            val source = MutableStateFlow(listOf(movie(1, favorite = true), movie(2, watchlist = true)))
            every { repository.getLibraryMovies() } returns source
            val viewModel = LibraryViewModel(repository)
            val job = launch { viewModel.uiState.collect {} }

            advanceUntilIdle()
            assertEquals(2, (viewModel.uiState.value as LibraryUiState.Success).movies.size)

            source.value = listOf(movie(3, watched = true))
            advanceUntilIdle()
            assertEquals(listOf(3), (viewModel.uiState.value as LibraryUiState.Success).movies.map { it.id })
            job.cancel()
        }

    @Test
    fun `remove uses the requested tab flag`() =
        runTest {
            every { repository.getLibraryMovies() } returns MutableStateFlow(emptyList())
            coEvery { repository.setFavorite(any(), any()) } returns Unit
            coEvery { repository.setWatchlist(any(), any()) } returns Unit
            coEvery { repository.setWatched(any(), any()) } returns Unit
            val viewModel = LibraryViewModel(repository)
            val target = movie(1, favorite = true, watchlist = true, watched = true)

            viewModel.onRemove(target, LibraryTab.WATCHLIST)
            viewModel.onRemove(target, LibraryTab.FAVORITES)
            viewModel.onRemove(target, LibraryTab.WATCHED)
            advanceUntilIdle()

            coVerify { repository.setWatchlist(target, false) }
            coVerify { repository.setFavorite(target, false) }
            coVerify { repository.setWatched(target, false) }
        }

    private fun movie(
        id: Int,
        favorite: Boolean = false,
        watchlist: Boolean = false,
        watched: Boolean = false,
    ) = Movie(
        id = id,
        title = "Movie $id",
        overview = "",
        posterPath = null,
        backdropPath = null,
        voteAverage = 0.0,
        releaseDate = "",
        webUrl = null,
        isFavorite = favorite,
        isWatchlist = watchlist,
        isWatched = watched,
        genreIds = emptyList(),
        genres = emptyList(),
    )
}
