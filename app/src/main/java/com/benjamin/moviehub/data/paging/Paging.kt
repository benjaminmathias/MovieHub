package com.benjamin.moviehub.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.benjamin.moviehub.data.local.MovieCategoryEntity
import com.benjamin.moviehub.data.local.MovieDao
import com.benjamin.moviehub.data.local.MovieDatabase
import com.benjamin.moviehub.data.local.MovieEntity
import com.benjamin.moviehub.data.local.MovieSearchResultEntity
import com.benjamin.moviehub.data.local.RemoteKey
import com.benjamin.moviehub.data.local.SearchQueryKey
import com.benjamin.moviehub.data.mapper.toEntity
import com.benjamin.moviehub.data.remote.MovieApiService
import com.benjamin.moviehub.data.remote.MovieDto
import com.benjamin.moviehub.data.remote.MovieResponse
import com.benjamin.moviehub.data.remote.isEndOfPagination
import com.benjamin.moviehub.domain.model.MovieCategory
import kotlinx.coroutines.CancellationException

internal const val PAGE_SIZE = 20
internal const val PREFETCH_DISTANCE = 5
internal const val INITIAL_LOAD_SIZE = 20

internal val moviePagingConfig =
    PagingConfig(
        pageSize = PAGE_SIZE,
        prefetchDistance = PREFETCH_DISTANCE,
        initialLoadSize = INITIAL_LOAD_SIZE,
        enablePlaceholders = false,
    ).also {
        // OfflineRemoteMediator persists pageOrder from these sizes, and TMDB returns a
        // fixed 20 items per page, so the three must stay aligned.
        check(it.initialLoadSize == it.pageSize) {
            "initialLoadSize (${it.initialLoadSize}) must equal pageSize (${it.pageSize}) for pageOrder math"
        }
    }

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

/** Offline mediator for one Home [category] feed. */
internal class MovieRemoteMediator(
    private val apiService: MovieApiService,
    database: MovieDatabase,
    private val category: MovieCategory,
) : OfflineRemoteMediator(database) {
    override val remoteKeyType: String = category.key

    override suspend fun fetchPage(page: Int): MovieResponse = apiService.categoryMovies(category, page)

    override suspend fun clearFeed(fetched: List<MovieDto>) {
        movieDao.clearRemoteKeysByType(remoteKeyType)
        movieDao.clearCategoryMovies(remoteKeyType)
    }

    override suspend fun persistAssociations(
        movies: List<MovieDto>,
        offset: Int,
    ) {
        movieDao.insertCategoryMovies(
            movies.mapIndexed { index, dto ->
                MovieCategoryEntity(
                    movieId = dto.id,
                    category = remoteKeyType,
                    pageOrder = offset + index,
                )
            },
        )
    }
}

/** Offline mediator for one normalized search [query]. */
internal class SearchMovieRemoteMediator(
    private val apiService: MovieApiService,
    database: MovieDatabase,
    private val query: String,
) : OfflineRemoteMediator(database) {
    private val queryKey = SearchQueryKey.normalize(query)

    override val remoteKeyType: String = SearchQueryKey.remoteKeyType(query)

    override suspend fun fetchPage(page: Int): MovieResponse = apiService.searchMovies(query = query, page = page)

    override suspend fun clearFeed(fetched: List<MovieDto>) {
        val previousResultIds = movieDao.getAllSearchResultMovieIds()
        movieDao.clearSearchResults()
        movieDao.clearSearchRemoteKeys()
        movieDao.deleteSearchOrphans(previousResultIds, fetched.map { it.id })
    }

    override suspend fun persistAssociations(
        movies: List<MovieDto>,
        offset: Int,
    ) {
        movieDao.insertSearchResults(
            movies.mapIndexed { index, dto ->
                MovieSearchResultEntity(
                    queryKey = queryKey,
                    movieId = dto.id,
                    pageOrder = offset + index,
                )
            },
        )
    }
}

private suspend fun MovieApiService.categoryMovies(
    category: MovieCategory,
    page: Int,
): MovieResponse =
    when (category) {
        MovieCategory.POPULAR -> getPopularMovies(page)
        MovieCategory.NOW_PLAYING -> getNowPlayingMovies(page)
        MovieCategory.UPCOMING -> getUpcomingMovies(page)
        MovieCategory.TOP_RATED -> getTopRatedMovies(page)
    }
