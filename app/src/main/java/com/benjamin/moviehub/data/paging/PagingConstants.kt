package com.benjamin.moviehub.data.paging

import androidx.paging.PagingConfig

internal const val PAGE_SIZE = 20
internal const val PREFETCH_DISTANCE = 5
internal const val INITIAL_LOAD_SIZE = 20

internal val moviePagingConfig =
    PagingConfig(
        pageSize = PAGE_SIZE,
        prefetchDistance = PREFETCH_DISTANCE,
        initialLoadSize = INITIAL_LOAD_SIZE,
        enablePlaceholders = false,
    ).also {
        // MediatorPagingHelper computes the persisted pageOrder from these sizes, and
        // TMDB returns a fixed 20 items per page, so the three must stay aligned.
        check(it.initialLoadSize == it.pageSize) {
            "initialLoadSize (${it.initialLoadSize}) must equal pageSize (${it.pageSize}) for pageOrder math"
        }
    }
