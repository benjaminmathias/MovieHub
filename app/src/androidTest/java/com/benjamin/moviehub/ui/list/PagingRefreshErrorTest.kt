package com.benjamin.moviehub.ui.list

import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.benjamin.moviehub.R
import com.benjamin.moviehub.TestStrings
import com.benjamin.moviehub.core.theme.MovieHubTheme
import com.benjamin.moviehub.data.local.MovieDatabase
import com.benjamin.moviehub.data.remote.FakeMovieApiService
import com.benjamin.moviehub.data.remote.MovieApiService
import com.benjamin.moviehub.data.remote.MovieResponse
import com.benjamin.moviehub.data.repository.MovieRepositoryImpl
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.model.MovieCategory
import com.benjamin.moviehub.inMemoryDatabase
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException

/**
 * Cached-rows refresh failure on the Home feed, exercised through the real Room-backed
 * RemoteMediator rather than a hand-rolled PagingSource.
 */
class PagingRefreshErrorTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var database: MovieDatabase
    private lateinit var api: ToggleFailApi

    @Before
    fun setUp() {
        database = inMemoryDatabase()
        api = ToggleFailApi(FakeMovieApiService())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun failedMediatorRefreshKeepsCachedRowsAndRetryRecovers() {
        val repository = MovieRepositoryImpl(api, database, database.movieDao())
        var items: LazyPagingItems<Movie>? = null

        composeRule.setContent {
            MovieHubTheme {
                val categoryMovies = remember(repository) { repository.getCategoryMovies(MovieCategory.POPULAR) }
                val lazyItems = categoryMovies.collectAsLazyPagingItems()
                items = lazyItems
                CategoryRow(
                    category = MovieCategory.POPULAR,
                    lazyPagingItems = lazyItems,
                    onMovieClick = {},
                    onToggleFavorite = {},
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 10_000) { api.popularPageRequests.isNotEmpty() }
        composeRule.waitUntil(timeoutMillis = 10_000) { hasText(CACHED_TITLE) }
        composeRule.onAllNodesWithText(TestStrings.get(R.string.error_refreshing_movies)).assertCountEquals(0)

        // A refresh whose mediator fetch fails must keep the cached rows and surface the compact retry.
        api.failPopular = true
        composeRule.runOnIdle { items?.refresh() }
        composeRule.waitUntil(timeoutMillis = 10_000) { hasText(TestStrings.get(R.string.error_refreshing_movies)) }
        composeRule.onNodeWithText(CACHED_TITLE).assertIsDisplayed()

        // Retrying through the real button now succeeds, so the message clears and the film stays.
        api.failPopular = false
        composeRule.onNodeWithText(TestStrings.get(R.string.retry)).assertIsDisplayed().performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            !hasText(TestStrings.get(R.string.error_refreshing_movies)) && hasText(CACHED_TITLE)
        }
    }

    private fun hasText(text: String): Boolean = composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    /** Real fake TMDB service with a switch that makes the popular endpoint fail on demand. */
    private class ToggleFailApi(
        private val delegate: MovieApiService,
    ) : MovieApiService by delegate {
        val popularPageRequests = mutableListOf<Int>()

        @Volatile
        var failPopular: Boolean = false

        override suspend fun getPopularMovies(page: Int): MovieResponse {
            popularPageRequests += page
            if (failPopular) throw IOException("offline")
            return delegate.getPopularMovies(page)
        }
    }

    private companion object {
        const val CACHED_TITLE = "Film Populaire 1"
    }
}
