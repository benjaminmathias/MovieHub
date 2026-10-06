package com.benjamin.moviehub.ui.components

import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems

/** Coarse phase of a paged feed, used for shared loading/empty/error rendering. */
enum class PagingPhase {
    LOADING,
    ERROR,
    EMPTY,
    CONTENT,
}

/** First page is loading and nothing is displayed yet. */
val LazyPagingItems<*>.isInitialLoading: Boolean
    get() =
        itemCount == 0 &&
            (loadState.refresh is LoadState.Loading || loadState.mediator?.refresh is LoadState.Loading)

/** A refresh is running, whether driven by the PagingSource or the RemoteMediator. */
val LazyPagingItems<*>.isRefreshing: Boolean
    get() =
        loadState.refresh is LoadState.Loading ||
            loadState.mediator?.refresh is LoadState.Loading

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

/**
 * A refresh failed while the feed already displays rows. The source and the mediator are checked
 * explicitly because the convenience `loadState.refresh` can report NotLoading when the mediator
 * succeeds even though the source refresh failed.
 */
internal fun isRefreshError(
    hasRows: Boolean,
    refresh: LoadState,
    sourceRefresh: LoadState,
    mediatorRefresh: LoadState?,
): Boolean = hasRows && (refresh is LoadState.Error || sourceRefresh is LoadState.Error || mediatorRefresh is LoadState.Error)

/** A refresh failed while the feed keeps displaying its rows, whether the source or the mediator drove it. */
val LazyPagingItems<*>.isRefreshError: Boolean
    get() = isRefreshError(itemCount > 0, loadState.refresh, loadState.source.refresh, loadState.mediator?.refresh)

val LazyPagingItems<*>.phase: PagingPhase
    get() =
        when {
            isInitialLoading -> PagingPhase.LOADING
            isInitialError -> PagingPhase.ERROR
            isEmptyAfterEndOfPagination -> PagingPhase.EMPTY
            else -> PagingPhase.CONTENT
        }
