package com.benjamin.moviehub.ui.discover

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
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
import java.util.Calendar

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
        composeRule.onNodeWithTag("discover_genre_row").performClick()
        composeRule.onNodeWithTag("discover_genre_option_28").performScrollTo().performClick()
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
        composeRule.onNodeWithTag("discover_year_row").performClick()
        composeRule.onNodeWithTag("discover_year_option_other").performScrollTo().performClick()

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
    fun invalidCustomYearsDisableApply() {
        var state by mutableStateOf(initialState())
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

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
        composeRule.onNodeWithTag("discover_year_row").performClick()
        composeRule.onNodeWithTag("discover_year_option_other").performScrollTo().performClick()
        val yearField = composeRule.onNodeWithTag("discover_custom_year")

        yearField.performTextInput("202")
        composeRule.onNodeWithTag("discover_apply_filters").assertIsNotEnabled()
        yearField.performTextClearance()
        yearField.performTextInput((currentYear + 1).toString())
        composeRule.onNodeWithTag("discover_apply_filters").assertIsNotEnabled()
        yearField.performTextClearance()
        yearField.performTextInput("1869")
        composeRule.onNodeWithTag("discover_apply_filters").assertIsNotEnabled()
    }

    @Test
    fun validCustomYearUpdatesDraftAndEnablesApply() {
        var state by mutableStateOf(initialState())
        val validYear = Calendar.getInstance().get(Calendar.YEAR) - 1

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
        composeRule.onNodeWithTag("discover_year_row").performClick()
        composeRule.onNodeWithTag("discover_year_option_other").performScrollTo().performClick()
        composeRule.onNodeWithTag("discover_custom_year").performScrollTo().performTextInput(validYear.toString())

        composeRule.runOnIdle {
            assertEquals(validYear, state.draftFilters.releaseYear)
        }
        composeRule.onNodeWithTag("discover_apply_filters").assertIsEnabled()
    }

    @Test
    fun genreErrorShowsRetryAndInvokesCallback() {
        var retryCalled = false

        composeRule.setContent {
            MovieHubTheme {
                DiscoverScreen(
                    state = initialState().copy(genres = emptyList(), hasGenreError = true),
                    discoverResults = flowOf(PagingData.empty()),
                    onGenreSelected = {},
                    onReleaseYearSelected = {},
                    onMinimumRatingSelected = {},
                    onSortSelected = {},
                    onBeginFilterEditing = {},
                    onApplyFilters = {},
                    onResetFilters = {},
                    onDiscardFilterEdits = {},
                    onRetryGenres = { retryCalled = true },
                    onMovieClick = {},
                )
            }
        }

        composeRule.onNodeWithTag("discover_filter_button").performClick()
        composeRule.onNodeWithTag("discover_genre_row").performClick()
        composeRule.onNodeWithTag("discover_retry_genres").assertIsDisplayed().performClick()

        composeRule.runOnIdle { assertTrue(retryCalled) }
    }

    @Test
    fun initialPagingErrorShowsRetry() {
        composeRule.setContent {
            MovieHubTheme {
                DiscoverScreen(
                    state = initialState(),
                    discoverResults = Pager(PagingConfig(pageSize = 1)) { ErrorPagingSource() }.flow,
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

        composeRule.onNodeWithText("Impossible de charger les films.").assertIsDisplayed()
        composeRule.onNodeWithText("Réessayer").assertIsDisplayed().performClick()
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
        composeRule.onNodeWithTag("discover_genre_row").performClick()
        composeRule.onNodeWithTag("discover_genre_option_28").performScrollTo().performClick()
        composeRule.onNodeWithTag("discover_close_filters").performClick()

        composeRule.runOnIdle {
            assertEquals(null, state.draftFilters.genreId)
        }
        composeRule.onAllNodesWithTag("discover_filter_sheet").assertCountEquals(0)
    }

    @Test
    fun resetRestoresDefaultsAndClosesSheet() {
        var state =
            initialState().copy(
                draftFilters =
                    DiscoverFilters(
                        genreId = 28,
                        releaseYear = 2020,
                        minimumVoteAverage = 8.0,
                        sort = DiscoverSortOption.RATING,
                    ),
                appliedFilters =
                    DiscoverFilters(
                        genreId = 28,
                        releaseYear = 2020,
                        minimumVoteAverage = 8.0,
                        sort = DiscoverSortOption.RATING,
                    ),
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

    @Test
    fun filterButtonIsIconOnlyAccessibleWithLargeTouchTarget() {
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

        composeRule
            .onNodeWithContentDescription("Filtres")
            .assertIsDisplayed()
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithTag("discover_filter_button").assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun retryGenreActionUsesSharedRetryStyleText() {
        composeRule.setContent {
            MovieHubTheme {
                DiscoverScreen(
                    state = initialState().copy(genres = emptyList(), hasGenreError = true),
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
        composeRule.onNodeWithTag("discover_genre_row").performClick()
        composeRule.onNodeWithTag("discover_retry_genres").assertIsDisplayed().assertHasClickAction()
        composeRule.onNodeWithText("Réessayer").assertIsDisplayed()
    }

    @Test
    fun selectingSortOptionUpdatesDraft() {
        var state by mutableStateOf(initialState())

        composeRule.setContent {
            MovieHubTheme {
                DiscoverScreen(
                    state = state,
                    discoverResults = flowOf(PagingData.empty()),
                    onGenreSelected = {},
                    onReleaseYearSelected = {},
                    onMinimumRatingSelected = {},
                    onSortSelected = { sort ->
                        state = state.copy(draftFilters = state.draftFilters.copy(sort = sort))
                    },
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
        composeRule.onNodeWithTag("discover_sort_option_RATING").performClick()

        composeRule.runOnIdle {
            assertEquals(DiscoverSortOption.RATING, state.draftFilters.sort)
        }
    }

    @Test
    fun enablingRatingFilterUpdatesDraft() {
        var state by mutableStateOf(initialState())

        composeRule.setContent {
            MovieHubTheme {
                DiscoverScreen(
                    state = state,
                    discoverResults = flowOf(PagingData.empty()),
                    onGenreSelected = {},
                    onReleaseYearSelected = {},
                    onMinimumRatingSelected = { rating ->
                        state = state.copy(draftFilters = state.draftFilters.copy(minimumVoteAverage = rating))
                    },
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
        composeRule.onNodeWithTag("discover_rating_switch").performScrollTo().performClick()
        composeRule
            .onNodeWithTag("discover_rating_slider")
            .performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(9f) }

        composeRule.runOnIdle {
            assertEquals(9.0, state.draftFilters.minimumVoteAverage)
        }
    }

    @Test
    fun subViewBackButtonReturnsToMainList() {
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
        composeRule.onNodeWithTag("discover_genre_row").performClick()
        composeRule.onNodeWithTag("discover_genre_option_all").assertIsDisplayed()
        composeRule.onNodeWithTag("discover_filter_back").performClick()
        composeRule.onNodeWithTag("discover_genre_row").assertIsDisplayed()
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

    private class ErrorPagingSource : PagingSource<Int, Movie>() {
        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Movie> = LoadResult.Error(IllegalStateException("test error"))

        override fun getRefreshKey(state: PagingState<Int, Movie>): Int? = null
    }
}
