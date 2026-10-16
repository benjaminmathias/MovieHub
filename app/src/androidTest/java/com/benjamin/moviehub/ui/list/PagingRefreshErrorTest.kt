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
import com.benjamin.moviehub.data.remote.movieDto
import com.benjamin.moviehub.data.repository.MovieRepositoryImpl
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.model.MovieCategory
import com.benjamin.moviehub.inMemoryDatabase
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

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
    fun failedMediatorRefreshKeepsCachedRowsAndRetryLoadsFreshRows() {
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

        composeRule.waitUntil(timeoutMillis = 10_000) { api.popularPageRequestCount.get() > 0 }
        composeRule.waitUntil(timeoutMillis = 10_000) { hasText(CACHED_TITLE) }
        composeRule.onAllNodesWithText(TestStrings.get(R.string.error_refreshing_movies)).assertCountEquals(0)

        // A refresh whose mediator fetch fails must keep the cached rows and surface the compact retry.
        api.failPopular = true
        composeRule.runOnIdle { items?.refresh() }
        composeRule.waitUntil(timeoutMillis = 10_000) { hasText(TestStrings.get(R.string.error_refreshing_movies)) }
        composeRule.onNodeWithText(CACHED_TITLE).assertIsDisplayed()

        // The successful retry must replace the cached row with the refreshed response.
        api.failPopular = false
        api.returnFreshPopular = true
        val requestsBeforeRetry = api.popularPageRequestCount.get()
        composeRule.onNodeWithText(TestStrings.get(R.string.retry)).assertIsDisplayed().performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            api.popularPageRequestCount.get() == requestsBeforeRetry + 1 &&
                !hasText(TestStrings.get(R.string.error_refreshing_movies)) &&
                hasText(REFRESHED_TITLE)
        }
        composeRule.onNodeWithText(REFRESHED_TITLE).assertIsDisplayed()
        composeRule.onAllNodesWithText(CACHED_TITLE).assertCountEquals(0)
    }

    private fun hasText(text: String): Boolean = composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    private class ToggleFailApi(
        private val delegate: MovieApiService,
    ) : MovieApiService by delegate {
        val popularPageRequestCount = AtomicInteger()

        @Volatile
        var failPopular: Boolean = false

        @Volatile
        var returnFreshPopular: Boolean = false

        override suspend fun getPopularMovies(page: Int): MovieResponse {
            popularPageRequestCount.incrementAndGet()
            if (failPopular) throw IOException("offline")
            if (returnFreshPopular) return MovieResponse(listOf(movieDto(id = 99, title = REFRESHED_TITLE)))
            return delegate.getPopularMovies(page)
        }
    }

    private companion object {
        const val CACHED_TITLE = "Film Populaire 1"
        const val REFRESHED_TITLE = "Film Populaire actualisé"
    }
}
