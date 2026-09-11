package com.benjamin.moviehub.ui.components

import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems

/** First page is loading and nothing is displayed yet. */
val LazyPagingItems<*>.isInitialLoading: Boolean
    get() =
        itemCount == 0 &&
            (loadState.refresh is LoadState.Loading || loadState.mediator?.refresh is LoadState.Loading)

/** First page failed and nothing is displayed yet. */
val LazyPagingItems<*>.isInitialError: Boolean
    get() =
        itemCount == 0 &&
            (loadState.refresh is LoadState.Error || loadState.mediator?.refresh is LoadState.Error)

/** The feed loaded successfully but is empty and has no more pages. */
val LazyPagingItems<*>.isEmptyAfterEndOfPagination: Boolean
    get() =
        itemCount == 0 &&
            loadState.refresh is LoadState.NotLoading &&
            (loadState.append as? LoadState.NotLoading)?.endOfPaginationReached == true
