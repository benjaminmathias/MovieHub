package com.benjamin.moviehub.data.repository

import android.util.Log
import com.benjamin.moviehub.BuildConfig
import com.benjamin.moviehub.data.local.MovieDao
import com.benjamin.moviehub.data.remote.MovieApiService
import com.benjamin.moviehub.data.mapper.toDomain
import com.benjamin.moviehub.data.mapper.toEntity
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.repository.MovieRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class MovieRepositoryImpl @Inject constructor(
    private val apiService: MovieApiService,
    private val movieDao: MovieDao
) : MovieRepository {

    override fun getPopularMovies(): Flow<List<Movie>> = flow {

        val localMovies = movieDao.getAllMovies()
        emit(localMovies.map { it.toDomain() })

        try {
            val response = apiService.getPopularMovies(apiKey = BuildConfig.TMDB_API_KEY)
            val favoriteIds = movieDao.getFavoriteMovieIds().toSet()
            val entities = response.movies.map {
                it.toEntity(isFavorite = favoriteIds.contains(it.id))
            }

            movieDao.insertMovies(entities)

            emit(entities.map { it.toDomain() })
        } catch (e: Exception) {
            Log.e("MovieRepository", "Erreur lors de la récupération des films", e)
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
        val query = apiService.searchMovies(
            apiKey = BuildConfig.TMDB_API_KEY,
            query = query
        )
        return query.movies.map { it.toDomain() }
    }

    override suspend fun toggleFavorite(movieId: Int, isFavorite: Boolean) {
        movieDao.updateFavoriteStatus(movieId, isFavorite)
    }
}