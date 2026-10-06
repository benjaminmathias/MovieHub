package com.benjamin.moviehub

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
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
    fun discoverTab_keepsAppliedFiltersAfterSwitchingTabs() {
        composeTestRule.onNodeWithTag("nav_tab_discover").performClick()
        composeTestRule.waitUntil(timeoutMillis = 8000) {
            composeTestRule.onAllNodesWithTag("discover_filter_button").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("discover_filter_button").performClick()
        composeTestRule.onNodeWithTag("discover_year_row").performClick()
        composeTestRule.onNodeWithTag("discover_year_option_other").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("discover_custom_year").performScrollTo().performTextInput("2020")
        composeTestRule.onNodeWithTag("discover_apply_filters").performClick()
        // Wait for the sheet dismissal animation to finish before asserting the badge,
        // otherwise the badge can be reported as not displayed while still covered.
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("discover_filter_sheet").fetchSemanticsNodes().isEmpty()
        }
        composeTestRule.onNodeWithTag("discover_filter_count").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_tab_home").performClick()
        composeTestRule.onNodeWithTag("nav_tab_discover").performClick()
        composeTestRule.waitUntil(timeoutMillis = 8000) {
            composeTestRule.onAllNodesWithTag("discover_filter_count").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("discover_filter_count").assertIsDisplayed()
    }

    @Test
    fun backFromDiscoverRoot_returnsToHomeTab() {
        composeTestRule.onNodeWithTag("nav_tab_discover").performClick()
        composeTestRule.waitUntil(timeoutMillis = 8000) {
            composeTestRule.onAllNodesWithTag("discover_filter_button").fetchSemanticsNodes().isNotEmpty()
        }

        pressBack()

        composeTestRule.waitUntil(timeoutMillis = 8000) {
            composeTestRule.onAllNodesWithTag("home_sections").fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(composeTestRule.onAllNodesWithTag("discover_filter_button").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun backFromLibraryRoot_returnsToHomeTab() {
        composeTestRule.onNodeWithTag("nav_tab_library").performClick()
        composeTestRule.waitUntil(timeoutMillis = 8000) {
            composeTestRule.onAllNodesWithTag("library_tab_watchlist").fetchSemanticsNodes().isNotEmpty()
        }

        pressBack()

        composeTestRule.waitUntil(timeoutMillis = 8000) {
            composeTestRule.onAllNodesWithTag("home_sections").fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(composeTestRule.onAllNodesWithTag("library_tab_watchlist").fetchSemanticsNodes().isEmpty())
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
