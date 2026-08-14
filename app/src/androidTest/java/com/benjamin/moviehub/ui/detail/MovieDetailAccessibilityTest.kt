package com.benjamin.moviehub.ui.detail

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.benjamin.moviehub.core.theme.MovieHubTheme
import com.benjamin.moviehub.domain.model.Movie
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
                MovieDetailContent(movie = movie, actors = emptyList(), onToggleFavorite = {})
            }
        }

        composeRule
            .onNodeWithContentDescription("Retirer des favoris")
            .assertHasClickAction()
    }
}
