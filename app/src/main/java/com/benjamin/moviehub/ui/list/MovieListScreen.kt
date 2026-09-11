package com.benjamin.moviehub.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.benjamin.moviehub.R
import com.benjamin.moviehub.core.theme.HomeMovieCardWidth
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.model.MovieCategory
import com.benjamin.moviehub.ui.components.ErrorRetryItem
import com.benjamin.moviehub.ui.components.HeroMovieBanner
import com.benjamin.moviehub.ui.components.HeroMovieShimmer
import com.benjamin.moviehub.ui.components.RowMovieItem
import com.benjamin.moviehub.ui.components.RowMovieShimmerItem
import com.benjamin.moviehub.ui.components.isInitialError
import com.benjamin.moviehub.ui.components.isInitialLoading
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieListScreen(
    categoryMovies: Map<MovieCategory, Flow<PagingData<Movie>>>,
    heroMovie: Movie?,
    onMovieClick: (Int) -> Unit,
    onToggleFavorite: (Movie, Boolean) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(R.string.search_label),
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings_title),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        HomeContent(
            categoryMovies = categoryMovies,
            heroMovie = heroMovie,
            onMovieClick = onMovieClick,
            onToggleFavorite = onToggleFavorite,
            modifier = Modifier.fillMaxSize().padding(paddingValues),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    categoryMovies: Map<MovieCategory, Flow<PagingData<Movie>>>,
    heroMovie: Movie?,
    onMovieClick: (Int) -> Unit,
    onToggleFavorite: (Movie, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val refreshState = rememberPullToRefreshState()
    var refreshRequested by remember { mutableStateOf(false) }
    val rowLoading = remember { mutableStateMapOf<MovieCategory, Boolean>() }

    // One pending refresh per category. A row consumes its own entry when it composes, so a single
    // pull refreshes each category once without re-triggering when a row is disposed and recreated.
    val pendingRefreshes = remember { mutableStateMapOf<MovieCategory, Boolean>() }

    // Keep the pull indicator up from the gesture until every loaded row settles.
    LaunchedEffect(refreshRequested) {
        if (!refreshRequested) return@LaunchedEffect
        withTimeoutOrNull(3_000) {
            snapshotFlow { rowLoading.values.any { it } }.first { it }
            snapshotFlow { rowLoading.values.none { it } }.first { it }
        }
        refreshRequested = false
    }

    PullToRefreshBox(
        state = refreshState,
        isRefreshing = refreshRequested,
        onRefresh = {
            refreshRequested = true
            MovieCategory.entries.forEach { category -> pendingRefreshes[category] = true }
        },
        modifier = modifier,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("home_sections"),
            contentPadding = PaddingValues(bottom = 8.dp),
        ) {
            item(key = "hero") {
                if (heroMovie != null) {
                    HeroMovieBanner(
                        movie = heroMovie,
                        onMovieClick = onMovieClick,
                        onToggleFavorite = onToggleFavorite,
                    )
                } else {
                    HeroMovieShimmer()
                }
            }

            MovieCategory.entries.forEach { category ->
                item(key = category.key) {
                    CategoryRow(
                        category = category,
                        movies = categoryMovies.getValue(category),
                        refreshRequested = pendingRefreshes[category] == true,
                        onRefreshHandled = { pendingRefreshes[category] = false },
                        onMovieClick = onMovieClick,
                        onToggleFavorite = onToggleFavorite,
                        onLoadingChanged = { loading -> rowLoading[category] = loading },
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(
    category: MovieCategory,
    movies: Flow<PagingData<Movie>>,
    refreshRequested: Boolean,
    onRefreshHandled: () -> Unit,
    onMovieClick: (Int) -> Unit,
    onToggleFavorite: (Movie, Boolean) -> Unit,
    onLoadingChanged: (Boolean) -> Unit,
) {
    val lazyPagingItems = movies.collectAsLazyPagingItems()

    LaunchedEffect(refreshRequested) {
        if (refreshRequested) {
            lazyPagingItems.refresh()
            onRefreshHandled()
        }
    }

    val isRowLoading =
        lazyPagingItems.loadState.refresh is LoadState.Loading ||
            lazyPagingItems.loadState.mediator?.refresh is LoadState.Loading
    LaunchedEffect(isRowLoading) {
        onLoadingChanged(isRowLoading)
    }
    DisposableEffect(category) {
        onDispose { onLoadingChanged(false) }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Text(
            text = stringResource(category.labelRes),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

        when {
            lazyPagingItems.isInitialLoading -> CategoryRowLoadingShimmer()
            lazyPagingItems.isInitialError ->
                ErrorRetryItem(
                    message = stringResource(R.string.error_loading_movies),
                    onRetry = { lazyPagingItems.retry() },
                )
            else ->
                CategoryRowList(
                    lazyPagingItems = lazyPagingItems,
                    onMovieClick = onMovieClick,
                    onToggleFavorite = onToggleFavorite,
                )
        }
    }
}

@Composable
private fun CategoryRowList(
    lazyPagingItems: LazyPagingItems<Movie>,
    onMovieClick: (Int) -> Unit,
    onToggleFavorite: (Movie, Boolean) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().testTag("category_row"),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(
            count = lazyPagingItems.itemCount,
            key = lazyPagingItems.itemKey { it.id },
        ) { index ->
            lazyPagingItems[index]?.let { movie ->
                RowMovieItem(
                    movie = movie,
                    onMovieClick = onMovieClick,
                    onToggleFavorite = onToggleFavorite,
                    modifier = Modifier.width(HomeMovieCardWidth),
                )
            }
        }

        when (val appendState = lazyPagingItems.loadState.append) {
            is LoadState.Error -> {
                item {
                    ErrorRetryItem(
                        message = stringResource(R.string.error_loading_more_movies),
                        onRetry = { lazyPagingItems.retry() },
                    )
                }
            }

            LoadState.Loading -> {
                item {
                    RowMovieShimmerItem(modifier = Modifier.width(HomeMovieCardWidth))
                }
            }

            is LoadState.NotLoading -> Unit
        }
    }
}

@Composable
private fun CategoryRowLoadingShimmer() {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false,
    ) {
        items(3) {
            RowMovieShimmerItem(modifier = Modifier.width(HomeMovieCardWidth))
        }
    }
}

private val MovieCategory.labelRes: Int
    get() =
        when (this) {
            MovieCategory.POPULAR -> R.string.category_popular
            MovieCategory.NOW_PLAYING -> R.string.category_now_playing
            MovieCategory.UPCOMING -> R.string.category_upcoming
            MovieCategory.TOP_RATED -> R.string.category_top_rated
        }
