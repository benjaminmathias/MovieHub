package com.benjamin.moviehub.viewmodel

import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.model.MovieCredits
import com.benjamin.moviehub.domain.repository.MovieRepository
import com.benjamin.moviehub.ui.detail.MovieDetailUiState
import com.benjamin.moviehub.ui.detail.MovieDetailViewModel
import com.benjamin.moviehub.ui.detail.MovieRecommendationsUiState
import com.benjamin.moviehub.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MovieDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: MovieRepository = mockk()

    private val movie =
        Movie(
            id = 1,
            title = "Cached movie",
            overview = "Overview",
            posterPath = null,
            backdropPath = null,
            voteAverage = 8.0,
            releaseDate = "2024-01-01",
            webUrl = null,
            isFavorite = false,
            genreIds = persistentListOf(),
            genres = persistentListOf("Drama"),
        )

    @Before
    fun setup() {
        coEvery { repository.getMovieRecommendations(any()) } returns emptyList()
        every { repository.getLibraryMovies() } returns flowOf(emptyList())
    }

    @Test
    fun `details remain visible when casting is unavailable`() =
        runTest {
            coEvery { repository.getMovieDetails(1) } returns movie
            coEvery { repository.getMovieCredits(1) } throws IllegalStateException()
            val viewModel = MovieDetailViewModel(repository)

            viewModel.loadMovieDetails(1)
            advanceUntilIdle()

            assertEquals(
                MovieDetailUiState.Success(movie, MovieCredits(), MovieRecommendationsUiState.Empty),
                viewModel.uiState.value,
            )
        }

    @Test
    fun `library flow reconciles detail and recommendations flags`() =
        runTest {
            val recommendation = movie.copy(id = 2, title = "Suggested")
            val library = MutableStateFlow<List<Movie>>(emptyList())
            every { repository.getLibraryMovies() } returns library
            coEvery { repository.getMovieDetails(1) } returns movie
            coEvery { repository.getMovieRecommendations(1) } returns listOf(recommendation)
            val viewModel = MovieDetailViewModel(repository)

            viewModel.loadMovieDetails(1)
            advanceUntilIdle()
            library.value = listOf(movie.copy(isFavorite = true), recommendation.copy(isWatchlist = true))
            runCurrent()

            val state = viewModel.uiState.value as MovieDetailUiState.Success
            assertTrue(state.movie.isFavorite)
            assertTrue((state.recommendations as MovieRecommendationsUiState.Success).movies.single().isWatchlist)

            library.value = emptyList()
            runCurrent()
            val cleared = viewModel.uiState.value as MovieDetailUiState.Success
            assertEquals(false, cleared.movie.isFavorite)
            assertEquals(false, (cleared.recommendations as MovieRecommendationsUiState.Success).movies.single().isWatchlist)
        }

    @Test
    fun `watchlist and watched toggles stay mutually exclusive`() =
        runTest {
            val initial = movie.copy(isWatchlist = true)
            every { repository.getLibraryMovies() } returns MutableStateFlow(listOf(initial))
            coEvery { repository.getMovieDetails(1) } returns initial
            coEvery { repository.setWatched(any(), any()) } returns Unit
            coEvery { repository.setWatchlist(any(), any()) } returns Unit
            val viewModel = MovieDetailViewModel(repository)

            viewModel.loadMovieDetails(1)
            advanceUntilIdle()
            viewModel.toggleWatched()
            advanceUntilIdle()

            var state = viewModel.uiState.value as MovieDetailUiState.Success
            assertTrue(state.movie.isWatched)
            assertTrue(!state.movie.isWatchlist)

            viewModel.toggleWatchlist()
            advanceUntilIdle()
            state = viewModel.uiState.value as MovieDetailUiState.Success
            assertTrue(state.movie.isWatchlist)
            assertTrue(!state.movie.isWatched)
        }

    @Test
    fun `details retry succeeds after loading error`() =
        runTest {
            var detailsCalls = 0
            coEvery { repository.getMovieDetails(1) } coAnswers {
                if (detailsCalls++ == 0) throw IllegalStateException()
                movie
            }
            coEvery { repository.getMovieCredits(1) } returns MovieCredits()
            val viewModel = MovieDetailViewModel(repository)

            viewModel.loadMovieDetails(1)
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value is MovieDetailUiState.Error)

            viewModel.loadMovieDetails(1)
            advanceUntilIdle()

            assertEquals(
                MovieDetailUiState.Success(movie, MovieCredits(), MovieRecommendationsUiState.Empty),
                viewModel.uiState.value,
            )
        }

    @Test
    fun `favorite update restores previous state when repository fails`() =
        runTest {
            coEvery { repository.getMovieDetails(1) } returns movie
            coEvery { repository.getMovieCredits(1) } returns MovieCredits()
            val failureGate = CompletableDeferred<Unit>()
            coEvery { repository.setFavorite(movie, true) } coAnswers {
                failureGate.await()
                throw IllegalStateException()
            }
            val viewModel = MovieDetailViewModel(repository)
            viewModel.loadMovieDetails(1)
            advanceUntilIdle()

            viewModel.toggleFavorite()
            runCurrent()
            val pendingState = viewModel.uiState.value as MovieDetailUiState.Success
            assertTrue(pendingState.movie.isFavorite)
            assertTrue(pendingState.isLibraryActionPending)
            failureGate.complete(Unit)
            advanceUntilIdle()
            val restoredState = viewModel.uiState.value as MovieDetailUiState.Success
            assertFalse(restoredState.movie.isFavorite)
            assertFalse(restoredState.isLibraryActionPending)
            coVerify(exactly = 1) { repository.setFavorite(movie, true) }
        }

    @Test
    fun `successful library action clears pending state`() =
        runTest {
            coEvery { repository.getMovieDetails(1) } returns movie
            coEvery { repository.getMovieCredits(1) } returns MovieCredits()
            val successGate = CompletableDeferred<Unit>()
            coEvery { repository.setFavorite(movie, true) } coAnswers { successGate.await() }
            val viewModel = MovieDetailViewModel(repository)

            viewModel.loadMovieDetails(1)
            advanceUntilIdle()

            viewModel.toggleFavorite()
            runCurrent()
            assertTrue((viewModel.uiState.value as MovieDetailUiState.Success).isLibraryActionPending)

            successGate.complete(Unit)
            advanceUntilIdle()

            val state = viewModel.uiState.value as MovieDetailUiState.Success
            assertTrue(state.movie.isFavorite)
            assertFalse(state.isLibraryActionPending)
        }

    @Test
    fun `double toggle uses latest state and does not rollback newer transition`() =
        runTest {
            coEvery { repository.getMovieDetails(1) } returns movie
            coEvery { repository.getMovieCredits(1) } returns MovieCredits()
            coEvery { repository.setFavorite(any(), any()) } returns Unit
            val viewModel = MovieDetailViewModel(repository)
            viewModel.loadMovieDetails(1)
            advanceUntilIdle()

            viewModel.toggleFavorite()
            viewModel.toggleFavorite()
            advanceUntilIdle()

            assertTrue(!(viewModel.uiState.value as MovieDetailUiState.Success).movie.isFavorite)
            coVerify(exactly = 1) { repository.setFavorite(movie, true) }
            coVerify(exactly = 1) { repository.setFavorite(movie.copy(isFavorite = true), false) }
        }

    @Test
    fun `late credits preserve an optimistic favorite`() =
        runTest {
            val creditsGate = CompletableDeferred<MovieCredits>()
            coEvery { repository.getMovieDetails(1) } returns movie
            coEvery { repository.getMovieCredits(1) } coAnswers { creditsGate.await() }
            coEvery { repository.setFavorite(movie, true) } returns Unit
            val viewModel = MovieDetailViewModel(repository)

            viewModel.loadMovieDetails(1)
            runCurrent()
            viewModel.toggleFavorite()
            runCurrent()
            assertTrue((viewModel.uiState.value as MovieDetailUiState.Success).movie.isFavorite)

            creditsGate.complete(MovieCredits(director = "Director"))
            advanceUntilIdle()

            val state = viewModel.uiState.value as MovieDetailUiState.Success
            assertTrue(state.movie.isFavorite)
            assertEquals("Director", state.credits.director)
        }

    @Test
    fun `recommendations failure maps to the error state without breaking details`() =
        runTest {
            coEvery { repository.getMovieDetails(1) } returns movie
            coEvery { repository.getMovieCredits(1) } returns MovieCredits()
            coEvery { repository.getMovieRecommendations(1) } throws IllegalStateException()
            val viewModel = MovieDetailViewModel(repository)

            viewModel.loadMovieDetails(1)
            advanceUntilIdle()

            val state = viewModel.uiState.value as MovieDetailUiState.Success
            assertEquals(MovieRecommendationsUiState.Error, state.recommendations)
            assertEquals(movie, state.movie)
        }

    @Test
    fun `failed library action emits a library error`() =
        runTest {
            coEvery { repository.getMovieDetails(1) } returns movie
            coEvery { repository.getMovieCredits(1) } returns MovieCredits()
            coEvery { repository.setWatched(movie, true) } throws IllegalStateException()
            val viewModel = MovieDetailViewModel(repository)
            val errors = mutableListOf<Unit>()
            val job = launch { viewModel.libraryActionErrors.collect { errors += it } }

            viewModel.loadMovieDetails(1)
            advanceUntilIdle()
            viewModel.toggleWatched()
            advanceUntilIdle()

            assertEquals(1, errors.size)
            job.cancel()
        }
}
