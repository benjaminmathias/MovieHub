package com.benjamin.moviehub.data.paging

import com.benjamin.moviehub.data.local.MovieDao
import com.benjamin.moviehub.data.local.MovieEntity

/**
 * Shared bits for [MovieRemoteMediator] and [SearchMovieRemoteMediator]. The two
 * mediators keep their own queries and REFRESH cleanup, but position and local
 * field preservation are identical.
 */
internal object MediatorPagingHelper {
    fun pageOrder(
        page: Int,
        pageSize: Int,
        index: Int,
    ): Int = ((page - 1) * pageSize) + index

    suspend fun preservedByIds(
        movieDao: MovieDao,
        ids: List<Int>,
    ): Map<Int, MovieEntity> =
        if (ids.isEmpty()) {
            emptyMap()
        } else {
            movieDao.getMoviesByIds(ids).associateBy { it.id }
        }
}
