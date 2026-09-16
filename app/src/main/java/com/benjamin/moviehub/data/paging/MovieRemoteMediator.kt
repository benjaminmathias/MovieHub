package com.benjamin.moviehub.data.paging

import com.benjamin.moviehub.data.local.MovieCategoryEntity
import com.benjamin.moviehub.data.local.MovieDatabase
import com.benjamin.moviehub.data.remote.MovieApiService
import com.benjamin.moviehub.data.remote.MovieDto
import com.benjamin.moviehub.data.remote.MovieResponse
import com.benjamin.moviehub.domain.model.MovieCategory

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
