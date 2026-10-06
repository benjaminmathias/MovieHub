package com.benjamin.moviehub

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.benjamin.moviehub.data.local.MovieDao
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class MovieSearchFavoriteTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var movieDao: MovieDao

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun searchInterstellar_selectMovie_andAddToFavorites() {
        // Search now lives on its own screen, opened from the home top bar.
        composeTestRule
            .onNodeWithContentDescription(TestStrings.get(R.string.search_label))
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = 30_000) {
            composeTestRule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule
            .onNode(hasSetTextAction())
            .performTextInput("Interstellar")

        composeTestRule.waitUntil(timeoutMillis = 30_000) {
            composeTestRule.onAllNodesWithTag("movie_item").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onAllNodesWithTag("movie_item").onFirst().performClick()

        composeTestRule.waitUntil(timeoutMillis = 30_000) {
            composeTestRule
                .onAllNodesWithContentDescription(TestStrings.get(R.string.favorite))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithTag("detail_screen").assertIsDisplayed()

        composeTestRule
            .onNodeWithContentDescription(TestStrings.get(R.string.favorite))
            .performClick()

        // The favorite must reach Room, not just the optimistic button state.
        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            runBlocking { movieDao.getMovieById(INTERSTELLAR_ID)?.isFavorite == true }
        }

        composeTestRule
            .onNodeWithContentDescription(TestStrings.get(R.string.remove_favorite))
            .assertIsDisplayed()
    }

    private companion object {
        private const val INTERSTELLAR_ID = 157336
    }
}
