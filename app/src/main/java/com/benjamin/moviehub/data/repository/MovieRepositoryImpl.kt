package com.benjamin.moviehub.data.repository

import android.util.Log
import com.benjamin.moviehub.BuildConfig
import com.benjamin.moviehub.data.local.MovieDao
import com.benjamin.moviehub.data.mapper.toDomain
import com.benjamin.moviehub.data.mapper.toEntity
import com.benjamin.moviehub.data.remote.MovieApiService
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.repository.MovieRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

class MovieRepositoryImpl @Inject constructor(
    private val apiService: MovieApiService,
    private val movieDao: MovieDao
) : MovieRepository {

    override fun getPopularMovies(): Flow<List<Movie>> = flow {
        coroutineScope {
            launch {
                try {
                    val response = apiService.getPopularMovies(apiKey = BuildConfig.TMDB_API_KEY)
                    val favoriteIds = movieDao.getFavoriteMovieIds().toSet()
                    val entities = response.movies.map { dto ->
                        dto.toEntity(
                            isFavorite = favoriteIds.contains(dto.id),
                            isPopular = true
                        )
                    }
                    movieDao.insertMovies(entities)
                } catch (e: Exception) {
                    Log.e("MovieRepository", "API Error", e)
                }
            }

            emitAll(movieDao.getPopularMovies().map { entities ->
                entities.map { it.toDomain() }
            })
        }
    }.flowOn(Dispatchers.IO)

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

    override suspend fun getSearchedMovies(query: String): List<Movie> {
        val response = apiService.searchMovies(
            apiKey = BuildConfig.TMDB_API_KEY,
            query = query
        )

        val favoriteIds = movieDao.getFavoriteMovieIds().toSet()

        return response.movies.map { dto ->
            dto.toDomain().copy(
                isFavorite = favoriteIds.contains(dto.id)
            )
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

    override suspend fun getFavoriteMovies(): Flow<List<Movie>> {
        return movieDao.getFavoriteMovies()
            .map { entities ->
                entities.map { it.toDomain() }
            }
            .flowOn(Dispatchers.IO)
    }

}