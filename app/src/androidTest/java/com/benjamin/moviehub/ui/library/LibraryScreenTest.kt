package com.benjamin.moviehub.ui.library

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.benjamin.moviehub.R
import com.benjamin.moviehub.TestStrings
import com.benjamin.moviehub.core.theme.MovieHubTheme
import com.benjamin.moviehub.domain.model.Movie
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LibraryScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun tabsAreOrderedAndSelectionFiltersMovies() {
        val watchlist = movie(1, "À regarder", watchlist = true)
        val favorite = movie(2, "Favori", favorite = true)
        composeRule.setContent {
            MovieHubTheme {
                LibraryScreen(
                    state = LibraryUiState.Success(listOf(watchlist, favorite).toImmutableList()),
                    onRemove = { _, _ -> },
                    onMovieClick = {},
                    onSettingsClick = {},
                )
            }
        }
        composeRule.onNodeWithTag("library_tab_watchlist").assertIsDisplayed()
        composeRule.onNodeWithTag("library_tab_favorites").assertIsDisplayed()
        composeRule.onNodeWithTag("library_tab_watched").assertIsDisplayed()
        composeRule.onNodeWithText("À regarder").assertIsDisplayed()
        composeRule.onNodeWithTag("library_tab_favorites").performClick()
        composeRule.onNodeWithText("Favori").assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithText("À regarder").fetchSemanticsNodes().isEmpty())
        composeRule.onNodeWithTag("library_tab_watched").performClick()
        composeRule.onNodeWithText(TestStrings.get(LibraryTab.WATCHED.emptyMessageRes)).assertIsDisplayed()
    }

    @Test
    fun errorStateOffersRetryAction() {
        var retried = false
        composeRule.setContent {
            MovieHubTheme {
                LibraryScreen(
                    state = LibraryUiState.Error(R.string.error_loading_movies),
                    onRemove = { _, _ -> },
                    onMovieClick = {},
                    onSettingsClick = {},
                    onRetry = { retried = true },
                )
            }
        }

        composeRule.onNodeWithText(TestStrings.get(R.string.retry)).assertIsDisplayed().performClick()
        assertTrue(retried)
    }

    @Test
    fun pendingActionErrorShowsMessageAndAcknowledgesAfterDisplay() {
        var acknowledged = false
        composeRule.setContent {
            MovieHubTheme {
                LibraryScreen(
                    state = LibraryUiState.Success(emptyList<Movie>().toImmutableList()),
                    onRemove = { _, _ -> },
                    onMovieClick = {},
                    onSettingsClick = {},
                    actionErrorPending = true,
                    onActionErrorAcknowledged = { acknowledged = true },
                )
            }
        }

        composeRule.onNodeWithText(TestStrings.get(R.string.error_updating_library)).assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 10_000) { acknowledged }
    }

    @Test
    fun leavingLibraryBeforeTheMessageFinishesKeepsTheErrorPending() {
        var pending by mutableStateOf(true)
        var showLibrary by mutableStateOf(true)
        var acknowledged = false
        val message = TestStrings.get(R.string.error_updating_library)

        composeRule.setContent {
            MovieHubTheme {
                if (showLibrary) {
                    LibraryScreen(
                        state = LibraryUiState.Success(emptyList<Movie>().toImmutableList()),
                        onRemove = { _, _ -> },
                        onMovieClick = {},
                        onSettingsClick = {},
                        actionErrorPending = pending,
                        onActionErrorAcknowledged = {
                            acknowledged = true
                            pending = false
                        },
                    )
                }
            }
        }

        composeRule.onNodeWithText(message).assertIsDisplayed()

        // Leaving the library mid-display cancels the effect, so the error is not acknowledged.
        composeRule.runOnIdle { showLibrary = false }
        composeRule.waitForIdle()
        assertFalse(acknowledged)
        assertTrue(pending)

        // Returning shows the still-pending message again.
        composeRule.runOnIdle { showLibrary = true }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(message).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun swipeExposesContextualRemoveAction() {
        val watchlist = movie(1, "À regarder", watchlist = true)
        composeRule.setContent {
            MovieHubTheme {
                LibraryScreen(
                    state = LibraryUiState.Success(listOf(watchlist).toImmutableList()),
                    onRemove = { _, _ -> },
                    onMovieClick = {},
                    onSettingsClick = {},
                )
            }
        }

        val actions =
            composeRule
                .onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.CustomActions))
                .fetchSemanticsNode()
                .config[SemanticsActions.CustomActions]
        assertTrue(actions.any { it.label == TestStrings.get(R.string.remove_watchlist_accessibility) })
    }

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
