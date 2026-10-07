package com.benjamin.moviehub.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.benjamin.moviehub.data.mapper.toDomain
import com.benjamin.moviehub.data.remote.MovieApiService
import com.benjamin.moviehub.data.remote.isEndOfPagination
import com.benjamin.moviehub.domain.model.DiscoverFilters
import com.benjamin.moviehub.domain.model.DiscoverSortOption
import com.benjamin.moviehub.domain.model.Movie
import kotlinx.coroutines.CancellationException

private const val RATING_MINIMUM_VOTE_COUNT = 200

class DiscoverMoviePagingSource(
    private val apiService: MovieApiService,
    private val filters: DiscoverFilters,
) : PagingSource<Int, Movie>() {
    /**
     * TMDB can return the same movie on two consecutive pages while the discover
     * result set shifts between requests. Duplicate ids would collide with the
     * grid's `key = { it.id }` and crash the LazyVerticalGrid, so drop repeats
     * within this pagination generation. A new PagingSource instance (refresh or
     * filter change) starts with a clean set. Memory stays bounded by TMDB's
     * discover result cap (a few thousand ids at most).
     */
    private val seenIds = mutableSetOf<Int>()

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Movie> {
        val page = params.key ?: 1

        return try {
            // The selected decade is inclusive: 1990 covers 1990-01-01 through 1999-12-31.
            val decadeStart = filters.releaseDecade
            val response =
                apiService.discoverMovies(
                    genreId = filters.genreId,
                    primaryReleaseDateGte = decadeStart?.let { "$it-01-01" },
                    primaryReleaseDateLte = decadeStart?.let { "${it + 9}-12-31" },
                    minimumVoteAverage = filters.minimumVoteAverage,
                    minimumVoteCount =
                        RATING_MINIMUM_VOTE_COUNT.takeIf { filters.sort == DiscoverSortOption.RATING },
                    sortBy = filters.sort.queryValue,
                    page = page,
                )
            LoadResult.Page(
                data = response.movies.map { it.toDomain() }.filter { seenIds.add(it.id) },
                prevKey = page.takeIf { it > 1 }?.minus(1),
                nextKey = page.takeIf { !response.isEndOfPagination(page, params.loadSize) }?.plus(1),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Movie>): Int? =
        state.anchorPosition?.let { position ->
            state.closestPageToPosition(position)?.let { page ->
                page.prevKey?.plus(1) ?: page.nextKey?.minus(1)
            }
        }
}
