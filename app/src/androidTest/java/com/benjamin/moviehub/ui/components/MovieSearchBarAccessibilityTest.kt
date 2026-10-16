package com.benjamin.moviehub.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
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

        val label = TestStrings.get(R.string.search_label)
        composeRule.onNode(hasSetTextAction() and hasText(label)).assertIsDisplayed()
    }
}
