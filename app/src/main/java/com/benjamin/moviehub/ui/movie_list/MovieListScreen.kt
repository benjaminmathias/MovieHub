package com.benjamin.moviehub.ui.movie_list

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
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
    retryGlobal: () -> Unit
) {

    val refreshState = rememberPullToRefreshState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                {
                    Text(
                        if (searchQuery.isEmpty()) stringResource(R.string.movie_hub_popular) else stringResource(
                            R.string.search
                        )
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            MovieSearchBar(
                query = searchQuery,
                onQueryChanged =
                    onSearchChanged
            )
            AnimatedContent(
                targetState = uiState,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(
                        animationSpec = tween(
                            300
                        )
                    )
                },
                label = "StateAnimation"
            ) { state ->
                when (state) {
                    is MovieListUiState.Loading -> {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            repeat(5) {
                                MovieShimmerItem()
                            }
                        }
                    }

                    is MovieListUiState.Success -> {
                        val pagedMovies = state.pagedMovies.collectAsLazyPagingItems()

                        val isInitialLoading = pagedMovies.loadState.refresh is LoadState.Loading
                        val isEmpty =
                            pagedMovies.loadState.refresh is LoadState.NotLoading && pagedMovies.itemCount == 0

                        PullToRefreshBox(
                            state = refreshState,
                            isRefreshing = isInitialLoading && pagedMovies.itemCount > 0,
                            onRefresh = { pagedMovies.refresh() },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            when {
                                // Initial load
                                isInitialLoading && pagedMovies.itemCount == 0 -> {
                                    Column {
                                        repeat(5) { MovieShimmerItem() }
                                    }
                                }

                                // Handle error on initial load
                                pagedMovies.loadState.refresh is LoadState.Error && pagedMovies.itemCount == 0 -> {
                                    val error =
                                        (pagedMovies.loadState.refresh as LoadState.Error).error
                                    EmptyStateView(
                                        message = error.localizedMessage
                                            ?: stringResource(R.string.error_loading_movies),
                                        icon = Icons.Default.CloudOff,
                                        onRetry = { pagedMovies.retry() }
                                    )
                                }

                                // Empty list
                                isEmpty -> {
                                    EmptyStateView(
                                        message = state.emptyMessage?.asString()
                                            ?: stringResource(R.string.no_movie_available),
                                        onRetry = { pagedMovies.retry() }
                                    )
                                }

                                // Show data
                                else -> {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(
                                            count = pagedMovies.itemCount,
                                            key = { index ->
                                                val movie = pagedMovies[index]
                                                movie?.let { "${it.id}_$index" } ?: index
                                            }
                                        ) { index ->
                                            pagedMovies[index]?.let { movie ->
                                                MovieItem(
                                                    movie = movie,
                                                    onMovieClick = onMovieClick
                                                )
                                            }
                                        }

                                        // Handle feedback if loading pages fails
                                        val appendState = pagedMovies.loadState.append
                                        if (appendState is LoadState.Error) {
                                            item {
                                                ErrorRetryItem(
                                                    message = appendState.error.localizedMessage
                                                        ?: stringResource(R.string.error_loading_movies),
                                                    onRetry = { pagedMovies.retry() }
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

                    is MovieListUiState.Error -> {
                        val errorMessage = state.errorMessage?.asString()
                            ?: stringResource(R.string.error_loading_movies)
                        EmptyStateView(
                            message = stringResource(R.string.error_prefix, errorMessage),
                            icon = Icons.Default.Error,
                            onRetry = retryGlobal
                        )
                    }
                }
            }
        }
    }
}


