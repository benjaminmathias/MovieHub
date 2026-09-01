package com.benjamin.moviehub.ui.detail

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.benjamin.moviehub.core.theme.MovieHubTheme
import com.benjamin.moviehub.domain.model.Actor
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.model.MovieCredits
import com.benjamin.moviehub.ui.components.ActorItem
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
                MovieDetailContent(
                    movie = movie,
                    credits = MovieCredits(),
                    onToggleFavorite = {},
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Retirer des favoris")
            .assertHasClickAction()
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
                MovieDetailContent(movie = movie, credits = MovieCredits(), onToggleFavorite = {})
            }
        }

        composeRule.onNodeWithContentDescription("Ajouter aux favoris").assertHasClickAction()
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
                genres = emptyList(),
                originalTitle = "Original movie",
                originalLanguage = "en",
                status = "Released",
                voteCount = 8673,
                budget = 100_000_000,
                productionCountries = listOf("United States"),
            )

        composeRule.setContent {
            MovieHubTheme {
                MovieDetailContent(
                    movie = movie,
                    credits = MovieCredits(emptyList(), "Director Name"),
                    onToggleFavorite = {},
                )
            }
        }

        composeRule.onNodeWithText("Director Name").assertIsDisplayed()
        composeRule.onNodeWithText("8.7 / 10").assertIsDisplayed()
        composeRule.onNodeWithText("8673 votes").assertIsDisplayed()
        composeRule.onNodeWithText("Budget").assertIsDisplayed()
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
                    onToggleFavorite = {},
                )
            }
        }

        assertTrue(composeRule.onAllNodesWithText("Informations").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithText("Budget").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithText("0.0 / 10").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithText("0 votes").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun actorWithLongTextRemainsVisible() {
        composeRule.setContent {
            MovieHubTheme {
                ActorItem(
                    Actor(
                        id = 1,
                        name = "A very long actor name that needs truncation",
                        character = "A very long character name that needs truncation",
                        profileUrl = "",
                    ),
                )
            }
        }

        composeRule.onNodeWithText("A very long actor name that needs truncation").assertIsDisplayed()
    }
}
