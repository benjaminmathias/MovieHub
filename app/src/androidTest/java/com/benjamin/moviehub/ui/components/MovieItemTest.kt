package com.benjamin.moviehub.ui.components

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import com.benjamin.moviehub.core.theme.MovieHubTheme
import com.benjamin.moviehub.domain.model.Movie
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MovieItemTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun posterAndCompactVariantsRender() {
        val clickedIds = mutableListOf<Int>()
        val movie =
            Movie(
                id = 1,
                title = "A deliberately long movie title that ellipsizes safely",
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
                MovieItem(movie = movie, onMovieClick = clickedIds::add)
                MovieItem(movie = movie.copy(id = 2), onMovieClick = clickedIds::add, compact = true)
            }
        }

        composeRule.onAllNodesWithTag("movie_item").assertCountEquals(2)
        composeRule.onAllNodesWithTag("movie_item")[0].performClick()
        composeRule.onAllNodesWithTag("movie_item")[1].performClick()
        assertEquals(listOf(1, 2), clickedIds)
    }
}
