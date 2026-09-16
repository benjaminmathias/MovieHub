package com.benjamin.moviehub.data.paging

import com.benjamin.moviehub.data.local.MovieDatabase
import com.benjamin.moviehub.data.local.MovieSearchResultEntity
import com.benjamin.moviehub.data.local.SearchQueryKey
import com.benjamin.moviehub.data.remote.MovieApiService
import com.benjamin.moviehub.data.remote.MovieDto
import com.benjamin.moviehub.data.remote.MovieResponse

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
