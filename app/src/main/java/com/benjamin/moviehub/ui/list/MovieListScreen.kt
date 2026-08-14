package com.benjamin.moviehub.ui.list

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.benjamin.moviehub.R
import com.benjamin.moviehub.ui.components.EmptyStateView
import com.benjamin.moviehub.ui.components.ErrorRetryItem
import com.benjamin.moviehub.ui.components.MovieItem
import com.benjamin.moviehub.ui.components.MovieSearchBar
import com.benjamin.moviehub.ui.components.MovieShimmerItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieListScreen(
    uiState: MovieListUiState,
    searchQuery: String,
    onSearchChanged: (String) -> Unit,
    onMovieClick: (Int) -> Unit,
    retryGlobal: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    val refreshState = rememberPullToRefreshState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                {
                    Text(
                        if (searchQuery.isEmpty()) {
                            stringResource(R.string.movie_hub_popular)
                        } else {
                            stringResource(
                                R.string.search,
                            )
                        },
                    )
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Paramètres",
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
            MovieSearchBar(
                query = searchQuery,
                onQueryChanged =
                onSearchChanged,
            )

            Spacer(modifier = Modifier.size(4.dp))

            when (val state = uiState) {
                is MovieListUiState.Loading -> {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        repeat(5) {
                            MovieShimmerItem()
                        }
                    }
                }

                is MovieListUiState.Success -> {
                    // Get paged data from Flow
                    val pagedMovies = state.pagedMovies.collectAsLazyPagingItems()
                    // Get combined state (room + mediator)
                    val combinedLoadStates = pagedMovies.loadState

                    val refreshLoadState = combinedLoadStates.refresh
                    val mediatorLoadState = combinedLoadStates.mediator?.refresh

                    // Forcing loading if Mediator is null (first load) or running
                    val isMediatorLoadingOrNull =
                        mediatorLoadState == null || mediatorLoadState is LoadState.Loading

                    // Shimmer on if loading and empty list
                    val isInitialLoading =
                        (refreshLoadState is LoadState.Loading || isMediatorLoadingOrNull) &&
                            pagedMovies.itemCount == 0

                    // Checking that pagination is completed
                    val isAppendEndOfPagination =
                        (combinedLoadStates.append as? LoadState.NotLoading)?.endOfPaginationReached == true

                    // Empty state if : not loading, end of pagination and no items
                    val isEmpty =
                        refreshLoadState is LoadState.NotLoading &&
                            isAppendEndOfPagination &&
                            pagedMovies.itemCount == 0

                    val isError =
                        (refreshLoadState is LoadState.Error || mediatorLoadState is LoadState.Error) &&
                            pagedMovies.itemCount == 0

                    PullToRefreshBox(
                        state = refreshState,
                        isRefreshing =
                            !isInitialLoading && (refreshLoadState is LoadState.Loading || mediatorLoadState is LoadState.Loading),
                        onRefresh = { pagedMovies.refresh() },
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
                                    onRetry = { pagedMovies.retry() },
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
                                        onRetry = { pagedMovies.refresh() },
                                    )
                                }
                            }

                            // Show data
                            else -> {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                ) {
                                    items(
                                        count = pagedMovies.itemCount,
                                        key = pagedMovies.itemKey { it.id },
                                    ) { index ->
                                        pagedMovies[index]?.let { movie ->
                                            MovieItem(
                                                movie = movie,
                                                onMovieClick = onMovieClick,
                                            )
                                        }
                                    }

                                    // Handle feedback if loading pages fails
                                    val appendState = pagedMovies.loadState.append
                                    if (appendState is LoadState.Error) {
                                        item {
                                            ErrorRetryItem(
                                                message =
                                                    appendState.error.localizedMessage
                                                        ?: stringResource(R.string.error_loading_movies),
                                                onRetry = { pagedMovies.retry() },
                                            )
                                        }
                                    }

                                    if (pagedMovies.loadState.append is LoadState.Loading) {
                                        item { MovieShimmerItem() }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
