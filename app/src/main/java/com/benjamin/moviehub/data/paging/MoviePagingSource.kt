package com.benjamin.moviehub.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.benjamin.moviehub.BuildConfig
import com.benjamin.moviehub.data.remote.MovieApiService
import com.benjamin.moviehub.data.remote.MovieDto

class MoviePagingSource(
    private val api: MovieApiService,
    private val query: String? = null,
) : PagingSource<Int, MovieDto>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MovieDto> {
        val position = params.key ?: 1
        return try {
            val response =
                if (query.isNullOrBlank()) {
                    api.getPopularMovies(page = position, apiKey = BuildConfig.TMDB_API_KEY)
                } else {
                    api.searchMovies(query = query, page = position, apiKey = BuildConfig.TMDB_API_KEY)
                }
            LoadResult.Page(
                data = response.movies,
                prevKey = if (position == 1) null else position - 1,
                nextKey = if (response.movies.isEmpty()) null else position + 1,
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, MovieDto>): Int? =
        state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
}
