package com.benjamin.moviehub.data.repository

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.benjamin.moviehub.BuildConfig
import com.benjamin.moviehub.data.local.MovieDao
import com.benjamin.moviehub.data.local.MovieDatabase
import com.benjamin.moviehub.data.mapper.toDomain
import com.benjamin.moviehub.data.mapper.toEntity
import com.benjamin.moviehub.data.paging.MovieRemoteMediator
import com.benjamin.moviehub.data.paging.SearchMovieRemoteMediator
import com.benjamin.moviehub.data.remote.MovieApiService
import com.benjamin.moviehub.domain.model.Actor
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.repository.MovieRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MovieRepositoryImpl @Inject constructor(
    private val apiService: MovieApiService,
    private val database: MovieDatabase,
    private val movieDao: MovieDao
) : MovieRepository {

    @OptIn(ExperimentalPagingApi::class)
    override fun getPagedMovies(query: String?): Flow<PagingData<Movie>> {

        val isSearch = !query.isNullOrBlank()

        return Pager(
            config = PagingConfig(
                pageSize = 20,
                prefetchDistance = 5,
                initialLoadSize = 20,
                enablePlaceholders = false
            ),
            remoteMediator =
                if (isSearch) {
                    SearchMovieRemoteMediator(apiService, database, query)
                } else {
                    MovieRemoteMediator(apiService, database)
                },
            pagingSourceFactory = {
                if (isSearch) {
                    movieDao.searchMoviesPaging()
                } else {
                    movieDao.getPopularMoviesPaging()
                }
            }
        ).flow
            .map { pagingData ->
                pagingData.map { entity -> entity.toDomain() }
            }
    }

    override suspend fun getMovieDetails(movieId: Int): Movie {

        // Try to get cached movie
        val localMovie = movieDao.getMovieById(movieId)

        Log.d("localMovie GenreId", localMovie?.genreIds.toString())

        return try {
            // API call
            val dto = apiService.getMovieDetails(
                movieId = movieId,
                apiKey = BuildConfig.TMDB_API_KEY
            )

            val remoteMovieEntity = dto.toEntity(
                isFavorite = localMovie?.isFavorite ?: false,
                isPopular = localMovie?.isPopular ?: false,
                isSearchResult = localMovie?.isSearchResult ?: false,
                pageOrder = localMovie?.pageOrder ?: -1
            )

            // Save to DB
            movieDao.insertMovie(remoteMovieEntity)

            remoteMovieEntity.toDomain()

        } catch (e: Exception) {
            localMovie?.toDomain() ?: throw e
        }
    }

    override suspend fun toggleFavorite(movie: Movie, isFavorite: Boolean) {

        val localMovie = movieDao.getMovieById(movie.id)

        if (localMovie == null) {
            // Like a movie not in db
            movieDao.insertMovie(
                movie.toEntity(
                    isFavorite = isFavorite,
                    isPopular = false
                )
            )
        } else {
            // Update favorite status of existing movie in db
            movieDao.updateFavoriteStatus(movie.id, isFavorite)
        }
    }

    override fun getFavoriteMovies(): Flow<List<Movie>> {
        return movieDao.getFavoriteMoviesFlow()
            .map { entities ->
                entities.map { it.toDomain() }
            }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun getMovieActors(movieId: Int): Result<List<Actor>> {
        return try {
            val response = apiService.getMovieCredits(movieId, BuildConfig.TMDB_API_KEY)

            val actors = response.cast.take(15).map { dto ->
                Actor(
                    id = dto.id,
                    name = dto.name,
                    character = dto.character,
                    profileUrl = if (dto.profilePath != null) {
                        "https://image.tmdb.org/t/p/w185${dto.profilePath}"
                    } else {
                        ""
                    }
                )
            }
            Result.success(actors)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}