package com.benjamin.moviehub.data.paging

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.benjamin.moviehub.BuildConfig
import com.benjamin.moviehub.data.local.MovieDatabase
import com.benjamin.moviehub.data.local.MovieEntity
import com.benjamin.moviehub.data.local.MovieRemoteKey
import com.benjamin.moviehub.data.mapper.toEntity
import com.benjamin.moviehub.data.remote.MovieApiService

@OptIn(ExperimentalPagingApi::class)
class MovieRemoteMediator(
    private val apiService: MovieApiService,
    private val database: MovieDatabase
) : RemoteMediator<Int, MovieEntity>() {

    private val movieDao = database.movieDao()

    private var isFetching = false

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, MovieEntity>
    ): MediatorResult {
        if (isFetching && loadType == LoadType.APPEND) {
            return MediatorResult.Success(endOfPaginationReached = false)
        }

        return try {
            isFetching = true
            val page = when (loadType) {
                LoadType.REFRESH -> 1
                LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
                LoadType.APPEND -> {
                    val remoteKeys = getRemoteKeyForLastItem(state)
                    val nextKey = remoteKeys?.nextKey ?: return MediatorResult.Success(
                        endOfPaginationReached = remoteKeys != null
                    )
                    nextKey
                }

            }

            Log.d("PagingLog", "LoadType: $loadType | Page: $page")

            Log.d("PagingLog", "Appel API Page $page")
            val response = apiService.getPopularMovies(
                BuildConfig.TMDB_API_KEY,
                page = page
            )
            Log.d("PagingLog", "Reçu ${response.movies.size} films")

            val movies = response.movies
            val endOfPaginationReached = movies.isEmpty()

            database.withTransaction {
                if (loadType == LoadType.REFRESH) {
                    movieDao.clearRemotesKeys()
                    movieDao.clearPopularMovies()
                }

                val favoriteIds = movieDao.getFavoriteMovieIds().toSet()

                val prevKey = if (page == 1) null else page - 1
                val nextKey = if (endOfPaginationReached) null else page + 1

                val keys = movies.map {
                    MovieRemoteKey(movieId = it.id, prevKey = prevKey, nextKey = nextKey)
                }

                val movieEntities = movies.mapIndexed { index, dto ->

                    val position = ((page - 1) * state.config.pageSize) + index
                    dto.toEntity(
                        isFavorite = favoriteIds.contains(dto.id),
                        isPopular = true,
                        pageOrder = position
                    )
                }
                movieDao.insertAllKeys(keys)
                movieDao.insertMovies(movieEntities)
            }
            MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
        } finally {
            isFetching = false
        }
    }

    private suspend fun getRemoteKeyForLastItem(state: PagingState<Int, MovieEntity>): MovieRemoteKey? {
        return state.pages.lastOrNull { it.data.isNotEmpty() }
            ?.data?.lastOrNull()
            ?.let { movie ->
                movieDao.getRemoteKeysForMovieId(movie.id)
            }
    }

    override suspend fun initialize(): InitializeAction {
        return if (database.withTransaction { movieDao.getRemoteKeysCount() == 0 }) {
            InitializeAction.LAUNCH_INITIAL_REFRESH
        } else {
            return InitializeAction.SKIP_INITIAL_REFRESH
        }
    }
}
