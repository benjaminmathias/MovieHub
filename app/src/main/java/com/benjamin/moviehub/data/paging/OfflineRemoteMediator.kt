package com.benjamin.moviehub.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.benjamin.moviehub.data.local.MovieDao
import com.benjamin.moviehub.data.local.MovieDatabase
import com.benjamin.moviehub.data.local.MovieEntity
import com.benjamin.moviehub.data.local.RemoteKey
import com.benjamin.moviehub.data.mapper.toEntity
import com.benjamin.moviehub.data.remote.MovieDto
import com.benjamin.moviehub.data.remote.MovieResponse
import com.benjamin.moviehub.data.remote.isEndOfPagination
import kotlinx.coroutines.CancellationException

/**
 * Room-backed [RemoteMediator] shared by the offline Home and Search feeds.
 *
 * Owns page resolution, the write transaction, remote-key bookkeeping and error
 * mapping. Subclasses only describe how to fetch a page, what a refresh must clear,
 * and how to place a page inside its feed.
 */
@OptIn(ExperimentalPagingApi::class)
internal abstract class OfflineRemoteMediator(
    private val database: MovieDatabase,
) : RemoteMediator<Int, MovieEntity>() {
    protected val movieDao: MovieDao = database.movieDao()

    /** Cursor row such as a category key or a normalized search query. */
    protected abstract val remoteKeyType: String

    protected abstract suspend fun fetchPage(page: Int): MovieResponse

    /** Feed-specific cleanup run inside the REFRESH transaction, after the page fetch. */
    protected abstract suspend fun clearFeed(fetched: List<MovieDto>)

    /** Persists the association rows placing [movies] at [offset] in the feed. */
    protected abstract suspend fun persistAssociations(
        movies: List<MovieDto>,
        offset: Int,
    )

    final override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, MovieEntity>,
    ): MediatorResult {
        val page =
            when (loadType) {
                LoadType.REFRESH -> FIRST_PAGE
                LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
                LoadType.APPEND ->
                    movieDao.getRemoteKey(remoteKeyType)?.nextKey
                        ?: return MediatorResult.Success(endOfPaginationReached = true)
            }

        return try {
            val response = fetchPage(page)
            val movies = response.movies
            val endReached = response.isEndOfPagination(page, state.config.pageSize)

            database.withTransaction {
                if (loadType == LoadType.REFRESH) clearFeed(movies)
                movieDao.upsertRemoteKey(
                    RemoteKey(
                        type = remoteKeyType,
                        nextKey = page.takeIf { !endReached }?.plus(1),
                    ),
                )
                persistAssociations(movies, offset = (page - FIRST_PAGE) * state.config.pageSize)
                movieDao.upsertMovies(movies.map { it.toEntity() })
            }
            MediatorResult.Success(endOfPaginationReached = endReached)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            MediatorResult.Error(e)
        }
    }

    /**
     * Always launches the initial refresh so dynamic feeds do not stay stale across
     * launches. Paging keeps showing the cached rows while the refresh runs, and a
     * failed refresh returns [MediatorResult.Error] before clearing anything.
     */
    final override suspend fun initialize(): InitializeAction = InitializeAction.LAUNCH_INITIAL_REFRESH

    private companion object {
        const val FIRST_PAGE = 1
    }
}
