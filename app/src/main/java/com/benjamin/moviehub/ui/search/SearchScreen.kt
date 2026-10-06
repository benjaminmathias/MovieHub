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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
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
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.benjamin.moviehub.R
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.ui.components.CompactMovieItem
import com.benjamin.moviehub.ui.components.CompactMovieShimmerItem
import com.benjamin.moviehub.ui.components.CompactMovieShimmerList
import com.benjamin.moviehub.ui.components.EmptyStateView
import com.benjamin.moviehub.ui.components.MovieSearchBar
import com.benjamin.moviehub.ui.components.PagingStatus
import com.benjamin.moviehub.ui.components.isRefreshError
import com.benjamin.moviehub.ui.components.movieAppendFooter
import com.benjamin.moviehub.ui.components.moviePagingItems
import com.benjamin.moviehub.ui.components.phase
import kotlinx.coroutines.flow.Flow

@Composable
fun SearchScreen(
    searchResults: Flow<PagingData<Movie>>,
    searchQuery: String,
    activeSearchQuery: String,
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
                activeSearchQuery = activeSearchQuery,
                onMovieClick = onMovieClick,
            )
        }
    }
}

@Composable
private fun SearchResults(
    searchResults: Flow<PagingData<Movie>>,
    searchQuery: String,
    activeSearchQuery: String,
    onMovieClick: (Int) -> Unit,
) {
    val items = searchResults.collectAsLazyPagingItems()

    if (searchQuery.isBlank()) {
        EmptyStateView(
            message = stringResource(R.string.search_placeholder),
            icon = Icons.Default.Search,
        )
        return
    }

    if (searchQuery.trim() != activeSearchQuery) {
        CompactMovieShimmerList()
        return
    }

    PagingStatus(
        phase = items.phase,
        onRetry = { items.retry() },
        emptyMessage = stringResource(R.string.empty_search_results, searchQuery.trim()),
        loading = { CompactMovieShimmerList() },
    ) {
        SearchMovieList(
            lazyPagingItems = items,
            onMovieClick = onMovieClick,
        )
    }
}

@Composable
private fun SearchMovieList(
    lazyPagingItems: LazyPagingItems<Movie>,
    onMovieClick: (Int) -> Unit,
) {
    val appendErrorMessage = stringResource(R.string.error_loading_movies)

    Column(modifier = Modifier.fillMaxSize()) {
        if (lazyPagingItems.isRefreshError) {
            EmptyStateView(
                message = stringResource(R.string.error_refreshing_movies),
                onRetry = { lazyPagingItems.retry() },
                compact = true,
            )
        }

        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(vertical = 8.dp)) {
            moviePagingItems(lazyPagingItems) { movie ->
                CompactMovieItem(movie = movie, onMovieClick = onMovieClick)
            }
            movieAppendFooter(
                items = lazyPagingItems,
                errorMessage = appendErrorMessage,
                loading = { CompactMovieShimmerItem() },
            )
        }
    }
}
