package com.benjamin.moviehub.viewmodel

import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.model.MovieCredits
import com.benjamin.moviehub.domain.repository.LibraryRepository
import com.benjamin.moviehub.domain.repository.MovieRepository
import com.benjamin.moviehub.ui.detail.LibraryObservationUiState
import com.benjamin.moviehub.ui.detail.MovieCreditsUiState
import com.benjamin.moviehub.ui.detail.MovieDetailUiState
import com.benjamin.moviehub.ui.detail.MovieDetailViewModel
import com.benjamin.moviehub.ui.detail.MovieRecommendationsUiState
import com.benjamin.moviehub.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
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
    private val libraryRepository: LibraryRepository = mockk()

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
        every { libraryRepository.getLibraryMovies() } returns flowOf(emptyList())
    }

    @Test
    fun `details remain visible when casting is unavailable`() =
        runTest {
            coEvery { repository.getMovieDetails(1) } returns movie
            coEvery { repository.getMovieCredits(1) } throws IllegalStateException()
            val viewModel = MovieDetailViewModel(repository, libraryRepository)

            viewModel.loadMovieDetails(1)
            advanceUntilIdle()

            assertEquals(
                MovieDetailUiState.Success(
                    movie = movie,
                    credits = MovieCreditsUiState.Error,
                    recommendations = MovieRecommendationsUiState.Empty,
                    libraryObservation = LibraryObservationUiState.Ready,
                ),
                viewModel.uiState.value,
            )
        }

    @Test
    fun `library flow reconciles detail and recommendations flags`() =
        runTest {
            val recommendation = movie.copy(id = 2, title = "Suggested")
            val library = MutableStateFlow<List<Movie>>(emptyList())
            every { libraryRepository.getLibraryMovies() } returns library
            coEvery { repository.getMovieDetails(1) } returns movie
            coEvery { repository.getMovieRecommendations(1) } returns listOf(recommendation)
            val viewModel = MovieDetailViewModel(repository, libraryRepository)

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
            every { libraryRepository.getLibraryMovies() } returns MutableStateFlow(listOf(initial))
            coEvery { repository.getMovieDetails(1) } returns initial
            coEvery { libraryRepository.setWatched(any(), any()) } returns Unit
            coEvery { libraryRepository.setWatchlist(any(), any()) } returns Unit
            val viewModel = MovieDetailViewModel(repository, libraryRepository)

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
            val viewModel = MovieDetailViewModel(repository, libraryRepository)

            viewModel.loadMovieDetails(1)
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value is MovieDetailUiState.Error)

            viewModel.loadMovieDetails(1)
            advanceUntilIdle()

            assertEquals(
                MovieDetailUiState.Success(
                    movie = movie,
                    credits = MovieCreditsUiState.Success(MovieCredits()),
                    recommendations = MovieRecommendationsUiState.Empty,
                    libraryObservation = LibraryObservationUiState.Ready,
                ),
                viewModel.uiState.value,
            )
        }

    @Test
    fun `favorite update restores previous state when repository fails`() =
        runTest {
            coEvery { repository.getMovieDetails(1) } returns movie
            coEvery { repository.getMovieCredits(1) } returns MovieCredits()
            val failureGate = CompletableDeferred<Unit>()
            coEvery { libraryRepository.setFavorite(movie, true) } coAnswers {
                failureGate.await()
                throw IllegalStateException()
            }
            val viewModel = MovieDetailViewModel(repository, libraryRepository)
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
            coVerify(exactly = 1) { libraryRepository.setFavorite(movie, true) }
        }

    @Test
    fun `successful library action clears pending state`() =
        runTest {
            coEvery { repository.getMovieDetails(1) } returns movie
            coEvery { repository.getMovieCredits(1) } returns MovieCredits()
            val successGate = CompletableDeferred<Unit>()
            coEvery { libraryRepository.setFavorite(movie, true) } coAnswers { successGate.await() }
            val viewModel = MovieDetailViewModel(repository, libraryRepository)

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
            coEvery { libraryRepository.setFavorite(any(), any()) } returns Unit
            val viewModel = MovieDetailViewModel(repository, libraryRepository)
            viewModel.loadMovieDetails(1)
            advanceUntilIdle()

            viewModel.toggleFavorite()
            viewModel.toggleFavorite()
            advanceUntilIdle()

            assertTrue(!(viewModel.uiState.value as MovieDetailUiState.Success).movie.isFavorite)
            coVerify(exactly = 1) { libraryRepository.setFavorite(movie, true) }
            coVerify(exactly = 1) { libraryRepository.setFavorite(movie.copy(isFavorite = true), false) }
        }

    @Test
    fun `late credits preserve an optimistic favorite`() =
        runTest {
            val creditsGate = CompletableDeferred<MovieCredits>()
            coEvery { repository.getMovieDetails(1) } returns movie
            coEvery { repository.getMovieCredits(1) } coAnswers { creditsGate.await() }
            coEvery { libraryRepository.setFavorite(movie, true) } returns Unit
            val viewModel = MovieDetailViewModel(repository, libraryRepository)

            viewModel.loadMovieDetails(1)
            runCurrent()
            viewModel.toggleFavorite()
            runCurrent()
            assertTrue((viewModel.uiState.value as MovieDetailUiState.Success).movie.isFavorite)

            creditsGate.complete(MovieCredits(director = "Director"))
            advanceUntilIdle()

            val state = viewModel.uiState.value as MovieDetailUiState.Success
            assertTrue(state.movie.isFavorite)
            assertEquals(MovieCreditsUiState.Success(MovieCredits(director = "Director")), state.credits)
        }

    @Test
    fun `recommendations failure maps to the error state without breaking details`() =
        runTest {
            coEvery { repository.getMovieDetails(1) } returns movie
            coEvery { repository.getMovieCredits(1) } returns MovieCredits()
            coEvery { repository.getMovieRecommendations(1) } throws IllegalStateException()
            val viewModel = MovieDetailViewModel(repository, libraryRepository)

            viewModel.loadMovieDetails(1)
            advanceUntilIdle()

            val state = viewModel.uiState.value as MovieDetailUiState.Success
            assertEquals(MovieRecommendationsUiState.Error, state.recommendations)
            assertEquals(MovieCreditsUiState.Success(MovieCredits()), state.credits)
            assertEquals(movie, state.movie)
        }

    @Test
    fun `failed library action keeps one pending error until acknowledged`() =
        runTest {
            coEvery { repository.getMovieDetails(1) } returns movie
            coEvery { repository.getMovieCredits(1) } returns MovieCredits()
            coEvery { libraryRepository.setWatched(movie, true) } throws IllegalStateException()
            val viewModel = MovieDetailViewModel(repository, libraryRepository)

            viewModel.loadMovieDetails(1)
            advanceUntilIdle()
            viewModel.toggleWatched()
            advanceUntilIdle()

            // The error is a state: it survives until the UI acknowledges it.
            assertTrue(viewModel.libraryActionErrorPending.value)

            viewModel.acknowledgeLibraryActionError()
            assertFalse(viewModel.libraryActionErrorPending.value)
        }

    @Test
    fun `loading the same movie while in flight does not restart the load`() =
        runTest {
            val detailsGate = CompletableDeferred<Movie>()
            coEvery { repository.getMovieDetails(1) } coAnswers { detailsGate.await() }
            coEvery { repository.getMovieCredits(1) } returns MovieCredits()
            val viewModel = MovieDetailViewModel(repository, libraryRepository)

            viewModel.loadMovieDetails(1)
            runCurrent()
            viewModel.loadMovieDetails(1)
            advanceUntilIdle()

            coVerify(exactly = 1) { repository.getMovieDetails(1) }

            detailsGate.complete(movie)
            advanceUntilIdle()

            assertEquals(
                MovieDetailUiState.Success(
                    movie = movie,
                    credits = MovieCreditsUiState.Success(MovieCredits()),
                    recommendations = MovieRecommendationsUiState.Empty,
                    libraryObservation = LibraryObservationUiState.Ready,
                ),
                viewModel.uiState.value,
            )
        }

    @Test
    fun `loading the same movie again after success does not reload`() =
        runTest {
            coEvery { repository.getMovieDetails(1) } returns movie
            coEvery { repository.getMovieCredits(1) } returns MovieCredits()
            val viewModel = MovieDetailViewModel(repository, libraryRepository)

            viewModel.loadMovieDetails(1)
            advanceUntilIdle()
            viewModel.loadMovieDetails(1)
            advanceUntilIdle()

            coVerify(exactly = 1) { repository.getMovieDetails(1) }
        }

    @Test
    fun `loading a different movie replaces the in flight load`() =
        runTest {
            val detailsGate = CompletableDeferred<Movie>()
            coEvery { repository.getMovieDetails(1) } coAnswers { detailsGate.await() }
            coEvery { repository.getMovieDetails(2) } returns movie.copy(id = 2, title = "Second")
            coEvery { repository.getMovieCredits(any()) } returns MovieCredits()
            val viewModel = MovieDetailViewModel(repository, libraryRepository)

            viewModel.loadMovieDetails(1)
            runCurrent()
            viewModel.loadMovieDetails(2)
            advanceUntilIdle()

            assertEquals(2, (viewModel.uiState.value as MovieDetailUiState.Success).movie.id)
            coVerify(exactly = 1) { repository.getMovieDetails(1) }
            coVerify(exactly = 1) { repository.getMovieDetails(2) }
        }

    @Test
    fun `credits retry reloads only credits and keeps the film`() =
        runTest {
            var creditsCalls = 0
            coEvery { repository.getMovieDetails(1) } returns movie
            coEvery { repository.getMovieCredits(1) } coAnswers {
                if (creditsCalls++ == 0) throw IllegalStateException()
                MovieCredits(director = "Director")
            }
            val viewModel = MovieDetailViewModel(repository, libraryRepository)

            viewModel.loadMovieDetails(1)
            advanceUntilIdle()
            assertTrue((viewModel.uiState.value as MovieDetailUiState.Success).credits is MovieCreditsUiState.Error)

            viewModel.retryCredits()
            advanceUntilIdle()

            val state = viewModel.uiState.value as MovieDetailUiState.Success
            assertEquals(MovieCreditsUiState.Success(MovieCredits(director = "Director")), state.credits)
            assertEquals(movie, state.movie)
            assertEquals(MovieRecommendationsUiState.Empty, state.recommendations)
            coVerify(exactly = 1) { repository.getMovieDetails(1) }
            coVerify(exactly = 1) { repository.getMovieRecommendations(1) }
            coVerify(exactly = 2) { repository.getMovieCredits(1) }
        }

    @Test
    fun `recommendations retry reloads only recommendations and keeps the film`() =
        runTest {
            var recommendationCalls = 0
            val recommendation = movie.copy(id = 2, title = "Suggested")
            coEvery { repository.getMovieDetails(1) } returns movie
            coEvery { repository.getMovieCredits(1) } returns MovieCredits(director = "Director")
            coEvery { repository.getMovieRecommendations(1) } coAnswers {
                if (recommendationCalls++ == 0) throw IllegalStateException()
                listOf(recommendation)
            }
            val viewModel = MovieDetailViewModel(repository, libraryRepository)

            viewModel.loadMovieDetails(1)
            advanceUntilIdle()
            assertEquals(
                MovieRecommendationsUiState.Error,
                (viewModel.uiState.value as MovieDetailUiState.Success).recommendations,
            )

            viewModel.retryRecommendations()
            advanceUntilIdle()

            val state = viewModel.uiState.value as MovieDetailUiState.Success
            assertEquals(listOf(recommendation), (state.recommendations as MovieRecommendationsUiState.Success).movies)
            assertEquals(movie, state.movie)
            assertEquals(MovieCreditsUiState.Success(MovieCredits(director = "Director")), state.credits)
            coVerify(exactly = 1) { repository.getMovieDetails(1) }
            coVerify(exactly = 1) { repository.getMovieCredits(1) }
            coVerify(exactly = 2) { repository.getMovieRecommendations(1) }
        }

    @Test
    fun `section retries keep the initial requests while they are in flight`() =
        runTest {
            val creditsGate = CompletableDeferred<MovieCredits>()
            val recommendationsGate = CompletableDeferred<List<Movie>>()
            coEvery { repository.getMovieDetails(1) } returns movie
            coEvery { repository.getMovieCredits(1) } coAnswers { creditsGate.await() }
            coEvery { repository.getMovieRecommendations(1) } coAnswers { recommendationsGate.await() }
            val viewModel = MovieDetailViewModel(repository, libraryRepository)

            viewModel.loadMovieDetails(1)
            runCurrent()
            val loadingState = viewModel.uiState.value as MovieDetailUiState.Success
            assertEquals(MovieCreditsUiState.Loading, loadingState.credits)
            assertEquals(MovieRecommendationsUiState.Loading, loadingState.recommendations)

            viewModel.retryCredits()
            viewModel.retryRecommendations()
            runCurrent()

            val credits = MovieCredits(director = "Director")
            val recommendations = listOf(movie.copy(id = 2))
            creditsGate.complete(credits)
            recommendationsGate.complete(recommendations)
            advanceUntilIdle()

            coVerify(exactly = 1) { repository.getMovieCredits(1) }
            coVerify(exactly = 1) { repository.getMovieRecommendations(1) }
            val state = viewModel.uiState.value as MovieDetailUiState.Success
            assertEquals(MovieCreditsUiState.Success(credits), state.credits)
            assertEquals(recommendations, (state.recommendations as MovieRecommendationsUiState.Success).movies)
        }

    @Test
    fun `a duplicate section retry is ignored while one is in flight`() =
        runTest {
            var creditsCalls = 0
            val retryGate = CompletableDeferred<MovieCredits>()
            coEvery { repository.getMovieDetails(1) } returns movie
            coEvery { repository.getMovieCredits(1) } coAnswers {
                if (creditsCalls++ == 0) MovieCredits(director = "First") else retryGate.await()
            }
            val viewModel = MovieDetailViewModel(repository, libraryRepository)

            viewModel.loadMovieDetails(1)
            advanceUntilIdle()

            viewModel.retryCredits()
            runCurrent()
            viewModel.retryCredits()
            runCurrent()

            coVerify(exactly = 2) { repository.getMovieCredits(1) }

            retryGate.complete(MovieCredits(director = "Second"))
            advanceUntilIdle()
            assertEquals(
                MovieCreditsUiState.Success(MovieCredits(director = "Second")),
                (viewModel.uiState.value as MovieDetailUiState.Success).credits,
            )
        }

    @Test
    fun `library writes stay disabled until the observer reports ready`() =
        runTest {
            val libraryGate = CompletableDeferred<Unit>()
            every { libraryRepository.getLibraryMovies() } returns
                flow<List<Movie>> {
                    libraryGate.await()
                    emit(listOf(movie))
                }
            coEvery { repository.getMovieDetails(1) } returns movie
            coEvery { repository.getMovieCredits(1) } returns MovieCredits()
            coEvery { libraryRepository.setFavorite(any(), any()) } returns Unit
            val viewModel = MovieDetailViewModel(repository, libraryRepository)

            viewModel.loadMovieDetails(1)
            advanceUntilIdle()
            assertEquals(
                LibraryObservationUiState.Loading,
                (viewModel.uiState.value as MovieDetailUiState.Success).libraryObservation,
            )

            viewModel.toggleFavorite()
            advanceUntilIdle()
            coVerify(exactly = 0) { libraryRepository.setFavorite(any(), any()) }
            assertFalse((viewModel.uiState.value as MovieDetailUiState.Success).isLibraryActionPending)

            libraryGate.complete(Unit)
            advanceUntilIdle()
            assertEquals(
                LibraryObservationUiState.Ready,
                (viewModel.uiState.value as MovieDetailUiState.Success).libraryObservation,
            )

            viewModel.toggleFavorite()
            advanceUntilIdle()
            coVerify(exactly = 1) { libraryRepository.setFavorite(movie, true) }
        }

    @Test
    fun `a library observer failing before its first snapshot keeps repository flags`() =
        runTest {
            val recommendation = movie.copy(id = 2, title = "Suggested", isFavorite = true)
            every { libraryRepository.getLibraryMovies() } returns
                flow<List<Movie>> {
                    throw IllegalStateException("library read failed")
                }
            coEvery { repository.getMovieDetails(1) } returns movie
            coEvery { repository.getMovieCredits(1) } returns MovieCredits()
            coEvery { repository.getMovieRecommendations(1) } returns listOf(recommendation)
            coEvery { libraryRepository.setFavorite(any(), any()) } returns Unit
            val viewModel = MovieDetailViewModel(repository, libraryRepository)

            viewModel.loadMovieDetails(1)
            advanceUntilIdle()

            val failed = viewModel.uiState.value as MovieDetailUiState.Success
            assertEquals(LibraryObservationUiState.Error, failed.libraryObservation)
            assertTrue((failed.recommendations as MovieRecommendationsUiState.Success).movies.single().isFavorite)

            viewModel.toggleFavorite()
            advanceUntilIdle()
            coVerify(exactly = 0) { libraryRepository.setFavorite(any(), any()) }

            // A genuine, successful empty snapshot is authoritative and still clears flags.
            every { libraryRepository.getLibraryMovies() } returns flowOf(emptyList())
            viewModel.retryLibraryObservation()
            advanceUntilIdle()

            val recovered = viewModel.uiState.value as MovieDetailUiState.Success
            assertEquals(LibraryObservationUiState.Ready, recovered.libraryObservation)
            assertFalse((recovered.recommendations as MovieRecommendationsUiState.Success).movies.single().isFavorite)
        }

    @Test
    fun `a failed library snapshot keeps the last flags and blocks writes until retry`() =
        runTest {
            every { libraryRepository.getLibraryMovies() } returns
                flow<List<Movie>> {
                    emit(listOf(movie.copy(isFavorite = true)))
                    throw IllegalStateException("library read failed")
                }
            coEvery { repository.getMovieDetails(1) } returns movie
            coEvery { repository.getMovieCredits(1) } returns MovieCredits()
            coEvery { libraryRepository.setFavorite(any(), any()) } returns Unit
            val viewModel = MovieDetailViewModel(repository, libraryRepository)

            viewModel.loadMovieDetails(1)
            advanceUntilIdle()

            val failed = viewModel.uiState.value as MovieDetailUiState.Success
            assertEquals(LibraryObservationUiState.Error, failed.libraryObservation)
            assertTrue(failed.movie.isFavorite)

            viewModel.toggleFavorite()
            advanceUntilIdle()
            val blocked = viewModel.uiState.value as MovieDetailUiState.Success
            assertTrue(blocked.movie.isFavorite)
            assertFalse(blocked.isLibraryActionPending)
            coVerify(exactly = 0) { libraryRepository.setFavorite(any(), any()) }

            every { libraryRepository.getLibraryMovies() } returns flowOf(emptyList())
            viewModel.retryLibraryObservation()
            advanceUntilIdle()

            val recovered = viewModel.uiState.value as MovieDetailUiState.Success
            assertEquals(LibraryObservationUiState.Ready, recovered.libraryObservation)
            assertFalse(recovered.movie.isFavorite)

            viewModel.toggleFavorite()
            advanceUntilIdle()
            coVerify(exactly = 1) { libraryRepository.setFavorite(movie, true) }
        }

    @Test
    fun `a cancelled section does not report an error`() =
        runTest {
            coEvery { repository.getMovieDetails(1) } returns movie
            coEvery { repository.getMovieCredits(1) } coAnswers { throw CancellationException("cancelled") }
            val viewModel = MovieDetailViewModel(repository, libraryRepository)

            viewModel.loadMovieDetails(1)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state !is MovieDetailUiState.Error)
            assertTrue((state as? MovieDetailUiState.Success)?.credits != MovieCreditsUiState.Error)
        }
}
