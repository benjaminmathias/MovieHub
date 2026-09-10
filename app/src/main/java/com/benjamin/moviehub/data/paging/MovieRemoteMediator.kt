package com.benjamin.moviehub.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.benjamin.moviehub.data.local.MovieCategoryEntity
import com.benjamin.moviehub.data.local.MovieDatabase
import com.benjamin.moviehub.data.local.MovieEntity
import com.benjamin.moviehub.data.mapper.toEntity
import com.benjamin.moviehub.data.remote.MovieApiService
import com.benjamin.moviehub.data.remote.MovieResponse
import com.benjamin.moviehub.data.remote.isEndOfPagination
import com.benjamin.moviehub.domain.model.MovieCategory

@OptIn(ExperimentalPagingApi::class)
class MovieRemoteMediator(
    private val apiService: MovieApiService,
    private val database: MovieDatabase,
    private val category: MovieCategory,
) : RemoteMediator<Int, MovieEntity>() {
    private val movieDao = database.movieDao()
    private val remoteKeyType = category.key

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, MovieEntity>,
    ): MediatorResult {
        val page =
            when (loadType) {
                LoadType.REFRESH -> 1
                LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
                LoadType.APPEND -> {
                    val remoteKeys = MediatorPagingHelper.remoteKeyForLastItem(state, movieDao, remoteKeyType)
                    remoteKeys?.nextKey ?: return MediatorResult.Success(
                        endOfPaginationReached = remoteKeys != null,
                    )
                }
            }

        return persistPage(
            page = page,
            pageSize = state.config.pageSize,
            clearCategory = loadType == LoadType.REFRESH,
        )
    }

    /**
     * Loads the first page while [initialize] runs, before Paging creates the local
     * [androidx.paging.PagingSource].
     *
     * A freshly switched category has no cached rows yet, so the Room
     * [androidx.room.paging.LimitOffsetPagingSource] does its first (empty) query while the remote
     * REFRESH writes the rows. Room can drop the invalidation emitted inside that first-query window
     * (`refreshComplete`), which would leave the new category stuck on an empty list. Preloading the
     * first page first guarantees the initial local query already sees the rows.
     */
    override suspend fun initialize(): InitializeAction {
        val hasCache = database.withTransaction { movieDao.getRemoteKeysCountByType(remoteKeyType) > 0 }
        if (hasCache) return InitializeAction.SKIP_INITIAL_REFRESH

        val result = persistPage(page = 1, pageSize = INITIAL_LOAD_SIZE, clearCategory = true)
        return if (result is MediatorResult.Error) {
            InitializeAction.LAUNCH_INITIAL_REFRESH
        } else {
            InitializeAction.SKIP_INITIAL_REFRESH
        }
    }

    private suspend fun persistPage(
        page: Int,
        pageSize: Int,
        clearCategory: Boolean,
    ): MediatorResult {
        return try {
            val response = fetchPage(page)

            val movies = response.movies
            val endOfPaginationReached = response.isEndOfPagination(page, pageSize)

            database.withTransaction {
                if (clearCategory) {
                    movieDao.clearRemoteKeysByType(remoteKeyType)
                    movieDao.clearCategoryMovies(remoteKeyType)
                }

                val movieIds = movies.map { it.id }
                val keys =
                    MediatorPagingHelper.remoteKeys(
                        movieIds,
                        page,
                        endOfPaginationReached,
                        remoteKeyType,
                    )

                val localMovies = MediatorPagingHelper.preservedByIds(movieDao, movieIds)

                val movieEntities =
                    movies.mapIndexed { index, dto ->
                        val localMovie = localMovies[dto.id]

                        dto.toEntity(
                            isFavorite = localMovie?.isFavorite ?: false,
                            isSearchResult = localMovie?.isSearchResult ?: false,
                            runtimeMinutesOverride = localMovie?.runtimeMinutes,
                        )
                    }
                val categoryEntries =
                    movies.mapIndexed { index, dto ->
                        MovieCategoryEntity(
                            movieId = dto.id,
                            category = remoteKeyType,
                            pageOrder = MediatorPagingHelper.pageOrder(page, pageSize, index),
                        )
                    }
                movieDao.insertAllKeys(keys)
                movieDao.insertCategoryMovies(categoryEntries)
                movieDao.upsertMovies(movieEntities)
            }
            MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            MediatorResult.Error(e)
        }
    }

    private suspend fun fetchPage(page: Int): MovieResponse =
        when (category) {
            MovieCategory.POPULAR -> apiService.getPopularMovies(page = page)
            MovieCategory.NOW_PLAYING -> apiService.getNowPlayingMovies(page = page)
            MovieCategory.UPCOMING -> apiService.getUpcomingMovies(page = page)
            MovieCategory.TOP_RATED -> apiService.getTopRatedMovies(page = page)
        }
}
