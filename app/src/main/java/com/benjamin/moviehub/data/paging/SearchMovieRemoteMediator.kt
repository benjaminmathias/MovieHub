package com.benjamin.moviehub.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.benjamin.moviehub.BuildConfig
import com.benjamin.moviehub.data.local.MovieDatabase
import com.benjamin.moviehub.data.local.MovieEntity
import com.benjamin.moviehub.data.local.MovieRemoteKey
import com.benjamin.moviehub.data.local.MovieSearchResultEntity
import com.benjamin.moviehub.data.local.SearchQueryKey
import com.benjamin.moviehub.data.mapper.toEntity
import com.benjamin.moviehub.data.remote.MovieApiService

@OptIn(ExperimentalPagingApi::class)
class SearchMovieRemoteMediator(
    private val apiService: MovieApiService,
    private val database: MovieDatabase,
    private val query: String,
) : RemoteMediator<Int, MovieEntity>() {
    private val movieDao = database.movieDao()
    private val queryKey = SearchQueryKey.normalize(query)
    private val remoteKeyType = SearchQueryKey.remoteKeyType(query)

    private var isFetching = false

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, MovieEntity>,
    ): MediatorResult {
        val page =
            when (loadType) {
                LoadType.REFRESH -> 1
                LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
                LoadType.APPEND -> {
                    val remoteKeys = getRemoteKeyForLastItem(state)
                    val nextKey =
                        remoteKeys?.nextKey ?: return MediatorResult.Success(
                            endOfPaginationReached = remoteKeys != null,
                        )
                    nextKey
                }
            }

        if (isFetching && loadType == LoadType.APPEND) {
            return MediatorResult.Success(endOfPaginationReached = false)
        }

        return try {
            isFetching = true
            val response =
                apiService.searchMovies(
                    BuildConfig.TMDB_API_KEY,
                    page = page,
                    query = query,
                )

            val movies = response.movies
            val endOfPaginationReached = movies.isEmpty() || movies.size < state.config.pageSize

            database.withTransaction {
                if (loadType == LoadType.REFRESH) {
                    movieDao.clearRemoteKeysByType(remoteKeyType)
                    movieDao.clearSearchResults(queryKey)
                }

                val prevKey = if (page == 1) null else page - 1
                val nextKey = if (endOfPaginationReached) null else page + 1

                val keys =
                    movies.map {
                        MovieRemoteKey(
                            movieId = it.id,
                            prevKey = prevKey,
                            nextKey = nextKey,
                            type = remoteKeyType,
                        )
                    }

                val movieEntities =
                    movies.mapIndexed { index, dto ->
                        val position = ((page - 1) * state.config.pageSize) + index

                        val localMovie = movieDao.getMovieById(dto.id)

                        dto.toEntity(
                            isFavorite = localMovie?.isFavorite ?: false,
                            isPopular = localMovie?.isPopular ?: false,
                            isSearchResult = true,
                            pageOrder = position,
                        )
                    }
                val searchResults =
                    movies.mapIndexed { index, dto ->
                        MovieSearchResultEntity(
                            queryKey = queryKey,
                            movieId = dto.id,
                            pageOrder = ((page - 1) * state.config.pageSize) + index,
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
        } finally {
            isFetching = false
        }
    }

    private suspend fun getRemoteKeyForLastItem(state: PagingState<Int, MovieEntity>): MovieRemoteKey? =
        state.pages
            .lastOrNull { it.data.isNotEmpty() }
            ?.data
            ?.lastOrNull()
            ?.let { movie ->
                movieDao.getRemoteKeysForMovieId(movie.id, remoteKeyType)
            }

    override suspend fun initialize(): InitializeAction = InitializeAction.LAUNCH_INITIAL_REFRESH
}
