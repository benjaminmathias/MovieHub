package com.benjamin.moviehub.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.benjamin.moviehub.data.local.MovieDatabase
import com.benjamin.moviehub.data.local.MovieEntity
import com.benjamin.moviehub.data.mapper.toEntity
import com.benjamin.moviehub.data.remote.MovieApiService
import com.benjamin.moviehub.data.remote.isEndOfPagination

@OptIn(ExperimentalPagingApi::class)
class MovieRemoteMediator(
    private val apiService: MovieApiService,
    private val database: MovieDatabase,
) : RemoteMediator<Int, MovieEntity>() {
    private val movieDao = database.movieDao()

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, MovieEntity>,
    ): MediatorResult {
        val page =
            when (loadType) {
                LoadType.REFRESH -> 1
                LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
                LoadType.APPEND -> {
                    val remoteKeys = MediatorPagingHelper.remoteKeyForLastItem(state, movieDao, POPULAR_REMOTE_KEY_TYPE)
                    val nextKey =
                        remoteKeys?.nextKey ?: return MediatorResult.Success(
                            endOfPaginationReached = remoteKeys != null,
                        )
                    nextKey
                }
            }

        return try {
            val response = apiService.getPopularMovies(page = page)

            val movies = response.movies
            val endOfPaginationReached = response.isEndOfPagination(page, state.config.pageSize)

            database.withTransaction {
                if (loadType == LoadType.REFRESH) {
                    movieDao.clearRemoteKeysByType(POPULAR_REMOTE_KEY_TYPE)
                    movieDao.clearPopularMovies()
                }

                val movieIds = movies.map { it.id }
                val keys =
                    MediatorPagingHelper.remoteKeys(
                        movieIds,
                        page,
                        endOfPaginationReached,
                        POPULAR_REMOTE_KEY_TYPE,
                    )

                val localMovies = MediatorPagingHelper.preservedByIds(movieDao, movieIds)

                val movieEntities =
                    movies.mapIndexed { index, dto ->
                        val localMovie = localMovies[dto.id]

                        dto.toEntity(
                            isFavorite = localMovie?.isFavorite ?: false,
                            isPopular = true,
                            isSearchResult = localMovie?.isSearchResult ?: false,
                            pageOrder = MediatorPagingHelper.pageOrder(page, state.config.pageSize, index),
                            runtimeMinutesOverride = localMovie?.runtimeMinutes,
                        )
                    }
                movieDao.insertAllKeys(keys)
                movieDao.upsertMovies(movieEntities)
            }
            MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            MediatorResult.Error(e)
        }
    }

    override suspend fun initialize(): InitializeAction =
        if (database.withTransaction { movieDao.getRemoteKeysCountByType(POPULAR_REMOTE_KEY_TYPE) == 0 }) {
            InitializeAction.LAUNCH_INITIAL_REFRESH
        } else {
            InitializeAction.SKIP_INITIAL_REFRESH
        }
}
