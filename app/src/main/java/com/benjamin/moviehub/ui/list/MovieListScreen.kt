package com.benjamin.moviehub.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.SearchOff
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.benjamin.moviehub.R
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.ui.components.EmptyStateView
import com.benjamin.moviehub.ui.components.ErrorRetryItem
import com.benjamin.moviehub.ui.components.MovieItem
import com.benjamin.moviehub.ui.components.MovieSearchBar
import com.benjamin.moviehub.ui.components.MovieShimmerItem
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieListScreen(
    pagedMovies: Flow<PagingData<Movie>>,
    searchQuery: String,
    onSearchChanged: (String) -> Unit,
    onMovieClick: (Int) -> Unit,
    onSettingsClick: () -> Unit,
) {
    val refreshState = rememberPullToRefreshState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.app_name),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    },
                    actions = {
                        IconButton(onClick = onSettingsClick) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = stringResource(R.string.settings_title),
                            )
                        }
                    },
                )
                MovieSearchBar(
                    query = searchQuery,
                    onQueryChanged = onSearchChanged,
                )
            }
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .imePadding(),
        ) {
            if (searchQuery.isBlank()) {
                Text(
                    text = stringResource(R.string.movie_hub_popular),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            val lazyPagingItems = pagedMovies.collectAsLazyPagingItems()
            val combinedLoadStates = lazyPagingItems.loadState
            val refreshLoadState = combinedLoadStates.refresh
            val mediatorLoadState = combinedLoadStates.mediator?.refresh
            val isMediatorLoadingOrNull =
                mediatorLoadState == null || mediatorLoadState is LoadState.Loading
            val isInitialLoading =
                (refreshLoadState is LoadState.Loading || isMediatorLoadingOrNull) &&
                    lazyPagingItems.itemCount == 0
            val isAppendEndOfPagination =
                (combinedLoadStates.append as? LoadState.NotLoading)?.endOfPaginationReached == true
            val isEmpty =
                refreshLoadState is LoadState.NotLoading &&
                    isAppendEndOfPagination &&
                    lazyPagingItems.itemCount == 0
            val isError =
                (refreshLoadState is LoadState.Error || mediatorLoadState is LoadState.Error) &&
                    lazyPagingItems.itemCount == 0
            val appendErrorMessage = stringResource(R.string.error_loading_more_movies)

            PullToRefreshBox(
                state = refreshState,
                isRefreshing =
                    !isInitialLoading &&
                        (refreshLoadState is LoadState.Loading || mediatorLoadState is LoadState.Loading),
                onRefresh = { lazyPagingItems.refresh() },
                modifier = Modifier.fillMaxSize(),
            ) {
                when {
                    isInitialLoading -> {
                        if (searchQuery.isBlank()) {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 144.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(6) { MovieShimmerItem() }
                            }
                        } else {
                            LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                                items(5) { MovieShimmerItem(compact = true) }
                            }
                        }
                    }

                    isError -> {
                        EmptyStateView(
                            message = stringResource(R.string.error_loading_movies),
                            icon = Icons.Default.CloudOff,
                            onRetry = { lazyPagingItems.retry() },
                        )
                    }

                    isEmpty -> {
                        if (searchQuery.isNotBlank()) {
                            EmptyStateView(
                                message =
                                    stringResource(
                                        R.string.empty_search_results,
                                        searchQuery.trim(),
                                    ),
                                icon = Icons.Default.SearchOff,
                                onRetry = null,
                            )
                        } else {
                            EmptyStateView(
                                message = stringResource(R.string.no_movie_available),
                                icon = Icons.Default.Movie,
                                onRetry = { lazyPagingItems.refresh() },
                            )
                        }
                    }

                    searchQuery.isBlank() -> {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 144.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(
                                count = lazyPagingItems.itemCount,
                                key = lazyPagingItems.itemKey { it.id },
                            ) { index ->
                                lazyPagingItems[index]?.let { movie ->
                                    MovieItem(movie = movie, onMovieClick = onMovieClick)
                                }
                            }
                            appendItems(
                                appendState = lazyPagingItems.loadState.append,
                                onRetry = { lazyPagingItems.retry() },
                                errorMessage = appendErrorMessage,
                            )
                        }
                    }

                    else -> {
                        LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                            items(
                                count = lazyPagingItems.itemCount,
                                key = lazyPagingItems.itemKey { it.id },
                            ) { index ->
                                lazyPagingItems[index]?.let { movie ->
                                    MovieItem(
                                        movie = movie,
                                        onMovieClick = onMovieClick,
                                        compact = true,
                                    )
                                }
                            }

                            val appendState = lazyPagingItems.loadState.append
                            if (appendState is LoadState.Error) {
                                item {
                                    ErrorRetryItem(
                                        message = stringResource(R.string.error_loading_movies),
                                        onRetry = { lazyPagingItems.retry() },
                                    )
                                }
                            }
                            if (appendState is LoadState.Loading) {
                                item { MovieShimmerItem(compact = true) }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.grid.LazyGridScope.appendItems(
    appendState: LoadState,
    onRetry: () -> Unit,
    errorMessage: String,
) {
    when (appendState) {
        is LoadState.Error -> {
            item(span = { GridItemSpan(maxLineSpan) }) {
                ErrorRetryItem(
                    message = errorMessage,
                    onRetry = onRetry,
                )
            }
        }

        LoadState.Loading -> {
            item(span = { GridItemSpan(maxLineSpan) }) {
                MovieShimmerItem()
            }
        }

        is LoadState.NotLoading -> Unit
    }
}
