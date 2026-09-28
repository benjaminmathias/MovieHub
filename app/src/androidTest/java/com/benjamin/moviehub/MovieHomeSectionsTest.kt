package com.benjamin.moviehub

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import com.benjamin.moviehub.data.remote.FakeMovieApiService
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class MovieHomeSectionsTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var fakeApi: FakeMovieApiService

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun home_showsSeveralCategoriesOnTheSamePage() {
        // Synchronize on the fake API request itself instead of an arbitrary wait: the popular feed
        // is the first one to load, then the UI renders it from Room.
        composeTestRule.waitUntil(timeoutMillis = 20_000) {
            fakeApi.popularPagesRequested.contains(1)
        }
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule
                .onAllNodesWithText("Film Populaire 1")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithText(TestStrings.get(R.string.category_popular)).assertIsDisplayed()

        composeTestRule.onNodeWithTag("home_sections").performScrollToNode(hasText(TestStrings.get(R.string.category_upcoming)))

        // Scrolling a row into view triggers its lazy category load.
        composeTestRule.waitUntil(timeoutMillis = 20_000) {
            fakeApi.upcomingPagesRequested.contains(1)
        }
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule
                .onAllNodesWithText("Film Prochainement 1")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Initial load must not hit the same endpoints repeatedly.
        assertEquals(1, fakeApi.popularPagesRequested.count { it == 1 })
        assertEquals(1, fakeApi.upcomingPagesRequested.count { it == 1 })
    }
}
