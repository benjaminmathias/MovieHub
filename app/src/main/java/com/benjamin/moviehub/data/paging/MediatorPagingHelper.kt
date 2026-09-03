package com.benjamin.moviehub.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingState
import com.benjamin.moviehub.data.local.MovieDao
import com.benjamin.moviehub.data.local.MovieEntity
import com.benjamin.moviehub.data.local.MovieRemoteKey

/**
 * Logique partagée par [MovieRemoteMediator] et [SearchMovieRemoteMediator].
 *
 * Les deux mediators gardent leurs classes séparées (requêtes et nettoyage
 * au REFRESH différents), mais le calcul des clés, des positions et la
 * préservation des champs locaux (favoris, runtime) sont identiques.
 */
@OptIn(ExperimentalPagingApi::class)
internal object MediatorPagingHelper {
    fun prevKey(page: Int): Int? = if (page == 1) null else page - 1

    fun nextKey(
        page: Int,
        endOfPaginationReached: Boolean,
    ): Int? = if (endOfPaginationReached) null else page + 1

    fun remoteKeys(
        movieIds: List<Int>,
        page: Int,
        endOfPaginationReached: Boolean,
        type: String,
    ): List<MovieRemoteKey> =
        movieIds.map { id ->
            MovieRemoteKey(
                movieId = id,
                prevKey = prevKey(page),
                nextKey = nextKey(page, endOfPaginationReached),
                type = type,
            )
        }

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

    suspend fun remoteKeyForLastItem(
        state: PagingState<Int, MovieEntity>,
        movieDao: MovieDao,
        type: String,
    ): MovieRemoteKey? =
        state.pages
            .lastOrNull { it.data.isNotEmpty() }
            ?.data
            ?.lastOrNull()
            ?.let { movie ->
                movieDao.getRemoteKeysForMovieId(movie.id, type)
            }
}
