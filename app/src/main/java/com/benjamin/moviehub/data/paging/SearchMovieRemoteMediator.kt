package com.benjamin.moviehub.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.benjamin.moviehub.data.local.MovieDatabase
import com.benjamin.moviehub.data.local.MovieEntity
import com.benjamin.moviehub.data.local.MovieSearchResultEntity
import com.benjamin.moviehub.data.local.SearchQueryKey
import com.benjamin.moviehub.data.mapper.toEntity
import com.benjamin.moviehub.data.remote.MovieApiService
import com.benjamin.moviehub.data.remote.isEndOfPagination

@OptIn(ExperimentalPagingApi::class)
class SearchMovieRemoteMediator(
    private val apiService: MovieApiService,
    private val database: MovieDatabase,
    private val query: String,
) : RemoteMediator<Int, MovieEntity>() {
    private val movieDao = database.movieDao()
    private val queryKey = SearchQueryKey.normalize(query)
    private val remoteKeyType = SearchQueryKey.remoteKeyType(query)

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
                    val nextKey =
                        remoteKeys?.nextKey ?: return MediatorResult.Success(
                            endOfPaginationReached = remoteKeys != null,
                        )
                    nextKey
                }
            }

        return try {
            val response = apiService.searchMovies(query = query, page = page)

            val movies = response.movies
            val endOfPaginationReached = response.isEndOfPagination(page, state.config.pageSize)

            database.withTransaction {
                val movieIds = movies.map { it.id }
                val localMovies = MediatorPagingHelper.preservedByIds(movieDao, movieIds)

                if (loadType == LoadType.REFRESH) {
                    movieDao.clearSearchResults(queryKey)
                    movieDao.clearRemoteKeysByType(remoteKeyType)
                    movieDao.clearOrphanSearchMovies(movieIds)
                }

                val keys =
                    MediatorPagingHelper.remoteKeys(
                        movieIds,
                        page,
                        endOfPaginationReached,
                        remoteKeyType,
                    )

                val movieEntities =
                    movies.map { dto ->
                        val localMovie = localMovies[dto.id]

                        dto.toEntity(
                            isFavorite = localMovie?.isFavorite ?: false,
                            isSearchResult = true,
                            runtimeMinutesOverride = localMovie?.runtimeMinutes,
                        )
                    }
                val searchResults =
                    movies.mapIndexed { index, dto ->
                        MovieSearchResultEntity(
                            queryKey = queryKey,
                            movieId = dto.id,
                            pageOrder = MediatorPagingHelper.pageOrder(page, state.config.pageSize, index),
                        )
                    }
                movieDao.insertAllKeys(keys)
                movieDao.insertSearchResults(searchResults)
                movieDao.upsertMovies(movieEntities)
            }
            MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            MediatorResult.Error(e)
        }
    }

    override suspend fun initialize(): InitializeAction = InitializeAction.LAUNCH_INITIAL_REFRESH
}
