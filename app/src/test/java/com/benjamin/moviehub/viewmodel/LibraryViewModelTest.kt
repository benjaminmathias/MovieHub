package com.benjamin.moviehub.viewmodel

import com.benjamin.moviehub.domain.model.LibraryFlag
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.repository.LibraryRepository
import com.benjamin.moviehub.ui.library.LibraryTab
import com.benjamin.moviehub.ui.library.LibraryUiState
import com.benjamin.moviehub.ui.library.LibraryViewModel
import com.benjamin.moviehub.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: LibraryRepository = mockk()

    @Test
    fun `copy with new movies recomputes tabs and leaves the original state unchanged`() {
        val original =
            LibraryUiState.Success(
                persistentListOf(movie(1, favorite = true), movie(2, watchlist = true)),
            )

        val updated = original.copy(movies = persistentListOf(movie(3, watched = true)))

        assertEquals(listOf(3), updated.moviesByTab.getValue(LibraryTab.WATCHED).map { it.id })
        assertTrue(updated.moviesByTab.getValue(LibraryTab.FAVORITES).isEmpty())
        assertTrue(updated.moviesByTab.getValue(LibraryTab.WATCHLIST).isEmpty())

        assertEquals(listOf(1), original.moviesByTab.getValue(LibraryTab.FAVORITES).map { it.id })
        assertEquals(listOf(2), original.moviesByTab.getValue(LibraryTab.WATCHLIST).map { it.id })
        assertTrue(original.moviesByTab.getValue(LibraryTab.WATCHED).isEmpty())
    }

    @Test
    fun `library emits all flagged movies reactively`() =
        runTest {
            val source = MutableStateFlow(listOf(movie(1, favorite = true, watchlist = true), movie(2, watchlist = true)))
            every { repository.getLibraryMovies() } returns source
            val viewModel = LibraryViewModel(repository)
            val job = launch { viewModel.uiState.collect {} }

            advanceUntilIdle()
            val initialState = viewModel.uiState.value as LibraryUiState.Success
            assertEquals(2, initialState.movies.size)
            assertEquals(listOf(1, 2), initialState.moviesByTab.getValue(LibraryTab.WATCHLIST).map { it.id })
            assertEquals(listOf(1), initialState.moviesByTab.getValue(LibraryTab.FAVORITES).map { it.id })
            assertTrue(initialState.moviesByTab.getValue(LibraryTab.WATCHED).isEmpty())

            source.value = listOf(movie(3, watched = true))
            advanceUntilIdle()
            val updatedState = viewModel.uiState.value as LibraryUiState.Success
            assertEquals(listOf(3), updatedState.movies.map { it.id })
            assertEquals(listOf(3), updatedState.moviesByTab.getValue(LibraryTab.WATCHED).map { it.id })
            assertTrue(updatedState.moviesByTab.getValue(LibraryTab.WATCHLIST).isEmpty())
            job.cancel()
        }

    @Test
    fun `remove uses the requested tab flag`() =
        runTest {
            every { repository.getLibraryMovies() } returns MutableStateFlow(emptyList())
            coEvery { repository.setLibraryFlag(any(), any(), any()) } returns Unit
            val viewModel = LibraryViewModel(repository)
            val target = movie(1, favorite = true, watchlist = true, watched = true)

            viewModel.onRemove(target, LibraryTab.WATCHLIST)
            viewModel.onRemove(target, LibraryTab.FAVORITES)
            viewModel.onRemove(target, LibraryTab.WATCHED)
            advanceUntilIdle()

            coVerify { repository.setLibraryFlag(target, LibraryFlag.WATCHLIST, false) }
            coVerify { repository.setLibraryFlag(target, LibraryFlag.FAVORITE, false) }
            coVerify { repository.setLibraryFlag(target, LibraryFlag.WATCHED, false) }
        }

    @Test
    fun `retry recovers after a loading error`() =
        runTest {
            var shouldFail = true
            every { repository.getLibraryMovies() } answers {
                if (shouldFail) flow { throw IllegalStateException("offline") } else flowOf(listOf(movie(1, favorite = true)))
            }
            val viewModel = LibraryViewModel(repository)
            val job = launch { viewModel.uiState.collect {} }

            advanceUntilIdle()
            assertTrue(viewModel.uiState.value is LibraryUiState.Error)

            shouldFail = false
            viewModel.onRetry()
            advanceUntilIdle()
            assertEquals(listOf(1), (viewModel.uiState.value as LibraryUiState.Success).movies.map { it.id })

            job.cancel()
        }

    @Test
    fun `remove failure keeps one pending error until acknowledged`() =
        runTest {
            every { repository.getLibraryMovies() } returns MutableStateFlow(emptyList())
            coEvery { repository.setLibraryFlag(any(), any(), any()) } throws IllegalStateException("offline")
            val viewModel = LibraryViewModel(repository)
            val target = movie(1, watchlist = true)

            viewModel.onRemove(target, LibraryTab.WATCHLIST)
            advanceUntilIdle()
            assertTrue(viewModel.actionErrorPending.value)

            // A repeated failure keeps the single pending message.
            viewModel.onRemove(target, LibraryTab.WATCHLIST)
            advanceUntilIdle()
            assertTrue(viewModel.actionErrorPending.value)

            viewModel.acknowledgeActionError()
            assertFalse(viewModel.actionErrorPending.value)
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
        genreIds = persistentListOf(),
        genres = persistentListOf(),
    )
}
