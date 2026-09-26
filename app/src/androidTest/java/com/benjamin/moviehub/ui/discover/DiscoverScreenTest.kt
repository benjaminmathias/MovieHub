package com.benjamin.moviehub.ui.discover

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.benjamin.moviehub.core.theme.MovieHubTheme
import com.benjamin.moviehub.domain.model.DiscoverSortOption
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.model.MovieGenre
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
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
    fun applyIsDisabledWhenDraftMatchesAppliedFilters() {
        setDiscoverContent(state = { initialState() }, results = flowOf(PagingData.empty()))

        composeRule.onNodeWithTag("discover_filter_button").performClick()
        composeRule.onNodeWithTag("discover_apply_filters").assertIsNotEnabled()
    }

    @Test
    fun customYearValidityControlsApply() {
        var state by mutableStateOf(initialState())
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        setDiscoverContent(
            state = { state },
            onReleaseYearSelected = { year -> state = state.copy(draftFilters = state.draftFilters.copy(releaseYear = year)) },
            results = flowOf(PagingData.empty()),
        )

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

        val validYear = currentYear - 1
        yearField.performTextClearance()
        yearField.performTextInput(validYear.toString())
        composeRule.runOnIdle { assertEquals(validYear, state.draftFilters.releaseYear) }
        composeRule.onNodeWithTag("discover_apply_filters").assertIsEnabled()
    }

    @Test
    fun genreErrorShowsRetryAndInvokesCallback() {
        var retryCalled = false

        setDiscoverContent(
            state = { initialState().copy(genres = persistentListOf(), hasGenreError = true) },
            onRetryGenres = { retryCalled = true },
            results = flowOf(PagingData.empty()),
        )

        composeRule.onNodeWithTag("discover_filter_button").performClick()
        composeRule.onNodeWithTag("discover_genre_row").performClick()
        composeRule
            .onNodeWithTag("discover_retry_genres")
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()

        composeRule.runOnIdle { assertTrue(retryCalled) }
    }

    @Test
    fun initialPagingErrorShowsRetry() {
        setDiscoverContent(
            state = { initialState() },
            results = Pager(PagingConfig(pageSize = 1)) { ErrorPagingSource() }.flow,
        )

        composeRule.onNodeWithText("Impossible de charger les films.").assertIsDisplayed()
        composeRule.onNodeWithText("Réessayer").assertIsDisplayed().performClick()
    }

    @Test
    fun closingSheetDiscardsDraftChanges() {
        var state by mutableStateOf(initialState())

        setDiscoverContent(
            state = { state },
            onGenreSelected = { genreId -> state = state.copy(draftFilters = state.draftFilters.copy(genreId = genreId)) },
            onBeginFilterEditing = { state = state.copy(draftFilters = state.appliedFilters) },
            onDiscardFilterEdits = { state = state.copy(draftFilters = state.appliedFilters) },
            results = flowOf(PagingData.empty()),
        )

        composeRule.onNodeWithTag("discover_filter_button").performClick()
        composeRule.onNodeWithTag("discover_genre_row").performClick()
        composeRule.onNodeWithTag("discover_genre_option_28").performScrollTo().performClick()
        composeRule.onNodeWithTag("discover_close_filters").performClick()

        composeRule.runOnIdle {
            assertEquals(null, state.draftFilters.genreId)
        }
        composeRule.onAllNodesWithTag("discover_filter_sheet").assertCountEquals(0)
    }

    private fun setDiscoverContent(
        state: () -> DiscoverUiState,
        results: Flow<PagingData<Movie>> = flowOf(PagingData.from(listOf(testMovie()))),
        onGenreSelected: (Int?) -> Unit = {},
        onReleaseYearSelected: (Int?) -> Unit = {},
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
                    onReleaseYearSelected = onReleaseYearSelected,
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

    private class ErrorPagingSource : PagingSource<Int, Movie>() {
        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Movie> = LoadResult.Error(IllegalStateException("test error"))

        override fun getRefreshKey(state: PagingState<Int, Movie>): Int? = null
    }
}
