package com.benjamin.moviehub.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.benjamin.moviehub.R
import com.benjamin.moviehub.TestStrings
import com.benjamin.moviehub.core.theme.MovieHubTheme
import org.junit.Rule
import org.junit.Test

class MovieSearchBarAccessibilityTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun searchFieldDisplaysItsExplicitAccessibleLabel() {
        composeRule.setContent {
            MovieHubTheme {
                MovieSearchBar(
                    query = "",
                    onQueryChanged = {},
                )
            }
        }

        composeRule.onNodeWithText(TestStrings.get(R.string.search_label)).assertIsDisplayed()
    }
}
