package com.benjamin.moviehub.ui.library

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import com.benjamin.moviehub.core.theme.MovieHubTheme
import com.benjamin.moviehub.domain.model.Movie
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LibraryScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun swipeHeldPastThresholdThenDraggedBackDoesNotRemove() {
        val watchlist = movie(1, "À regarder", watchlist = true)
        var removeCount = 0
        composeRule.setContent {
            MovieHubTheme {
                LibraryScreen(
                    state = LibraryUiState.Success(listOf(watchlist).toImmutableList()),
                    onRemove = { _, _ -> removeCount++ },
                    onMovieClick = {},
                    onSettingsClick = {},
                    onRetry = {},
                    actionError = LibraryActionErrorState.None,
                    onActionErrorAcknowledged = {},
                )
            }
        }
        val item = composeRule.onNode(swipeableItem())

        // Cross the dismiss threshold while the finger is still held down. The pointer stays
        // down so recomposition runs mid gesture, matching a real held swipe.
        item.performTouchInput {
            val y = centerY
            down(Offset(width * 0.9f, y))
            moveTo(Offset(width * 0.55f, y))
            moveTo(Offset(width * 0.2f, y))
        }
        composeRule.runOnIdle { assertEquals(0, removeCount) }

        // Drag back below the threshold and release: the row must stay settled.
        item.performTouchInput {
            val y = centerY
            moveTo(Offset(width * 0.55f, y))
            moveTo(Offset(width * 0.92f, y))
            up()
        }
        composeRule.runOnIdle { assertEquals(0, removeCount) }
    }

    @Test
    fun swipePastThresholdAndReleaseRemovesOnce() {
        val watchlist = movie(1, "À regarder", watchlist = true)
        val removed = mutableListOf<Movie>()
        composeRule.setContent {
            MovieHubTheme {
                LibraryScreen(
                    state = LibraryUiState.Success(listOf(watchlist).toImmutableList()),
                    onRemove = { movie, _ -> removed += movie },
                    onMovieClick = {},
                    onSettingsClick = {},
                    onRetry = {},
                    actionError = LibraryActionErrorState.None,
                    onActionErrorAcknowledged = {},
                )
            }
        }

        composeRule.onNode(swipeableItem()).performTouchInput {
            val y = centerY
            down(Offset(width * 0.9f, y))
            moveTo(Offset(width * 0.55f, y))
            moveTo(Offset(width * 0.25f, y))
            moveTo(Offset(width * 0.03f, y))
            up()
        }

        composeRule.waitUntil(timeoutMillis = 5_000) { removed.isNotEmpty() }
        composeRule.waitForIdle()
        assertEquals(listOf(watchlist), removed)
    }

    @Test
    fun tabsKeepIndependentScrollPositionsAcrossEmptyTabAndRestoration() {
        val restoration = StateRestorationTester(composeRule)
        val movies =
            (
                (0..59).map { movie(it, "Watchlist $it", watchlist = true) } +
                    (0..59).map { movie(100 + it, "Favorite $it", favorite = true) }
            ).toImmutableList()
        restoration.setContent {
            MovieHubTheme {
                LibraryScreen(
                    state = LibraryUiState.Success(movies),
                    onRemove = { _, _ -> },
                    onMovieClick = {},
                    onSettingsClick = {},
                    onRetry = {},
                    actionError = LibraryActionErrorState.None,
                    onActionErrorAcknowledged = {},
                )
            }
        }

        composeRule.onNode(hasScrollAction()).performScrollToIndex(30)
        composeRule.onNodeWithText("Watchlist 30").assertIsDisplayed()
        composeRule.onNodeWithTag("library_tab_favorites").performClick()
        composeRule.onNodeWithText("Favorite 0").assertIsDisplayed()
        composeRule.onNode(hasScrollAction()).performScrollToIndex(20)
        composeRule.onNodeWithText("Favorite 20").assertIsDisplayed()

        composeRule.onNodeWithTag("library_tab_watched").performClick()
        composeRule.onNodeWithTag("library_tab_watchlist").performClick()
        composeRule.onNodeWithText("Watchlist 30").assertIsDisplayed()

        restoration.emulateSavedInstanceStateRestore()
        composeRule.onNodeWithText("Watchlist 30").assertIsDisplayed()
        composeRule.onNodeWithTag("library_tab_favorites").performClick()
        composeRule.onNodeWithText("Favorite 20").assertIsDisplayed()
    }

    private fun swipeableItem(): SemanticsMatcher = SemanticsMatcher.keyIsDefined(SemanticsActions.CustomActions)

    private fun movie(
        id: Int,
        title: String,
        favorite: Boolean = false,
        watchlist: Boolean = false,
    ) = Movie(
        id = id,
        title = title,
        overview = "",
        posterPath = null,
        backdropPath = null,
        voteAverage = 0.0,
        releaseDate = "",
        webUrl = null,
        isFavorite = favorite,
        isWatchlist = watchlist,
        isWatched = false,
        genreIds = persistentListOf(),
        genres = persistentListOf(),
    )
}
