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
import com.benjamin.moviehub.R
import com.benjamin.moviehub.TestStrings
import com.benjamin.moviehub.core.theme.MovieHubTheme
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.model.MovieCredits
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.text.NumberFormat
import java.util.Locale

class MovieDetailAccessibilityTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun favoriteActionUsesDynamicAccessibilityDescription() {
        setDetailScreen(movie(isFavorite = true))

        composeRule.onNodeWithContentDescription(TestStrings.get(R.string.remove_favorite)).assertHasClickAction()
        composeRule.onNodeWithTag("detail_favorite").assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun detailShowsCreditsAndUsefulMetadata() {
        setDetailContent(
            movie(voteAverage = 8.7, voteCount = 123, genres = listOf("Action", "Drame")),
            credits = MovieCredits(director = "Director Name"),
        )

        composeRule.onNodeWithText(TestStrings.get(R.string.rating_out_of_ten, 8.7)).assertIsDisplayed()
        composeRule
            .onNodeWithText(TestStrings.get(R.string.vote_count, NumberFormat.getIntegerInstance(Locale.FRANCE).format(123)))
            .assertIsDisplayed()
        composeRule.onNodeWithText(TestStrings.get(R.string.director_format, "Director Name")).assertIsDisplayed()
        composeRule.onNodeWithText("Action • Drame").assertIsDisplayed()
    }

    @Test
    fun longSynopsisCanBeExpandedAndCollapsed() {
        setDetailContent(movie(overview = "A long synopsis that needs to be truncated before it can be expanded. ".repeat(8)))

        composeRule
            .onNodeWithText(TestStrings.get(R.string.read_more))
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithText(TestStrings.get(R.string.show_less)).assertIsDisplayed()
    }

    @Test
    fun detailOmitsEmptyOptionalInformation() {
        setDetailContent(movie(voteAverage = 0.0, voteCount = 0))

        assertTrue(composeRule.onAllNodesWithText(TestStrings.get(R.string.rating_out_of_ten, 0.0)).fetchSemanticsNodes().isEmpty())
        assertTrue(
            composeRule
                .onAllNodesWithText(TestStrings.get(R.string.vote_count, NumberFormat.getIntegerInstance(Locale.FRANCE).format(0)))
                .fetchSemanticsNodes()
                .isEmpty(),
        )
        composeRule.onNodeWithText(TestStrings.get(R.string.rating_not_available)).assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithText(TestStrings.get(R.string.cast_principal)).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun libraryChipsExposeAccessibleActions() {
        setDetailScreen(movie(isWatchlist = true))

        composeRule.onNodeWithTag("detail_favorite").assertHasClickAction().assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithTag("detail_watchlist").assertHasClickAction()
        composeRule
            .onNodeWithContentDescription(TestStrings.get(R.string.remove_watchlist_accessibility))
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithTag("detail_watched").assertHasClickAction()
        composeRule.onNodeWithContentDescription(TestStrings.get(R.string.mark_watched_accessibility)).assertHasClickAction()
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
            genreIds = persistentListOf(),
            genres = genres.toImmutableList(),
            voteCount = voteCount,
        )
}
