package com.benjamin.moviehub.ui.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.benjamin.moviehub.R
import com.benjamin.moviehub.TestStrings
import com.benjamin.moviehub.core.theme.MovieHubTheme
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.model.MovieCredits
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Rule
import org.junit.Test

class MovieDetailAccessibilityTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun favoriteActionAccessibilityDescriptionTracksFavoriteState() {
        var currentMovie by mutableStateOf(movie())
        setDetailScreen(
            movie = { currentMovie },
            onToggleFavorite = { currentMovie = currentMovie.copy(isFavorite = !currentMovie.isFavorite) },
        )

        val addFavorite = TestStrings.get(R.string.favorite)
        val removeFavorite = TestStrings.get(R.string.remove_favorite)
        composeRule.onNodeWithContentDescription(addFavorite).assertHasClickAction()
        composeRule.onNodeWithTag("detail_favorite").assertHeightIsAtLeast(48.dp)

        composeRule.onNodeWithContentDescription(addFavorite).performClick()

        composeRule.onNodeWithContentDescription(removeFavorite).assertHasClickAction()
        composeRule.onNodeWithContentDescription(addFavorite).assertDoesNotExist()
    }

    private fun setDetailScreen(
        movie: () -> Movie,
        onToggleFavorite: () -> Unit = {},
        onToggleWatchlist: () -> Unit = {},
        onToggleWatched: () -> Unit = {},
        isLibraryActionPending: Boolean = false,
    ) {
        composeRule.setContent {
            MovieHubTheme {
                MovieDetailScreen(
                    uiState =
                        MovieDetailUiState.Success(
                            movie = movie(),
                            credits = MovieCreditsUiState.Success(MovieCredits()),
                            isLibraryActionPending = isLibraryActionPending,
                            libraryObservation = LibraryObservationUiState.Ready,
                        ),
                    onBackClick = {},
                    onToggleFavorite = onToggleFavorite,
                    onToggleWatchlist = onToggleWatchlist,
                    onToggleWatched = onToggleWatched,
                    onRetry = {},
                    onRetryCredits = {},
                    onRetryRecommendations = {},
                    onRetryLibraryObservation = {},
                    libraryActionError = MovieDetailActionErrorState.None,
                    onLibraryActionErrorAcknowledged = {},
                    onRecommendationClick = {},
                )
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
