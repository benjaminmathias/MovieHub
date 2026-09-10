package com.benjamin.moviehub

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import com.benjamin.moviehub.data.remote.FakeMovieApiService
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class MovieHomeRefreshTest {
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
    fun pullToRefresh_refreshesVisibleCategoryOnce_andScrollingBackDoesNotRefreshAgain() {
        composeTestRule.waitUntil(timeoutMillis = 20_000) {
            fakeApi.popularPagesRequested.contains(1)
        }
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule
                .onAllNodesWithText("Film Populaire 1")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        val popularRequestsAfterLoad = fakeApi.popularPagesRequested.count { it == 1 }

        composeTestRule.onNodeWithTag("home_sections").performTouchInput { swipeDown() }

        composeTestRule.waitUntil(timeoutMillis = 20_000) {
            fakeApi.popularPagesRequested.count { it == 1 } == popularRequestsAfterLoad + 1
        }
        val popularRequestsAfterRefresh = fakeApi.popularPagesRequested.count { it == 1 }

        // Leave the popular row, then bring it back: recomposition must not refresh it again.
        composeTestRule.onNodeWithTag("home_sections").performScrollToNode(hasText("Prochainement"))
        composeTestRule.onNodeWithTag("home_sections").performScrollToNode(hasText("Populaires"))
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule
                .onAllNodesWithText("Film Populaire 1")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        assertEquals(
            "Scrolling a category back into composition must not refresh it again",
            popularRequestsAfterRefresh,
            fakeApi.popularPagesRequested.count { it == 1 },
        )
    }
}
