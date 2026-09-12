package com.benjamin.moviehub.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.benjamin.moviehub.core.theme.MovieHubTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MovieSearchBarTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun clearActionIsAccessibleAndResetsQuery() {
        var query by mutableStateOf("Interstellar")

        composeRule.setContent {
            MovieHubTheme {
                MovieSearchBar(
                    query = query,
                    onQueryChanged = { query = it },
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Effacer")
            .assertIsDisplayed()
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        composeRule.runOnIdle { assertEquals("", query) }
    }
}
