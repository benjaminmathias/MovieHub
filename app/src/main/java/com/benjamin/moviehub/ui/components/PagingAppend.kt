package com.benjamin.moviehub.ui.components

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.Composable
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.benjamin.moviehub.domain.model.Movie

/** Paged [Movie] rows with stable keys, keeping the null placeholder slots hidden. */
internal fun LazyListScope.moviePagingItems(
    items: LazyPagingItems<Movie>,
    itemContent: @Composable (Movie) -> Unit,
) {
    items(
        count = items.itemCount,
        key = items.itemKey { it.id },
        contentType = { "movie" },
    ) { index ->
        items[index]?.let { movie -> itemContent(movie) }
    }
}

/** Shared append error/loading footer of a paged [Movie] list. */
internal fun LazyListScope.movieAppendFooter(
    items: LazyPagingItems<Movie>,
    errorMessage: String,
    loading: @Composable () -> Unit,
) {
    when (items.loadState.append) {
        is LoadState.Error ->
            item(contentType = "append-error") {
                EmptyStateView(message = errorMessage, onRetry = { items.retry() }, compact = true)
            }

        LoadState.Loading -> item(contentType = "append-loading") { loading() }
        is LoadState.NotLoading -> Unit
    }
}

/** Paged [Movie] cells with stable keys, keeping the null placeholder slots hidden. */
internal fun LazyGridScope.moviePagingItems(
    items: LazyPagingItems<Movie>,
    itemContent: @Composable (Movie) -> Unit,
) {
    items(
        count = items.itemCount,
        key = items.itemKey { it.id },
        contentType = { "movie" },
    ) { index ->
        items[index]?.let { movie -> itemContent(movie) }
    }
}

/** Shared append error/loading footer of a paged [Movie] grid, spanning every column. */
internal fun LazyGridScope.movieAppendFooter(
    items: LazyPagingItems<Movie>,
    errorMessage: String,
    loading: @Composable () -> Unit,
) {
    when (items.loadState.append) {
        is LoadState.Error ->
            item(span = { GridItemSpan(maxLineSpan) }, contentType = "append-error") {
                EmptyStateView(message = errorMessage, onRetry = { items.retry() }, compact = true)
            }

        LoadState.Loading -> item(span = { GridItemSpan(maxLineSpan) }, contentType = "append-loading") { loading() }
        is LoadState.NotLoading -> Unit
    }
}
