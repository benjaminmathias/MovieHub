package com.benjamin.moviehub.ui.library

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.benjamin.moviehub.R
import com.benjamin.moviehub.TestStrings
import com.benjamin.moviehub.core.theme.MovieHubTheme
import com.benjamin.moviehub.domain.model.Movie
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
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
