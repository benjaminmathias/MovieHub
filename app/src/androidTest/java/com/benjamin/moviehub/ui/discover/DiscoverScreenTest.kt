package com.benjamin.moviehub.ui.discover

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.paging.PagingData
import com.benjamin.moviehub.core.theme.MovieHubTheme
import com.benjamin.moviehub.domain.model.DiscoverSortOption
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.model.MovieGenre
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

        setDiscoverContent(
            state = { state },
            onGenreSelected = { genreId -> state = state.copy(draftFilters = state.draftFilters.copy(genreId = genreId)) },
            onBeginFilterEditing = { state = state.copy(draftFilters = state.appliedFilters) },
            onApplyFilters = {
                state = state.copy(appliedFilters = state.draftFilters)
                applyCalled = true
            },
            onDiscardFilterEdits = { state = state.copy(draftFilters = state.appliedFilters) },
        )

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
    fun currentDecadeOptionSelectsAndApplies() {
        val currentDecade = Calendar.getInstance().get(Calendar.YEAR) / 10 * 10
        var state by mutableStateOf(initialState())
        var applyCalled = false

        setDiscoverContent(
            state = { state },
            onBeginFilterEditing = { state = state.copy(draftFilters = state.appliedFilters) },
            onReleaseDecadeSelected = { decade -> state = state.copy(draftFilters = state.draftFilters.copy(releaseDecade = decade)) },
            onApplyFilters = {
                state = state.copy(appliedFilters = state.draftFilters)
                applyCalled = true
            },
            onDiscardFilterEdits = { state = state.copy(draftFilters = state.appliedFilters) },
        )

        composeRule.onNodeWithTag("discover_filter_button").performClick()
        composeRule.onNodeWithTag("discover_decade_row").performClick()

        // The newest selectable decade is the current one; only decade starts are offered.
        composeRule.onNodeWithTag("discover_decade_option_$currentDecade").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("discover_decade_option_${currentDecade + 10}").assertDoesNotExist()
        composeRule.onNodeWithTag("discover_decade_option_${currentDecade + 5}").assertDoesNotExist()

        composeRule.onNodeWithTag("discover_decade_option_$currentDecade").performClick()

        // Selecting a decade stores the draft and returns to the main panel without applying.
        composeRule.onNodeWithTag("discover_decade_row").assertIsDisplayed()
        composeRule.onAllNodesWithTag("discover_decade_section").assertCountEquals(0)
        composeRule.runOnIdle {
            assertEquals(currentDecade, state.draftFilters.releaseDecade)
            assertFalse(applyCalled)
        }

        composeRule.onNodeWithTag("discover_apply_filters").assertIsEnabled().performClick()
        composeRule.runOnIdle {
            assertTrue(applyCalled)
            assertEquals(currentDecade, state.appliedFilters.releaseDecade)
        }
        composeRule.onAllNodesWithTag("discover_filter_sheet").assertCountEquals(0)
    }

    @Test
    fun decadeSelectionStaysWithinEligibleBounds() {
        var state by mutableStateOf(initialState())

        setDiscoverContent(
            state = { state },
            onBeginFilterEditing = { state = state.copy(draftFilters = state.appliedFilters) },
            onReleaseDecadeSelected = { decade -> state = state.copy(draftFilters = state.draftFilters.copy(releaseDecade = decade)) },
            onApplyFilters = { state = state.copy(appliedFilters = state.draftFilters) },
            onDiscardFilterEdits = { state = state.copy(draftFilters = state.appliedFilters) },
        )

        composeRule.onNodeWithTag("discover_filter_button").performClick()
        composeRule.onNodeWithTag("discover_decade_row").performClick()

        // The oldest selectable decade is 1870.
        composeRule.onNodeWithTag("discover_decade_option_1870").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("discover_decade_option_1990").performScrollTo().performClick()

        composeRule.onNodeWithTag("discover_decade_row").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(1990, state.draftFilters.releaseDecade) }

        composeRule.onNodeWithTag("discover_apply_filters").performClick()
        composeRule.runOnIdle { assertEquals(1990, state.appliedFilters.releaseDecade) }

        // Reopening restores the applied decade as the selected option.
        composeRule.onNodeWithTag("discover_filter_button").performClick()
        composeRule.onNodeWithTag("discover_decade_row").performClick()
        composeRule.onNodeWithTag("discover_decade_option_1990").performScrollTo().assertIsSelected()

        // "Toutes les décennies" clears the draft, but the applied filter stays until Apply.
        composeRule.onNodeWithTag("discover_decade_option_all").performScrollTo().performClick()
        composeRule.onNodeWithTag("discover_decade_row").assertIsDisplayed()
        composeRule.onAllNodesWithTag("discover_decade_section").assertCountEquals(0)
        composeRule.runOnIdle {
            assertEquals(null, state.draftFilters.releaseDecade)
            assertEquals(1990, state.appliedFilters.releaseDecade)
        }

        composeRule.onNodeWithTag("discover_apply_filters").performClick()
        composeRule.runOnIdle { assertEquals(null, state.appliedFilters.releaseDecade) }
    }

    @Test
    fun dismissingSheetDiscardsDecadeAndReopenUsesApplied() {
        val currentDecade = Calendar.getInstance().get(Calendar.YEAR) / 10 * 10
        var state by mutableStateOf(initialState())

        setDiscoverContent(
            state = { state },
            onBeginFilterEditing = { state = state.copy(draftFilters = state.appliedFilters) },
            onReleaseDecadeSelected = { decade -> state = state.copy(draftFilters = state.draftFilters.copy(releaseDecade = decade)) },
            onDiscardFilterEdits = { state = state.copy(draftFilters = state.appliedFilters) },
        )

        composeRule.onNodeWithTag("discover_filter_button").performClick()
        composeRule.onNodeWithTag("discover_decade_row").performClick()
        composeRule.onNodeWithTag("discover_decade_option_1970").performScrollTo().performClick()

        composeRule.onNodeWithTag("discover_decade_row").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(1970, state.draftFilters.releaseDecade) }

        // Dismissing cancels the pending decade and restores the applied filters.
        composeRule.onNodeWithTag("discover_close_filters").performClick()
        composeRule.onAllNodesWithTag("discover_filter_sheet").assertCountEquals(0)
        composeRule.runOnIdle { assertEquals(null, state.draftFilters.releaseDecade) }

        // Reopening starts from the applied draft, so no decade is selected.
        composeRule.onNodeWithTag("discover_filter_button").performClick()
        composeRule.onNodeWithTag("discover_decade_row").performClick()
        composeRule.onNodeWithTag("discover_decade_option_all").performScrollTo().assertIsSelected()
        composeRule.onNodeWithTag("discover_decade_option_$currentDecade").assertExists()
    }

    private fun setDiscoverContent(
        state: () -> DiscoverUiState,
        results: Flow<PagingData<Movie>> = flowOf(PagingData.from(listOf(testMovie()))),
        onGenreSelected: (Int?) -> Unit = {},
        onReleaseDecadeSelected: (Int?) -> Unit = {},
        onMinimumRatingSelected: (Double?) -> Unit = {},
        onSortSelected: (DiscoverSortOption) -> Unit = {},
        onBeginFilterEditing: () -> Unit = {},
        onApplyFilters: () -> Unit = {},
        onResetFilters: () -> Unit = {},
        onDiscardFilterEdits: () -> Unit = {},
        onRetryGenres: () -> Unit = {},
    ) {
        composeRule.setContent {
            MovieHubTheme {
                DiscoverScreen(
                    state = state(),
                    discoverResults = results,
                    onGenreSelected = onGenreSelected,
                    onReleaseDecadeSelected = onReleaseDecadeSelected,
                    onMinimumRatingSelected = onMinimumRatingSelected,
                    onSortSelected = onSortSelected,
                    onBeginFilterEditing = onBeginFilterEditing,
                    onApplyFilters = onApplyFilters,
                    onResetFilters = onResetFilters,
                    onDiscardFilterEdits = onDiscardFilterEdits,
                    onRetryGenres = onRetryGenres,
                    onMovieClick = {},
                )
            }
        }
    }

    private fun initialState() =
        DiscoverUiState(
            genres = listOf(MovieGenre(id = 28, name = "Action")).toImmutableList(),
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
            genreIds = persistentListOf(),
            genres = persistentListOf(),
        )
}
