package com.benjamin.moviehub.ui.library

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import com.benjamin.moviehub.core.theme.MovieHubTheme
import com.benjamin.moviehub.domain.model.Movie
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue

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
                    state = LibraryUiState.Success(listOf(watchlist, favorite)),
                    onRemove = { _, _ -> },
                    onMovieClick = {},
                    onSettingsClick = {},
                )
            }
        }
        composeRule.onNodeWithText("À voir").assertIsDisplayed()
        composeRule.onNodeWithText("Favoris").assertIsDisplayed()
        composeRule.onNodeWithText("Vu").assertIsDisplayed()
        composeRule.onNodeWithText("À regarder").assertIsDisplayed()
        composeRule.onNodeWithText("Favoris").performClick()
        composeRule.onNodeWithText("Favori").assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithText("À regarder").fetchSemanticsNodes().isEmpty())
        composeRule.onNodeWithText("Vu").performClick()
        composeRule.onNodeWithText("Aucun film vu.").assertIsDisplayed()
    }

    @Test
    fun emptyStateUsesContextualMessageForEachTab() {
        composeRule.setContent {
            MovieHubTheme {
                LibraryScreen(
                    state = LibraryUiState.Success(emptyList()),
                    onRemove = { _, _ -> },
                    onMovieClick = {},
                    onSettingsClick = {},
                )
            }
        }

        composeRule.onNodeWithText("Aucun film à voir.").assertIsDisplayed()
        composeRule.onNodeWithText("Favoris").performClick()
        composeRule.onNodeWithText("Aucun film favori n'a été ajouté.").assertIsDisplayed()
        composeRule.onNodeWithText("Vu").performClick()
        composeRule.onNodeWithText("Aucun film vu.").assertIsDisplayed()
    }

    @Test
    fun swipeExposesContextualRemoveAction() {
        val watchlist = movie(1, "À regarder", watchlist = true)
        composeRule.setContent {
            MovieHubTheme {
                LibraryScreen(
                    state = LibraryUiState.Success(listOf(watchlist)),
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
        assertTrue(actions.any { it.label == "Retirer de la liste À voir" })
    }

    private fun movie(id: Int, title: String, favorite: Boolean = false, watchlist: Boolean = false) = Movie(
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
        genreIds = emptyList(),
        genres = emptyList(),
    )
}
