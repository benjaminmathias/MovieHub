package com.benjamin.moviehub.ui.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.benjamin.moviehub.R
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.ui.components.CompactMovieItem
import com.benjamin.moviehub.ui.components.CompactMovieShimmerItem
import com.benjamin.moviehub.ui.components.EmptyStateView
import com.benjamin.moviehub.ui.components.ErrorRetryItem
import com.benjamin.moviehub.ui.components.MovieSearchBar
import com.benjamin.moviehub.ui.components.isEmptyAfterEndOfPagination
import com.benjamin.moviehub.ui.components.isInitialError
import com.benjamin.moviehub.ui.components.isInitialLoading
import kotlinx.coroutines.flow.Flow

@Composable
fun SearchScreen(
    searchResults: Flow<PagingData<Movie>>,
    searchQuery: String,
    onSearchChanged: (String) -> Unit,
    onMovieClick: (Int) -> Unit,
    onBack: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .consumeWindowInsets(paddingValues)
                    .imePadding(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                    )
                }
                MovieSearchBar(
                    query = searchQuery,
                    onQueryChanged = onSearchChanged,
                    modifier = Modifier.weight(1f).focusRequester(focusRequester),
                )
            }

            SearchResults(
                searchResults = searchResults,
                searchQuery = searchQuery,
                onMovieClick = onMovieClick,
            )
        }
    }
}

@Composable
private fun SearchResults(
    searchResults: Flow<PagingData<Movie>>,
    searchQuery: String,
    onMovieClick: (Int) -> Unit,
) {
    if (searchQuery.isBlank()) {
        EmptyStateView(
            message = stringResource(R.string.search_placeholder),
            icon = Icons.Default.Search,
            onRetry = null,
        )
        return
    }

    val lazyPagingItems = searchResults.collectAsLazyPagingItems()

    when {
        lazyPagingItems.isInitialLoading -> {
            SearchLoadingShimmer()
        }

        lazyPagingItems.isInitialError -> {
            EmptyStateView(
                message = stringResource(R.string.error_loading_movies),
                icon = Icons.Default.CloudOff,
                onRetry = { lazyPagingItems.retry() },
            )
        }

        lazyPagingItems.isEmptyAfterEndOfPagination -> {
            EmptyStateView(
                message = stringResource(R.string.empty_search_results, searchQuery.trim()),
                icon = Icons.Default.SearchOff,
                onRetry = null,
            )
        }

        else -> {
            SearchMovieList(
                lazyPagingItems = lazyPagingItems,
                onMovieClick = onMovieClick,
                onRetry = { lazyPagingItems.retry() },
            )
        }
    }
}

@Composable
private fun SearchLoadingShimmer() {
    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
        items(5) { CompactMovieShimmerItem() }
    }
}

@Composable
private fun SearchMovieList(
    lazyPagingItems: LazyPagingItems<Movie>,
    onMovieClick: (Int) -> Unit,
    onRetry: () -> Unit,
) {
    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
        items(
            count = lazyPagingItems.itemCount,
            key = lazyPagingItems.itemKey { it.id },
        ) { index ->
            lazyPagingItems[index]?.let { movie ->
                CompactMovieItem(
                    movie = movie,
                    onMovieClick = onMovieClick,
                )
            }
        }

        val appendState = lazyPagingItems.loadState.append
        if (appendState is LoadState.Error) {
            item {
                ErrorRetryItem(
                    message = stringResource(R.string.error_loading_movies),
                    onRetry = onRetry,
                )
            }
        }
        if (appendState is LoadState.Loading) {
            item { CompactMovieShimmerItem() }
        }
    }
}
