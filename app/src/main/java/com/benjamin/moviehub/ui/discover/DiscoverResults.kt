package com.benjamin.moviehub.ui.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.benjamin.moviehub.R
import com.benjamin.moviehub.core.theme.MovieGridMinCellSize
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.ui.components.CompactMovieShimmerItem
import com.benjamin.moviehub.ui.components.EmptyStateView
import com.benjamin.moviehub.ui.components.ErrorRetryItem
import com.benjamin.moviehub.ui.components.PosterMovieItem
import com.benjamin.moviehub.ui.components.PosterMovieShimmerItem
import com.benjamin.moviehub.ui.components.isEmptyAfterEndOfPagination
import com.benjamin.moviehub.ui.components.isInitialError
import com.benjamin.moviehub.ui.components.isInitialLoading
import kotlinx.coroutines.flow.Flow

@Composable
internal fun DiscoverResults(
    discoverResults: Flow<PagingData<Movie>>,
    onMovieClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lazyPagingItems = discoverResults.collectAsLazyPagingItems()

    when {
        lazyPagingItems.isInitialLoading -> DiscoverLoading(modifier)
        lazyPagingItems.isInitialError ->
            EmptyStateView(
                message = stringResource(R.string.error_loading_movies),
                icon = Icons.Default.CloudOff,
                onRetry = { lazyPagingItems.retry() },
                modifier = modifier,
            )
        lazyPagingItems.isEmptyAfterEndOfPagination ->
            EmptyStateView(
                message = stringResource(R.string.discover_empty_results),
                icon = Icons.Default.SearchOff,
                modifier = modifier,
            )
        else -> DiscoverMovieGrid(lazyPagingItems, onMovieClick, modifier)
    }
}

@Composable
private fun DiscoverLoading(modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = MovieGridMinCellSize),
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(6) { PosterMovieShimmerItem() }
    }
}

@Composable
private fun DiscoverMovieGrid(
    lazyPagingItems: LazyPagingItems<Movie>,
    onMovieClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = MovieGridMinCellSize),
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(
            count = lazyPagingItems.itemCount,
            key = lazyPagingItems.itemKey { it.id },
        ) { index ->
            lazyPagingItems[index]?.let { movie ->
                PosterMovieItem(movie = movie, onMovieClick = onMovieClick)
            }
        }

        when (val state = lazyPagingItems.loadState.append) {
            is LoadState.Error ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ErrorRetryItem(
                        message = stringResource(R.string.error_loading_more_movies),
                        onRetry = { lazyPagingItems.retry() },
                    )
                }
            LoadState.Loading ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    CompactMovieShimmerItem()
                }
            is LoadState.NotLoading -> Unit
        }
    }
}
