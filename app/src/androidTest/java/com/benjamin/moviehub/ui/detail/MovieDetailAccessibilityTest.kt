package com.benjamin.moviehub.ui.detail

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MovieDetailAccessibilityTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun favoriteActionUsesDynamicAccessibilityDescription() {
        setDetailScreen(movie(isFavorite = true))

        composeRule.onNodeWithContentDescription("Retirer des favoris").assertHasClickAction()
        composeRule.onNodeWithTag("detail_favorite").assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun favoriteActionUsesAddAccessibilityDescriptionWhenNotFavorite() {
        setDetailScreen(movie())

        composeRule.onNodeWithContentDescription("Ajouter aux favoris").assertHasClickAction()
    }

    @Test
    fun detailShowsCreditsAndUsefulMetadata() {
        setDetailContent(
            movie(voteAverage = 8.7, voteCount = 123, genres = listOf("Action", "Drame")),
            credits = MovieCredits(emptyList(), "Director Name"),
        )

        composeRule.onNodeWithText("8.7 / 10").assertIsDisplayed()
        composeRule.onNodeWithText("123 votes").assertIsDisplayed()
        composeRule.onNodeWithText("Réalisé par Director Name").assertIsDisplayed()
        composeRule.onNodeWithText("Action • Drame").assertIsDisplayed()
    }

    @Test
    fun longSynopsisCanBeExpandedAndCollapsed() {
        setDetailContent(movie(overview = "A long synopsis that needs to be truncated before it can be expanded. ".repeat(8)))

        composeRule
            .onNodeWithText("Afficher plus")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithText("Afficher moins").assertIsDisplayed()
    }

    @Test
    fun detailOmitsEmptyOptionalInformation() {
        setDetailContent(movie(voteAverage = 0.0, voteCount = 0))

        assertTrue(composeRule.onAllNodesWithText("0.0 / 10").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithText("0 votes").fetchSemanticsNodes().isEmpty())
        composeRule.onNodeWithText("Non noté").assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithText("Distribution").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun detailControlsRemainAccessibleWithLargeTouchTargets() {
        setDetailScreen(movie())

        composeRule.onNodeWithContentDescription("Retour").assertHasClickAction().assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithContentDescription("Partager").assertHasClickAction().assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun libraryChipsExposeAccessibleActions() {
        setDetailScreen(movie(isWatchlist = true))

        composeRule.onNodeWithTag("detail_favorite").assertHasClickAction().assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithTag("detail_watchlist").assertHasClickAction()
        composeRule.onNodeWithContentDescription("Retirer de la liste À voir").assertHasClickAction().assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithTag("detail_watched").assertHasClickAction()
        composeRule.onNodeWithContentDescription("Marquer comme vu").assertHasClickAction().assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun libraryActionsExposeSingleToggleSemantic() {
        setDetailScreen(movie(isFavorite = true))

        composeRule.onAllNodesWithTag("detail_favorite").assertCountEquals(1)
        composeRule.onAllNodesWithTag("detail_watchlist").assertCountEquals(1)
        composeRule.onAllNodesWithTag("detail_watched").assertCountEquals(1)
        composeRule.onNodeWithTag("detail_favorite").assertIsOn()
        composeRule.onNodeWithTag("detail_watchlist").assertIsOff()
        composeRule.onNodeWithTag("detail_watched").assertIsOff()
    }

    @Test
    fun libraryActionLabelIsPartOfTheClickableTarget() {
        var favoriteClicks = 0
        setDetailScreen(movie(), onToggleFavorite = { favoriteClicks++ })

        composeRule.onNodeWithText("Favoris").performClick()
        composeRule.runOnIdle { assertEquals(1, favoriteClicks) }
    }

    private fun setDetailScreen(
        movie: Movie,
        onToggleFavorite: () -> Unit = {},
        onToggleWatchlist: () -> Unit = {},
        onToggleWatched: () -> Unit = {},
    ) {
        composeRule.setContent {
            MovieHubTheme {
                MovieDetailScreen(
                    uiState = MovieDetailUiState.Success(movie, MovieCredits()),
                    onBackClick = {},
                    onToggleFavorite = onToggleFavorite,
                    onToggleWatchlist = onToggleWatchlist,
                    onToggleWatched = onToggleWatched,
                    onRetry = {},
                )
            }
        }
    }

    private fun setDetailContent(
        movie: Movie,
        credits: MovieCredits = MovieCredits(),
    ) {
        composeRule.setContent {
            MovieHubTheme {
                MovieDetailContent(movie = movie, credits = credits)
            }
        }
    }

    private fun movie(
        isFavorite: Boolean = false,
        isWatchlist: Boolean = false,
        isWatched: Boolean = false,
        overview: String = "Overview",
        voteAverage: Double = 7.5,
        voteCount: Int? = null,
        genres: List<String> = emptyList(),
    ): Movie =
        Movie(
            id = 1,
            title = "Test movie",
            overview = overview,
            posterPath = null,
            backdropPath = null,
            voteAverage = voteAverage,
            releaseDate = "2024-01-01",
            webUrl = null,
            isFavorite = isFavorite,
            isWatchlist = isWatchlist,
            isWatched = isWatched,
            genreIds = emptyList(),
            genres = genres,
            voteCount = voteCount,
        )
}
