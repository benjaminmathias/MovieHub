package com.benjamin.moviehub.ui.list

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
            CenterAlignedTopAppBar(
                {
                    Text(
                        stringResource(R.string.app_name),
                        color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
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
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            if (searchQuery.isEmpty()) {
                Text(
                    text = stringResource(R.string.movie_hub_popular),
                    style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            MovieSearchBar(
                query = searchQuery,
                onQueryChanged =
                onSearchChanged,
            )

            Spacer(modifier = Modifier.size(4.dp))

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
                        Column { repeat(5) { MovieShimmerItem() } }
                    }

                    isError -> {
                        val errorState =
                            (mediatorLoadState as? LoadState.Error)
                                ?: (refreshLoadState as? LoadState.Error)
                        EmptyStateView(
                            message =
                                errorState?.error?.localizedMessage
                                    ?: stringResource(R.string.error_loading_movies),
                            icon = Icons.Default.CloudOff,
                            onRetry = { lazyPagingItems.retry() },
                        )
                    }

                    isEmpty -> {
                        if (searchQuery.isNotEmpty()) {
                            // Search without result
                            EmptyStateView(
                                message =
                                    stringResource(
                                        R.string.empty_search_results,
                                        searchQuery,
                                    ),
                                icon = Icons.Default.SearchOff,
                                onRetry = null,
                            )
                        } else {
                            // Empty popular list
                            EmptyStateView(
                                message = stringResource(R.string.no_movie_available),
                                icon = Icons.Default.Movie,
                                onRetry = { lazyPagingItems.refresh() },
                            )
                        }
                    }

                    // Show data
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(
                                count = lazyPagingItems.itemCount,
                                key = lazyPagingItems.itemKey { it.id },
                            ) { index ->
                                lazyPagingItems[index]?.let { movie ->
                                    MovieItem(
                                        movie = movie,
                                        onMovieClick = onMovieClick,
                                        compact = searchQuery.isNotEmpty(),
                                    )
                                }
                            }

                            // Handle feedback if loading pages fails
                            val appendState = lazyPagingItems.loadState.append
                            if (appendState is LoadState.Error) {
                                item {
                                    ErrorRetryItem(
                                        message =
                                            appendState.error.localizedMessage
                                                ?: stringResource(R.string.error_loading_movies),
                                        onRetry = { lazyPagingItems.retry() },
                                    )
                                }
                            }

                            if (lazyPagingItems.loadState.append is LoadState.Loading) {
                                item { MovieShimmerItem() }
                            }
                        }
                    }
                }
            }
        }
    }
}
