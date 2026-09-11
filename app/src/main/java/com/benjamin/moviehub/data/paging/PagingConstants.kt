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
    )
