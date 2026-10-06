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
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
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
