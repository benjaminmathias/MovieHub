package com.benjamin.moviehub.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.benjamin.moviehub.BuildConfig
import com.benjamin.moviehub.data.local.MovieDao
import com.benjamin.moviehub.data.mapper.toDomain
import com.benjamin.moviehub.data.mapper.toEntity
import com.benjamin.moviehub.data.paging.MoviePagingSource
import com.benjamin.moviehub.data.remote.MovieApiService
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.repository.MovieRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MovieRepositoryImpl @Inject constructor(
    private val apiService: MovieApiService,
    private val movieDao: MovieDao
) : MovieRepository {

    override fun getPagedMovies(query: String?): Flow<PagingData<Movie>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                prefetchDistance = 5,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { MoviePagingSource(apiService, query) }
        ).flow
            .map { pagingData ->
                pagingData.map { dto -> dto.toDomain() }
            }
    }

    override suspend fun getMovieDetails(movieId: Int): Movie {

        val localMovie = movieDao.getMovieById(movieId)
        if (localMovie != null) {
            return localMovie.toDomain()
        } else {
            val dto = apiService.getMovieDetails(
                movieId = movieId,
                apiKey = BuildConfig.TMDB_API_KEY,
            )
            return dto.toDomain()
        }
    }

    override suspend fun toggleFavorite(movie: Movie, isFavorite: Boolean) {

        val localMovie = movieDao.getMovieById(movie.id)

        if (localMovie == null) {
            movieDao.insertMovies(
                listOf(
                    movie.toEntity(
                        isFavorite = isFavorite,
                        isPopular = false
                    )
                )
            )
        } else {
            movieDao.updateFavoriteStatus(movie.id, isFavorite)
        }
        movieDao.updateFavoriteStatus(movie.id, isFavorite)
    }

    override fun getFavoriteMovies(): Flow<List<Movie>> {
        return movieDao.getFavoriteMovies()
            .map { entities ->
                entities.map { it.toDomain() }
            }
            .flowOn(Dispatchers.IO)
    }
}