package com.benjamin.moviehub.viewmodel

import androidx.paging.PagingData
import com.benjamin.moviehub.domain.model.DiscoverFilters
import com.benjamin.moviehub.domain.model.DiscoverSortOption
import com.benjamin.moviehub.domain.model.MovieGenre
import com.benjamin.moviehub.domain.repository.MovieRepository
import com.benjamin.moviehub.ui.discover.DiscoverViewModel
import com.benjamin.moviehub.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.verify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

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
        viewModel = DiscoverViewModel(repository)
    }

    @Test
    fun `genres success clears loading and exposes genres`() =
        runTest {
            advanceUntilIdle()

            assertEquals(false, viewModel.uiState.value.isLoadingGenres)
            assertEquals(false, viewModel.uiState.value.hasGenreError)
            assertEquals(
                listOf(MovieGenre(id = 28, name = "Action"), MovieGenre(id = 18, name = "Drame")),
                viewModel.uiState.value.genres,
            )
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

            assertEquals(false, viewModel.uiState.value.isLoadingGenres)
            assertEquals(false, viewModel.uiState.value.hasGenreError)
            assertEquals(listOf(MovieGenre(id = 28, name = "Action")), viewModel.uiState.value.genres)
        }

    @Test
    fun `default filters are empty and sorted by popularity`() {
        val defaults = DiscoverFilters()

        assertEquals(defaults, viewModel.uiState.value.draftFilters)
        assertEquals(defaults, viewModel.uiState.value.appliedFilters)
        assertEquals(DiscoverSortOption.POPULARITY, defaults.sort)
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
    fun `opening editor starts from applied filters and dismissing discards draft edits`() {
        viewModel.onGenreSelected(28)
        viewModel.applyFilters()

        viewModel.onReleaseYearSelected(2020)
        viewModel.beginFilterEditing()
        assertEquals(viewModel.uiState.value.appliedFilters, viewModel.uiState.value.draftFilters)

        viewModel.onMinimumRatingSelected(8.0)
        viewModel.discardFilterEdits()

        assertEquals(viewModel.uiState.value.appliedFilters, viewModel.uiState.value.draftFilters)
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
    fun `supported sort options expose their TMDB values`() {
        assertEquals(
            listOf("popularity.desc", "vote_average.desc", "primary_release_date.desc"),
            DiscoverSortOption.entries.map(DiscoverSortOption::queryValue),
        )
    }
}
