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
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
            genreIds = emptyList(),
            genres = listOf("Drama"),
        )

    @Before
    fun setup() {
        coEvery { repository.getMovieRecommendations(any()) } returns emptyList()
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
            coEvery { repository.toggleFavorite(movie, true) } coAnswers {
                failureGate.await()
                throw IllegalStateException()
            }
            val viewModel = MovieDetailViewModel(repository)
            viewModel.loadMovieDetails(1)
            advanceUntilIdle()

            viewModel.toggleFavorite()
            runCurrent()
            assertEquals(true, (viewModel.uiState.value as MovieDetailUiState.Success).movie.isFavorite)
            failureGate.complete(Unit)
            advanceUntilIdle()
            assertEquals(false, (viewModel.uiState.value as MovieDetailUiState.Success).movie.isFavorite)
            coVerify(exactly = 1) { repository.toggleFavorite(movie, true) }
        }

    @Test
    fun `double toggle uses latest state and does not rollback newer transition`() =
        runTest {
            coEvery { repository.getMovieDetails(1) } returns movie
            coEvery { repository.getMovieCredits(1) } returns MovieCredits()
            coEvery { repository.toggleFavorite(any(), any()) } returns Unit
            val viewModel = MovieDetailViewModel(repository)
            viewModel.loadMovieDetails(1)
            advanceUntilIdle()

            viewModel.toggleFavorite()
            viewModel.toggleFavorite()
            advanceUntilIdle()

            assertTrue((viewModel.uiState.value as MovieDetailUiState.Success).movie.isFavorite.not())
            coVerify(exactly = 1) { repository.toggleFavorite(movie, true) }
            coVerify(exactly = 1) { repository.toggleFavorite(movie.copy(isFavorite = true), false) }
        }

    @Test
    fun `two failed toggles serialize and leave the original state`() =
        runTest {
            coEvery { repository.getMovieDetails(1) } returns movie
            coEvery { repository.getMovieCredits(1) } returns MovieCredits()
            coEvery { repository.toggleFavorite(any(), any()) } throws IllegalStateException()
            val viewModel = MovieDetailViewModel(repository)
            viewModel.loadMovieDetails(1)
            advanceUntilIdle()

            viewModel.toggleFavorite()
            viewModel.toggleFavorite()
            advanceUntilIdle()

            assertTrue((viewModel.uiState.value as MovieDetailUiState.Success).movie.isFavorite.not())
            coVerify(exactly = 2) { repository.toggleFavorite(movie, true) }
        }

    @Test
    fun `late credits preserve an optimistic favorite`() =
        runTest {
            val creditsGate = CompletableDeferred<MovieCredits>()
            coEvery { repository.getMovieDetails(1) } returns movie
            coEvery { repository.getMovieCredits(1) } coAnswers { creditsGate.await() }
            coEvery { repository.toggleFavorite(movie, true) } returns Unit
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
    fun `recommendations stay loading until the remote call resolves`() =
        runTest {
            val recommendationsGate = CompletableDeferred<List<Movie>>()
            coEvery { repository.getMovieDetails(1) } returns movie
            coEvery { repository.getMovieCredits(1) } returns MovieCredits()
            coEvery { repository.getMovieRecommendations(1) } coAnswers { recommendationsGate.await() }
            val viewModel = MovieDetailViewModel(repository)

            viewModel.loadMovieDetails(1)
            runCurrent()

            assertEquals(
                MovieRecommendationsUiState.Loading,
                (viewModel.uiState.value as MovieDetailUiState.Success).recommendations,
            )

            val suggested = movie.copy(id = 2, title = "Suggested")
            recommendationsGate.complete(listOf(suggested))
            advanceUntilIdle()

            assertEquals(
                MovieRecommendationsUiState.Success(listOf(suggested)),
                (viewModel.uiState.value as MovieDetailUiState.Success).recommendations,
            )
        }

    @Test
    fun `empty recommendations map to the empty state`() =
        runTest {
            coEvery { repository.getMovieDetails(1) } returns movie
            coEvery { repository.getMovieCredits(1) } returns MovieCredits()
            coEvery { repository.getMovieRecommendations(1) } returns emptyList()
            val viewModel = MovieDetailViewModel(repository)

            viewModel.loadMovieDetails(1)
            advanceUntilIdle()

            assertEquals(
                MovieRecommendationsUiState.Empty,
                (viewModel.uiState.value as MovieDetailUiState.Success).recommendations,
            )
        }

    @Test
    fun `recommendation failure maps to the error state without breaking details`() =
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
}
