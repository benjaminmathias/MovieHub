package com.benjamin.moviehub

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class MovieNavigationTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun clickMovie_navigatesToDetailScreen() {
        composeTestRule.waitUntil(timeoutMillis = 8000) {
            composeTestRule.onAllNodesWithTag("movie_item").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule
            .onAllNodesWithTag("movie_item")
            .onFirst()
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("detail_screen").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("detail_screen").assertIsDisplayed()
    }

    @Test
    fun backFromDetail_returnsToOriginatingTab() {
        composeTestRule.onNodeWithTag("nav_tab_discover").performClick()
        composeTestRule.waitUntil(timeoutMillis = 8000) {
            composeTestRule.onAllNodesWithTag("movie_item").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onAllNodesWithTag("movie_item").onFirst().performClick()
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("detail_screen").fetchSemanticsNodes().isNotEmpty()
        }

        pressBack()

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("discover_filter_button").fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(composeTestRule.onAllNodesWithTag("detail_screen").fetchSemanticsNodes().isEmpty())
    }

    private fun pressBack() {
        composeTestRule.runOnUiThread {
            composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
        }
    }
}
