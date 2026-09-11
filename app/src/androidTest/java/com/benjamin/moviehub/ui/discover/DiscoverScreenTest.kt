package com.benjamin.moviehub.ui.discover

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.swipeUp
import androidx.paging.PagingData
import com.benjamin.moviehub.core.theme.MovieHubTheme
import com.benjamin.moviehub.domain.model.DiscoverFilters
import com.benjamin.moviehub.domain.model.DiscoverSortOption
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.model.MovieGenre
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DiscoverScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun filterSheetAppliesGenreAndCloses() {
        var state by mutableStateOf(initialState())
        var applyCalled = false

        composeRule.setContent {
            MovieHubTheme {
                DiscoverScreen(
                    state = state,
                    discoverResults = flowOf(PagingData.from(listOf(testMovie()))),
                    onGenreSelected = { genreId ->
                        state = state.copy(draftFilters = state.draftFilters.copy(genreId = genreId))
                    },
                    onReleaseYearSelected = {},
                    onMinimumRatingSelected = {},
                    onSortSelected = {},
                    onBeginFilterEditing = { state = state.copy(draftFilters = state.appliedFilters) },
                    onApplyFilters = {
                        state = state.copy(appliedFilters = state.draftFilters)
                        applyCalled = true
                    },
                    onResetFilters = {},
                    onDiscardFilterEdits = { state = state.copy(draftFilters = state.appliedFilters) },
                    onRetryGenres = {},
                    onMovieClick = {},
                )
            }
        }

        composeRule.onNodeWithTag("discover_filter_button").performClick()
        composeRule.onNodeWithTag("discover_filter_sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("discover_genre_section").performClick()
        composeRule.onNodeWithTag("discover_genre_option_28").performClick()
        composeRule.onNodeWithTag("discover_apply_filters").performClick()

        composeRule.runOnIdle {
            assertTrue(applyCalled)
            assertEquals(28, state.appliedFilters.genreId)
        }
        composeRule.onAllNodesWithTag("discover_filter_sheet").assertCountEquals(0)
    }

    @Test
    fun selectingOtherYearKeepsDraftSeparateFromAppliedFilters() {
        var state by mutableStateOf(initialState())

        composeRule.setContent {
            MovieHubTheme {
                DiscoverScreen(
                    state = state,
                    discoverResults = flowOf(PagingData.empty()),
                    onGenreSelected = {},
                    onReleaseYearSelected = { year ->
                        state = state.copy(draftFilters = state.draftFilters.copy(releaseYear = year))
                    },
                    onMinimumRatingSelected = {},
                    onSortSelected = {},
                    onBeginFilterEditing = { state = state.copy(draftFilters = state.appliedFilters) },
                    onApplyFilters = {},
                    onResetFilters = {},
                    onDiscardFilterEdits = {},
                    onRetryGenres = {},
                    onMovieClick = {},
                )
            }
        }

        composeRule.onNodeWithTag("discover_filter_button").performClick()
        composeRule.onNodeWithTag("discover_year_section").performClick()
        composeRule.onNodeWithTag("discover_year_option_other").performClick()

        composeRule.runOnIdle {
            assertEquals(null, state.draftFilters.releaseYear)
            assertEquals(DiscoverFilters(), state.appliedFilters)
        }
    }

    @Test
    fun activeFilterCountIsDisplayed() {
        composeRule.setContent {
            MovieHubTheme {
                DiscoverScreen(
                    state =
                        initialState().copy(
                            appliedFilters = DiscoverFilters(genreId = 28, releaseYear = 2020),
                        ),
                    discoverResults = flowOf(PagingData.empty()),
                    onGenreSelected = {},
                    onReleaseYearSelected = {},
                    onMinimumRatingSelected = {},
                    onSortSelected = {},
                    onBeginFilterEditing = {},
                    onApplyFilters = {},
                    onResetFilters = {},
                    onDiscardFilterEdits = {},
                    onRetryGenres = {},
                    onMovieClick = {},
                )
            }
        }

        composeRule.onNodeWithTag("discover_filter_count").assertIsDisplayed()
        composeRule.onNodeWithTag("discover_active_filter_summary").assertIsDisplayed()
    }

    @Test
    fun applyIsDisabledWhenDraftMatchesAppliedFilters() {
        composeRule.setContent {
            MovieHubTheme {
                DiscoverScreen(
                    state = initialState(),
                    discoverResults = flowOf(PagingData.empty()),
                    onGenreSelected = {},
                    onReleaseYearSelected = {},
                    onMinimumRatingSelected = {},
                    onSortSelected = {},
                    onBeginFilterEditing = {},
                    onApplyFilters = {},
                    onResetFilters = {},
                    onDiscardFilterEdits = {},
                    onRetryGenres = {},
                    onMovieClick = {},
                )
            }
        }

        composeRule.onNodeWithTag("discover_filter_button").performClick()
        composeRule.onNodeWithTag("discover_apply_filters").assertIsNotEnabled()
    }

    @Test
    fun closingSheetDiscardsDraftChanges() {
        var state by mutableStateOf(initialState())

        composeRule.setContent {
            MovieHubTheme {
                DiscoverScreen(
                    state = state,
                    discoverResults = flowOf(PagingData.empty()),
                    onGenreSelected = { genreId ->
                        state = state.copy(draftFilters = state.draftFilters.copy(genreId = genreId))
                    },
                    onReleaseYearSelected = {},
                    onMinimumRatingSelected = {},
                    onSortSelected = {},
                    onBeginFilterEditing = { state = state.copy(draftFilters = state.appliedFilters) },
                    onApplyFilters = {},
                    onResetFilters = {},
                    onDiscardFilterEdits = { state = state.copy(draftFilters = state.appliedFilters) },
                    onRetryGenres = {},
                    onMovieClick = {},
                )
            }
        }

        composeRule.onNodeWithTag("discover_filter_button").performClick()
        composeRule.onNodeWithTag("discover_genre_section").performClick()
        composeRule.onNodeWithTag("discover_genre_option_28").performClick()
        composeRule.onNodeWithTag("discover_close_filters").performClick()

        composeRule.runOnIdle {
            assertEquals(null, state.draftFilters.genreId)
        }
        composeRule.onAllNodesWithTag("discover_filter_sheet").assertCountEquals(0)
    }

    @Test
    fun resetRestoresDefaultsAndClosesSheet() {
        var state = initialState().copy(
            draftFilters = DiscoverFilters(genreId = 28, releaseYear = 2020, minimumVoteAverage = 8.0, sort = DiscoverSortOption.RATING),
            appliedFilters = DiscoverFilters(genreId = 28, releaseYear = 2020, minimumVoteAverage = 8.0, sort = DiscoverSortOption.RATING),
        )
        var resetCalled = false

        composeRule.setContent {
            MovieHubTheme {
                DiscoverScreen(
                    state = state,
                    discoverResults = flowOf(PagingData.empty()),
                    onGenreSelected = {},
                    onReleaseYearSelected = {},
                    onMinimumRatingSelected = {},
                    onSortSelected = {},
                    onBeginFilterEditing = {},
                    onApplyFilters = {},
                    onResetFilters = {
                        state = state.copy(draftFilters = DiscoverFilters(), appliedFilters = DiscoverFilters())
                        resetCalled = true
                    },
                    onDiscardFilterEdits = {},
                    onRetryGenres = {},
                    onMovieClick = {},
                )
            }
        }

        composeRule.onNodeWithTag("discover_filter_button").performClick()
        composeRule.onNodeWithTag("discover_reset_filters").performClick()

        composeRule.runOnIdle {
            assertTrue(resetCalled)
            assertEquals(DiscoverFilters(), state.appliedFilters)
            assertEquals(DiscoverFilters(), state.draftFilters)
        }
        composeRule.onAllNodesWithTag("discover_filter_sheet").assertCountEquals(0)
    }

    private fun initialState() =
        DiscoverUiState(
            genres = listOf(MovieGenre(id = 28, name = "Action")),
            isLoadingGenres = false,
        )

    private fun testMovie() =
        Movie(
            id = 1,
            title = "Test movie",
            overview = "Overview",
            posterPath = null,
            backdropPath = null,
            voteAverage = 7.5,
            releaseDate = "2024-01-01",
            webUrl = null,
            isFavorite = false,
            genreIds = emptyList(),
            genres = emptyList(),
        )
}
