package com.benjamin.moviehub.ui.library

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
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
