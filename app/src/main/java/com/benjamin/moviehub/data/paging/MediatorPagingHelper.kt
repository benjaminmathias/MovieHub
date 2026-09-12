package com.benjamin.moviehub.data.paging

/**
 * Shared bits for [MovieRemoteMediator] and [SearchMovieRemoteMediator]. The two
 * mediators keep their own queries and REFRESH cleanup, but position handling is shared.
 */
internal object MediatorPagingHelper {
    fun pageOrder(
        page: Int,
        pageSize: Int,
        index: Int,
    ): Int = ((page - 1) * pageSize) + index
}
