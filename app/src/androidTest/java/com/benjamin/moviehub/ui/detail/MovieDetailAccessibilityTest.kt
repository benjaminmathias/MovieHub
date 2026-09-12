package com.benjamin.moviehub.ui.detail

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
                voteCount = 123,
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
        composeRule.onNodeWithText("123 votes").assertIsDisplayed()
        composeRule.onNodeWithText("Réalisé par Director Name").assertIsDisplayed()
        composeRule.onNodeWithText("Action • Drame").assertIsDisplayed()
    }

    @Test
    fun longSynopsisCanBeExpandedAndCollapsed() {
        val movie =
            Movie(
                id = 1,
                title = "Test movie",
                overview =
                    "A long synopsis that needs to be truncated before it can be expanded. "
                        .repeat(8),
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
                MovieDetailContent(
                    movie = movie,
                    credits = MovieCredits(),
                )
            }
        }

        composeRule
            .onNodeWithText("Afficher plus")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithText("Afficher moins").assertIsDisplayed()
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

        assertTrue(composeRule.onAllNodesWithText("0.0 / 10").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithText("0 votes").fetchSemanticsNodes().isEmpty())
        composeRule.onNodeWithText("Non noté").assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithText("Distribution").fetchSemanticsNodes().isEmpty())
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

    @Test
    fun libraryChipsExposeAccessibleActions() {
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
                isWatchlist = true,
                isWatched = false,
                genreIds = emptyList(),
                genres = emptyList(),
            )
        composeRule.setContent {
            MovieHubTheme {
                MovieDetailScreen(
                    uiState = MovieDetailUiState.Success(movie, MovieCredits()),
                    onBackClick = {},
                    onToggleFavorite = {},
                    onToggleWatchlist = {},
                    onToggleWatched = {},
                    onRetry = {},
                )
            }
        }
        composeRule
            .onNodeWithTag("detail_favorite")
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithTag("detail_watchlist").assertHasClickAction()
        composeRule
            .onNodeWithContentDescription("Retirer de la liste À voir")
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithTag("detail_watched").assertHasClickAction()
        composeRule
            .onNodeWithContentDescription("Marquer comme vu")
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
    }

}
