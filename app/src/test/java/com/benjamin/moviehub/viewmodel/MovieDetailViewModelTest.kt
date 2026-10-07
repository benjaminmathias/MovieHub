package com.benjamin.moviehub.viewmodel

import com.benjamin.moviehub.domain.model.LibraryFlag
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.model.MovieCredits
import com.benjamin.moviehub.domain.repository.LibraryRepository
import com.benjamin.moviehub.domain.repository.MovieRepository
import com.benjamin.moviehub.ui.detail.LibraryObservationErrorCode
import com.benjamin.moviehub.ui.detail.LibraryObservationUiState
import com.benjamin.moviehub.ui.detail.MovieCreditsErrorCode
import com.benjamin.moviehub.ui.detail.MovieCreditsUiState
import com.benjamin.moviehub.ui.detail.MovieDetailActionErrorCode
import com.benjamin.moviehub.ui.detail.MovieDetailActionErrorState
import com.benjamin.moviehub.ui.detail.MovieDetailUiState
import com.benjamin.moviehub.ui.detail.MovieDetailViewModel
import com.benjamin.moviehub.ui.detail.MovieRecommendationsErrorCode
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
                    credits = MovieCreditsUiState.Error(MovieCreditsErrorCode.LOAD_CREDITS),
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
            coEvery { libraryRepository.setLibraryFlag(any(), LibraryFlag.WATCHED, any()) } returns Unit
            coEvery { libraryRepository.setLibraryFlag(any(), LibraryFlag.WATCHLIST, any()) } returns Unit
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
    fun `favorite update restores previous state when repository fails`() =
        runTest {
            coEvery { repository.getMovieDetails(1) } returns movie
            coEvery { repository.getMovieCredits(1) } returns MovieCredits()
            val failureGate = CompletableDeferred<Unit>()
            coEvery { libraryRepository.setLibraryFlag(movie, LibraryFlag.FAVORITE, true) } coAnswers {
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
            coVerify(exactly = 1) { libraryRepository.setLibraryFlag(movie, LibraryFlag.FAVORITE, true) }
        }

    @Test
    fun `double toggle uses latest state and does not rollback newer transition`() =
        runTest {
            coEvery { repository.getMovieDetails(1) } returns movie
            coEvery { repository.getMovieCredits(1) } returns MovieCredits()
            coEvery { libraryRepository.setLibraryFlag(any(), any(), any()) } returns Unit
            val viewModel = MovieDetailViewModel(repository, libraryRepository)
            viewModel.loadMovieDetails(1)
            advanceUntilIdle()

            viewModel.toggleFavorite()
            viewModel.toggleFavorite()
            advanceUntilIdle()

            assertTrue(!(viewModel.uiState.value as MovieDetailUiState.Success).movie.isFavorite)
            coVerify(exactly = 1) { libraryRepository.setLibraryFlag(movie, LibraryFlag.FAVORITE, true) }
            coVerify(exactly = 1) { libraryRepository.setLibraryFlag(movie.copy(isFavorite = true), LibraryFlag.FAVORITE, false) }
        }

    @Test
    fun `late credits preserve an optimistic favorite`() =
        runTest {
            val creditsGate = CompletableDeferred<MovieCredits>()
            coEvery { repository.getMovieDetails(1) } returns movie
            coEvery { repository.getMovieCredits(1) } coAnswers { creditsGate.await() }
            coEvery { libraryRepository.setLibraryFlag(movie, LibraryFlag.FAVORITE, true) } returns Unit
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
    fun `failed library action keeps one pending error until acknowledged`() =
        runTest {
            coEvery { repository.getMovieDetails(1) } returns movie
            coEvery { repository.getMovieCredits(1) } returns MovieCredits()
            coEvery { libraryRepository.setLibraryFlag(movie, LibraryFlag.WATCHED, true) } throws IllegalStateException()
            val viewModel = MovieDetailViewModel(repository, libraryRepository)

            viewModel.loadMovieDetails(1)
            advanceUntilIdle()
            viewModel.toggleWatched()
            advanceUntilIdle()

            // The error is a state: it survives until the UI acknowledges it.
            assertEquals(
                MovieDetailActionErrorState.Failure(MovieDetailActionErrorCode.UPDATE_LIBRARY),
                viewModel.libraryActionError.value,
            )

            viewModel.acknowledgeLibraryActionError()
            assertEquals(MovieDetailActionErrorState.None, viewModel.libraryActionError.value)
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
                MovieRecommendationsUiState.Error(MovieRecommendationsErrorCode.LOAD_RECOMMENDATIONS),
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
            coEvery { libraryRepository.setLibraryFlag(any(), any(), any()) } returns Unit
            val viewModel = MovieDetailViewModel(repository, libraryRepository)

            viewModel.loadMovieDetails(1)
            advanceUntilIdle()
            assertEquals(
                LibraryObservationUiState.Loading,
                (viewModel.uiState.value as MovieDetailUiState.Success).libraryObservation,
            )

            viewModel.toggleFavorite()
            advanceUntilIdle()
            coVerify(exactly = 0) { libraryRepository.setLibraryFlag(any(), any(), any()) }
            assertFalse((viewModel.uiState.value as MovieDetailUiState.Success).isLibraryActionPending)

            libraryGate.complete(Unit)
            advanceUntilIdle()
            assertEquals(
                LibraryObservationUiState.Ready,
                (viewModel.uiState.value as MovieDetailUiState.Success).libraryObservation,
            )

            viewModel.toggleFavorite()
            advanceUntilIdle()
            coVerify(exactly = 1) { libraryRepository.setLibraryFlag(movie, LibraryFlag.FAVORITE, true) }
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
            coEvery { libraryRepository.setLibraryFlag(any(), any(), any()) } returns Unit
            val viewModel = MovieDetailViewModel(repository, libraryRepository)

            viewModel.loadMovieDetails(1)
            advanceUntilIdle()

            val failed = viewModel.uiState.value as MovieDetailUiState.Success
            assertEquals(
                LibraryObservationUiState.Error(LibraryObservationErrorCode.LOAD_LIBRARY),
                failed.libraryObservation,
            )
            assertTrue(failed.movie.isFavorite)

            viewModel.toggleFavorite()
            advanceUntilIdle()
            val blocked = viewModel.uiState.value as MovieDetailUiState.Success
            assertTrue(blocked.movie.isFavorite)
            assertFalse(blocked.isLibraryActionPending)
            coVerify(exactly = 0) { libraryRepository.setLibraryFlag(any(), any(), any()) }

            every { libraryRepository.getLibraryMovies() } returns flowOf(emptyList())
            viewModel.retryLibraryObservation()
            advanceUntilIdle()

            val recovered = viewModel.uiState.value as MovieDetailUiState.Success
            assertEquals(LibraryObservationUiState.Ready, recovered.libraryObservation)
            assertFalse(recovered.movie.isFavorite)

            viewModel.toggleFavorite()
            advanceUntilIdle()
            coVerify(exactly = 1) { libraryRepository.setLibraryFlag(movie, LibraryFlag.FAVORITE, true) }
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
            assertTrue((state as? MovieDetailUiState.Success)?.credits !is MovieCreditsUiState.Error)
        }
}
