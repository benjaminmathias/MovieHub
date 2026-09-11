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
    fun pullToRefresh_refreshesEveryCategoryOnce_andScrollingDoesNotRefreshAgain() {
        composeTestRule.waitUntil(timeoutMillis = 20_000) {
            categoryPageOneRequestCounts().values.all { it > 0 }
        }
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule
                .onAllNodesWithText("Film Populaire 1")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        val requestsAfterLoad = categoryPageOneRequestCounts()

        composeTestRule.onNodeWithTag("home_sections").performTouchInput { swipeDown() }

        composeTestRule.waitUntil(timeoutMillis = 20_000) {
            categoryPageOneRequestCounts().all { (category, count) ->
                count == requestsAfterLoad.getValue(category) + 1
            }
        }
        val requestsAfterRefresh = categoryPageOneRequestCounts()

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
            "Scrolling must not refresh any category again",
            requestsAfterRefresh,
            categoryPageOneRequestCounts(),
        )
    }

    private fun categoryPageOneRequestCounts(): Map<String, Int> =
        mapOf(
            "popular" to fakeApi.popularPagesRequested.count { it == 1 },
            "nowPlaying" to fakeApi.nowPlayingPagesRequested.count { it == 1 },
            "upcoming" to fakeApi.upcomingPagesRequested.count { it == 1 },
            "topRated" to fakeApi.topRatedPagesRequested.count { it == 1 },
        )
}
