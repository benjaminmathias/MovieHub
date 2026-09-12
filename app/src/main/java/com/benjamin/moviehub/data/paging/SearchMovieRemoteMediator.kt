package com.benjamin.moviehub.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.benjamin.moviehub.data.local.MovieDatabase
import com.benjamin.moviehub.data.local.MovieEntity
import com.benjamin.moviehub.data.local.MovieSearchResultEntity
import com.benjamin.moviehub.data.local.RemoteKey
import com.benjamin.moviehub.data.local.SearchQueryKey
import com.benjamin.moviehub.data.mapper.toEntity
import com.benjamin.moviehub.data.remote.MovieApiService
import com.benjamin.moviehub.data.remote.isEndOfPagination
import kotlinx.coroutines.CancellationException

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
                LoadType.APPEND ->
                    movieDao.getRemoteKey(remoteKeyType)?.nextKey
                        ?: return MediatorResult.Success(endOfPaginationReached = true)
            }

        return try {
            val response = apiService.searchMovies(query = query, page = page)

            val movies = response.movies
            val endOfPaginationReached = response.isEndOfPagination(page, state.config.pageSize)

            database.withTransaction {
                if (loadType == LoadType.REFRESH) {
                    val previousResultIds = movieDao.getAllSearchResultMovieIds()
                    movieDao.clearSearchResults()
                    movieDao.clearSearchRemoteKeys()
                    movieDao.deleteSearchOrphans(previousResultIds, movies.map { it.id })
                }

                val movieEntities = movies.map { it.toEntity() }
                val searchResults =
                    movies.mapIndexed { index, dto ->
                        MovieSearchResultEntity(
                            queryKey = queryKey,
                            movieId = dto.id,
                            pageOrder = MediatorPagingHelper.pageOrder(page, state.config.pageSize, index),
                        )
                    }
                movieDao.upsertRemoteKey(
                    RemoteKey(
                        type = remoteKeyType,
                        nextKey = page.takeIf { !endOfPaginationReached }?.plus(1),
                    ),
                )
                movieDao.insertSearchResults(searchResults)
                movieDao.upsertMovies(movieEntities)
            }
            MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            MediatorResult.Error(e)
        }
    }

    override suspend fun initialize(): InitializeAction = InitializeAction.LAUNCH_INITIAL_REFRESH
}
