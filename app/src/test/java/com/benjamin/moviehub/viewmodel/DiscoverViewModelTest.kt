package com.benjamin.moviehub.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.paging.PagingData
import com.benjamin.moviehub.domain.model.DiscoverFilters
import com.benjamin.moviehub.domain.model.DiscoverSortOption
import com.benjamin.moviehub.domain.model.MovieGenre
import com.benjamin.moviehub.domain.repository.MovieRepository
import com.benjamin.moviehub.ui.discover.DiscoverViewModel
import com.benjamin.moviehub.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class DiscoverViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: MovieRepository = mockk()
    private lateinit var viewModel: DiscoverViewModel

    @Before
    fun setup() {
        every { repository.getDiscoverMovies(any()) } returns flowOf(PagingData.empty())
        coEvery {
            repository.getMovieGenres()
        } returns listOf(MovieGenre(id = 28, name = "Action"), MovieGenre(id = 18, name = "Drame"))
        viewModel = DiscoverViewModel(repository, SavedStateHandle())
    }

    @Test
    fun `genres load and default filters start empty on popularity`() =
        runTest {
            advanceUntilIdle()

            assertEquals(false, viewModel.uiState.value.isLoadingGenres)
            assertEquals(false, viewModel.uiState.value.hasGenreError)
            assertEquals(
                listOf(MovieGenre(id = 28, name = "Action"), MovieGenre(id = 18, name = "Drame")),
                viewModel.uiState.value.genres,
            )
            assertEquals(DiscoverFilters(), viewModel.uiState.value.draftFilters)
            assertEquals(DiscoverFilters(), viewModel.uiState.value.appliedFilters)
        }

    @Test
    fun `genres error is exposed and retry can recover`() =
        runTest {
            var shouldFail = true
            coEvery {
                repository.getMovieGenres()
            } coAnswers {
                if (shouldFail) throw IOException("offline")
                listOf(MovieGenre(id = 28, name = "Action"))
            }

            viewModel.retryGenres()
            advanceUntilIdle()

            assertEquals(false, viewModel.uiState.value.isLoadingGenres)
            assertEquals(true, viewModel.uiState.value.hasGenreError)

            shouldFail = false
            viewModel.retryGenres()
            advanceUntilIdle()

            assertEquals(false, viewModel.uiState.value.hasGenreError)
            assertEquals(listOf(MovieGenre(id = 28, name = "Action")), viewModel.uiState.value.genres)
        }

    @Test
    fun `genre retries keep the initial request while it is in flight`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val localRepository: MovieRepository = mockk()
            val genresGate = CompletableDeferred<List<MovieGenre>>()
            var cancellations = 0
            coEvery { localRepository.getMovieGenres() } coAnswers {
                try {
                    genresGate.await()
                } catch (e: CancellationException) {
                    cancellations++
                    throw e
                }
            }
            val viewModel = DiscoverViewModel(localRepository, SavedStateHandle())

            // Ignore duplicate triggers before the initial coroutine starts as well.
            viewModel.retryGenres()
            runCurrent()
            viewModel.retryGenres()
            runCurrent()
            viewModel.retryGenres()
            runCurrent()

            val genres = listOf(MovieGenre(id = 18, name = "Drame"))
            genresGate.complete(genres)
            advanceUntilIdle()

            coVerify(exactly = 1) { localRepository.getMovieGenres() }
            assertEquals(0, cancellations)
            assertEquals(genres, viewModel.uiState.value.genres)
            assertEquals(false, viewModel.uiState.value.isLoadingGenres)
            assertEquals(false, viewModel.uiState.value.hasGenreError)
        }

    @Test
    fun `editing filters does not request until apply and then starts a new query`() =
        runTest {
            val job = launch { viewModel.discoverResults.collect() }
            advanceUntilIdle()
            val filters =
                DiscoverFilters(
                    genreId = 28,
                    releaseYear = 2020,
                    minimumVoteAverage = 8.0,
                    sort = DiscoverSortOption.RELEASE_DATE,
                )

            viewModel.onGenreSelected(filters.genreId)
            viewModel.onReleaseYearSelected(filters.releaseYear)
            viewModel.onMinimumRatingSelected(filters.minimumVoteAverage)
            viewModel.onSortSelected(filters.sort)
            advanceUntilIdle()

            assertEquals(DiscoverFilters(), viewModel.uiState.value.appliedFilters)
            verify(exactly = 1) { repository.getDiscoverMovies(DiscoverFilters()) }

            viewModel.applyFilters()
            advanceUntilIdle()

            assertEquals(filters, viewModel.uiState.value.appliedFilters)
            verify { repository.getDiscoverMovies(filters) }
            job.cancel()
        }

    @Test
    fun `applying new filters cancels the previous discover flow`() =
        runTest {
            val initialFilters = DiscoverFilters()
            val appliedFilters = DiscoverFilters(genreId = 28)
            val initialFlowCancelled = CompletableDeferred<Unit>()

            every { repository.getDiscoverMovies(initialFilters) } returns
                flow {
                    try {
                        emit(PagingData.empty())
                        awaitCancellation()
                    } finally {
                        initialFlowCancelled.complete(Unit)
                    }
                }
            every { repository.getDiscoverMovies(appliedFilters) } returns flowOf(PagingData.empty())

            val job = launch { viewModel.discoverResults.collect() }
            advanceUntilIdle()

            viewModel.onGenreSelected(appliedFilters.genreId)
            viewModel.applyFilters()
            advanceUntilIdle()

            assertTrue(initialFlowCancelled.isCompleted)
            verify(exactly = 1) { repository.getDiscoverMovies(initialFilters) }
            verify(exactly = 1) { repository.getDiscoverMovies(appliedFilters) }
            job.cancel()
        }

    @Test
    fun `recollecting discover results for the same filters reuses the cached pipeline`() =
        runTest {
            val firstJob = launch { viewModel.discoverResults.collect() }
            advanceUntilIdle()
            firstJob.cancel()

            val secondJob = launch { viewModel.discoverResults.collect() }
            advanceUntilIdle()

            verify(exactly = 1) { repository.getDiscoverMovies(DiscoverFilters()) }
            secondJob.cancel()
        }

    @Test
    fun `reset restores and applies draft and applied defaults`() =
        runTest {
            val job = launch { viewModel.discoverResults.collect() }
            advanceUntilIdle()

            viewModel.onGenreSelected(28)
            viewModel.onReleaseYearSelected(2020)
            viewModel.onSortSelected(DiscoverSortOption.RATING)
            viewModel.applyFilters()
            viewModel.onMinimumRatingSelected(8.0)
            advanceUntilIdle()

            viewModel.resetFilters()
            advanceUntilIdle()

            assertEquals(DiscoverFilters(), viewModel.uiState.value.draftFilters)
            assertEquals(DiscoverFilters(), viewModel.uiState.value.appliedFilters)
            verify(exactly = 2) { repository.getDiscoverMovies(DiscoverFilters()) }
            job.cancel()
        }

    @Test
    fun `genres load in flight keeps filter edits and requests genres once`() =
        runTest {
            val localRepository: MovieRepository = mockk()
            every { localRepository.getDiscoverMovies(any()) } returns flowOf(PagingData.empty())
            val genresGate = CompletableDeferred<List<MovieGenre>>()
            coEvery { localRepository.getMovieGenres() } coAnswers { genresGate.await() }

            val localViewModel = DiscoverViewModel(localRepository, SavedStateHandle())
            advanceUntilIdle()

            localViewModel.onGenreSelected(28)
            localViewModel.onReleaseYearSelected(2020)
            advanceUntilIdle()

            assertEquals(true, localViewModel.uiState.value.isLoadingGenres)
            assertEquals(emptyList<MovieGenre>(), localViewModel.uiState.value.genres)

            genresGate.complete(listOf(MovieGenre(id = 28, name = "Action")))
            advanceUntilIdle()

            assertEquals(false, localViewModel.uiState.value.isLoadingGenres)
            assertEquals(listOf(MovieGenre(id = 28, name = "Action")), localViewModel.uiState.value.genres)
            assertEquals(28, localViewModel.uiState.value.draftFilters.genreId)
            assertEquals(2020, localViewModel.uiState.value.draftFilters.releaseYear)
            coVerify(exactly = 1) { localRepository.getMovieGenres() }
        }

    @Test
    fun `draft and applied filters are restored independently from primitive saved values`() =
        runTest {
            val handle = SavedStateHandle()
            val first = DiscoverViewModel(repository, handle)
            advanceUntilIdle()

            first.onGenreSelected(28)
            first.onReleaseYearSelected(2020)
            first.onMinimumRatingSelected(8.0)
            first.onSortSelected(DiscoverSortOption.RELEASE_DATE)
            first.applyFilters()
            first.onGenreSelected(18)
            advanceUntilIdle()

            assertTrue(handle.keys().isNotEmpty())
            val savedValues = handle.keys().map { key -> handle.get<Any?>(key) }
            assertTrue(
                savedValues.all { value -> value == null || value is Int || value is Double || value is String },
            )

            val restoredHandle = SavedStateHandle(handle.keys().associateWith { key -> handle.get<Any?>(key) })
            val restored = DiscoverViewModel(repository, restoredHandle)
            advanceUntilIdle()

            assertEquals(
                DiscoverFilters(
                    genreId = 18,
                    releaseYear = 2020,
                    minimumVoteAverage = 8.0,
                    sort = DiscoverSortOption.RELEASE_DATE,
                ),
                restored.uiState.value.draftFilters,
            )
            assertEquals(
                DiscoverFilters(
                    genreId = 28,
                    releaseYear = 2020,
                    minimumVoteAverage = 8.0,
                    sort = DiscoverSortOption.RELEASE_DATE,
                ),
                restored.uiState.value.appliedFilters,
            )
        }
}
