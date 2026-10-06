package com.benjamin.moviehub.ui.detail

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.benjamin.moviehub.R
import com.benjamin.moviehub.TestStrings
import com.benjamin.moviehub.core.theme.MovieHubTheme
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.model.MovieCredits
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Recovery behavior for the independently loaded detail sections: a failed credits,
 * recommendations or library-sync request keeps the film visible and exposes a retry for
 * that endpoint only.
 */
class MovieDetailRecoveryTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun creditsErrorKeepsFilmVisibleAndRetriesOnlyCredits() {
        var creditsRetries = 0
        var recommendationRetries = 0
        setDetailScreen(
            credits = MovieCreditsUiState.Error,
            onRetryCredits = { creditsRetries++ },
            onRetryRecommendations = { recommendationRetries++ },
        )

        composeRule.onNodeWithText(TITLE).assertIsDisplayed()
        composeRule.onNodeWithTag("detail_content_list").performScrollToNode(hasTestTag("credits_error"))
        composeRule.onNodeWithTag("credits_error").assertIsDisplayed()
        composeRule.onNodeWithText(TestStrings.get(R.string.error_loading_credits)).assertIsDisplayed()

        composeRule.onNodeWithText(TestStrings.get(R.string.retry)).performClick()

        assertEquals(1, creditsRetries)
        assertEquals(0, recommendationRetries)
        composeRule.onNodeWithText(TITLE).assertIsDisplayed()
    }

    @Test
    fun recommendationsErrorKeepsFilmVisibleAndRetriesOnlyRecommendations() {
        var creditsRetries = 0
        var recommendationRetries = 0
        setDetailScreen(
            recommendations = MovieRecommendationsUiState.Error,
            onRetryCredits = { creditsRetries++ },
            onRetryRecommendations = { recommendationRetries++ },
        )

        composeRule.onNodeWithText(TITLE).assertIsDisplayed()
        composeRule
            .onNodeWithTag("detail_content_list")
            .performScrollToNode(hasTestTag("recommendations_error"))
        composeRule.onNodeWithTag("recommendations_error").assertIsDisplayed()
        composeRule.onNodeWithText(TestStrings.get(R.string.error_loading_recommendations)).assertIsDisplayed()

        composeRule.onNodeWithText(TestStrings.get(R.string.retry)).performClick()

        assertEquals(1, recommendationRetries)
        assertEquals(0, creditsRetries)
        composeRule.onNodeWithText(TITLE).assertIsDisplayed()
    }

    @Test
    fun librarySyncErrorDisablesWritesAndRetriesObservation() {
        var observationRetries = 0
        setDetailScreen(
            libraryObservation = LibraryObservationUiState.Error,
            onRetryLibraryObservation = { observationRetries++ },
        )

        composeRule.onNodeWithText(TITLE).assertIsDisplayed()
        assertLibraryActionsDisabled()

        composeRule
            .onNodeWithTag("detail_content_list")
            .performScrollToNode(hasTestTag("library_sync_error"))
        composeRule.onNodeWithTag("library_sync_error").assertIsDisplayed()
        composeRule.onNodeWithText(TestStrings.get(R.string.error_syncing_library)).assertIsDisplayed()

        composeRule.onNodeWithText(TestStrings.get(R.string.retry)).performClick()

        assertEquals(1, observationRetries)
        assertLibraryActionsDisabled()
    }

    @Test
    fun libraryWritesStayDisabledUntilObservationIsReady() {
        setDetailScreen(libraryObservation = LibraryObservationUiState.Loading)

        assertLibraryActionsDisabled()
    }

    @Test
    fun libraryWritesAreEnabledOnceObservationIsReady() {
        setDetailScreen(libraryObservation = LibraryObservationUiState.Ready)

        libraryActionTags.forEach { tag ->
            composeRule.onNodeWithTag(tag).assertIsEnabled().assertHasClickAction()
        }
    }

    private fun assertLibraryActionsDisabled() {
        libraryActionTags.forEach { tag ->
            composeRule.onNodeWithTag(tag).assertIsNotEnabled()
        }
    }

    private fun setDetailScreen(
        credits: MovieCreditsUiState = MovieCreditsUiState.Success(MovieCredits()),
        recommendations: MovieRecommendationsUiState = MovieRecommendationsUiState.Empty,
        libraryObservation: LibraryObservationUiState = LibraryObservationUiState.Ready,
        onRetryCredits: () -> Unit = {},
        onRetryRecommendations: () -> Unit = {},
        onRetryLibraryObservation: () -> Unit = {},
    ) {
        composeRule.setContent {
            MovieHubTheme {
                MovieDetailScreen(
                    uiState =
                        MovieDetailUiState.Success(
                            movie = movie(),
                            credits = credits,
                            recommendations = recommendations,
                            libraryObservation = libraryObservation,
                        ),
                    onBackClick = {},
                    onToggleFavorite = {},
                    onToggleWatchlist = {},
                    onToggleWatched = {},
                    onRetry = {},
                    onRetryCredits = onRetryCredits,
                    onRetryRecommendations = onRetryRecommendations,
                    onRetryLibraryObservation = onRetryLibraryObservation,
                )
            }
        }
    }

    private fun movie(): Movie =
        Movie(
            id = 1,
            title = TITLE,
            overview = "",
            posterPath = null,
            backdropPath = null,
            voteAverage = 7.5,
            releaseDate = "2024-01-01",
            webUrl = null,
            isFavorite = false,
            genreIds = persistentListOf(),
            genres = persistentListOf(),
        )

    private companion object {
        private const val TITLE = "Film visible"
        private val libraryActionTags = listOf("detail_favorite", "detail_watchlist", "detail_watched")
    }
}
