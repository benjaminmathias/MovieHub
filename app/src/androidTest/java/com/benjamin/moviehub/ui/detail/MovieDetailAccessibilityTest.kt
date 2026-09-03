package com.benjamin.moviehub.ui.detail

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.benjamin.moviehub.core.theme.MovieHubTheme
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.model.MovieCredits
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MovieDetailAccessibilityTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun favoriteActionUsesDynamicAccessibilityDescription() {
        val movie =
            Movie(
                id = 1,
                title = "Test movie",
                overview = "Overview",
                posterPath = null,
                backdropPath = null,
                voteAverage = 7.5,
                releaseDate = "2024-01-01",
                webUrl = null,
                isFavorite = true,
                genreIds = emptyList(),
                genres = emptyList(),
            )

        composeRule.setContent {
            MovieHubTheme {
                MovieDetailScreen(
                    uiState = MovieDetailUiState.Success(movie, MovieCredits()),
                    onBackClick = {},
                    onToggleFavorite = {},
                    onRetry = {},
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Retirer des favoris")
            .assertHasClickAction()
        composeRule.onNodeWithTag("detail_favorite").assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun favoriteActionUsesAddAccessibilityDescriptionWhenNotFavorite() {
        val movie =
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

        composeRule.setContent {
            MovieHubTheme {
                MovieDetailScreen(
                    uiState = MovieDetailUiState.Success(movie, MovieCredits()),
                    onBackClick = {},
                    onToggleFavorite = {},
                    onRetry = {},
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Ajouter aux favoris")
            .assertHasClickAction()
    }

    @Test
    fun detailShowsCreditsAndUsefulMetadata() {
        val movie =
            Movie(
                id = 1,
                title = "Test movie",
                overview = "Overview",
                posterPath = null,
                backdropPath = null,
                voteAverage = 8.7,
                releaseDate = "2024-01-01",
                webUrl = null,
                isFavorite = false,
                genreIds = emptyList(),
                genres = listOf("Action", "Drame"),
            )

        composeRule.setContent {
            MovieHubTheme {
                MovieDetailContent(
                    movie = movie,
                    credits = MovieCredits(emptyList(), "Director Name"),
                )
            }
        }

        composeRule.onNodeWithText("8.7 / 10").assertIsDisplayed()
        composeRule.onNodeWithText("Réalisé par Director Name").assertIsDisplayed()
        composeRule.onNodeWithText("Genres").assertIsDisplayed()
        composeRule.onNodeWithText("Action").assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithText("Budget").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun detailOmitsEmptyOptionalInformation() {
        val movie =
            Movie(
                id = 1,
                title = "Test movie",
                overview = "",
                posterPath = null,
                backdropPath = null,
                voteAverage = 0.0,
                releaseDate = "2024-01-01",
                webUrl = null,
                isFavorite = false,
                genreIds = emptyList(),
                genres = emptyList(),
                voteCount = 0,
            )

        composeRule.setContent {
            MovieHubTheme {
                MovieDetailContent(
                    movie = movie,
                    credits = MovieCredits(),
                )
            }
        }

        assertTrue(composeRule.onAllNodesWithText("Budget").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithText("0.0 / 10").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithText("0 votes").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithText("Casting Principal").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun detailControlsRemainAccessibleWithLargeTouchTargets() {
        val movie =
            Movie(
                id = 1,
                title = "Test movie",
                overview = "Overview",
                posterPath = null,
                backdropPath = null,
                voteAverage = 0.0,
                releaseDate = "2024-01-01",
                webUrl = null,
                isFavorite = false,
                genreIds = emptyList(),
                genres = emptyList(),
            )

        composeRule.setContent {
            MovieHubTheme {
                MovieDetailScreen(
                    uiState = MovieDetailUiState.Success(movie, MovieCredits()),
                    onBackClick = {},
                    onToggleFavorite = {},
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Retour")
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithContentDescription("Partager")
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
    }

}
