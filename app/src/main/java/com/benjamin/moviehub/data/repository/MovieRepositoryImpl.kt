package com.benjamin.moviehub.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingData
import androidx.paging.map
import com.benjamin.moviehub.data.local.MovieDao
import com.benjamin.moviehub.data.local.MovieDatabase
import com.benjamin.moviehub.data.local.SearchQueryKey
import com.benjamin.moviehub.data.mapper.toDomain
import com.benjamin.moviehub.data.mapper.toEntity
import com.benjamin.moviehub.data.paging.DiscoverMoviePagingSource
import com.benjamin.moviehub.data.paging.MovieRemoteMediator
import com.benjamin.moviehub.data.paging.SearchMovieRemoteMediator
import com.benjamin.moviehub.data.paging.moviePagingConfig
import com.benjamin.moviehub.data.remote.MovieApiService
import com.benjamin.moviehub.domain.model.DiscoverFilters
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.model.MovieCategory
import com.benjamin.moviehub.domain.model.MovieGenre
import com.benjamin.moviehub.domain.repository.MovieRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.IOException
import retrofit2.HttpException
import javax.inject.Inject

class MovieRepositoryImpl
    @Inject
    constructor(
        private val apiService: MovieApiService,
        private val database: MovieDatabase,
        private val movieDao: MovieDao,
    ) : MovieRepository {
        @OptIn(ExperimentalPagingApi::class)
        override fun getCategoryMovies(category: MovieCategory): Flow<PagingData<Movie>> =
            Pager(
                config = moviePagingConfig,
                remoteMediator = MovieRemoteMediator(apiService, database, category),
                pagingSourceFactory = { movieDao.getCategoryMoviesPaging(category.key) },
            ).flow.map { pagingData -> pagingData.map { entity -> entity.toDomain() } }

        @OptIn(ExperimentalPagingApi::class)
        override fun searchMovies(query: String): Flow<PagingData<Movie>> {
            val queryKey = SearchQueryKey.normalize(query)

            return Pager(
                config = moviePagingConfig,
                remoteMediator = SearchMovieRemoteMediator(apiService, database, query),
                pagingSourceFactory = { movieDao.searchMoviesPaging(queryKey) },
            ).flow.map { pagingData -> pagingData.map { entity -> entity.toDomain() } }
        }

        override fun getDiscoverMovies(filters: DiscoverFilters): Flow<PagingData<Movie>> =
            Pager(
                config = moviePagingConfig,
                pagingSourceFactory = { DiscoverMoviePagingSource(apiService, filters) },
            ).flow

        override suspend fun getMovieGenres(): List<MovieGenre> =
            apiService
                .getMovieGenres()
                .genres
                .mapNotNull { genre ->
                    genre.name?.trim()?.takeIf(String::isNotEmpty)?.let { name ->
                        MovieGenre(id = genre.id, name = name)
                    }
                }

        override fun getHeroMovie(category: MovieCategory): Flow<Movie?> =
            movieDao
                .getHeroMovieFlow(category.key)
                .map { entity -> entity?.toDomain() }

        override suspend fun getMovieDetails(movieId: Int): Movie {
            return try {
                // API call
                val dto = apiService.getMovieDetails(movieId = movieId)

                val remoteMovieEntity = dto.toEntity()
                val savedMovie = movieDao.upsertMovieDetails(remoteMovieEntity)

                dto.toDomain(savedMovie.toDomain())
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                movieDao.getMovieById(movieId)?.toDomain() ?: throw e
            } catch (e: HttpException) {
                if (e.code() >= 500 || e.code() == 408 || e.code() == 429) {
                    movieDao.getMovieById(movieId)?.toDomain() ?: throw e
                } else {
                    throw e
                }
            }
        }

        override suspend fun toggleFavorite(
            movie: Movie,
            isFavorite: Boolean,
        ) {
            movieDao.setFavorite(movie.toEntity(isFavorite = isFavorite), isFavorite)
        }

        override fun getFavoriteMovies(): Flow<List<Movie>> =
            movieDao
                .getFavoriteMoviesFlow()
                .map { entities -> entities.map { it.toDomain() } }

        override fun getFavoriteMovieIds(): Flow<Set<Int>> =
            movieDao
                .getFavoriteMovieIdsFlow()
                .map { ids -> ids.toSet() }

        override suspend fun getMovieCredits(movieId: Int) =
            apiService.getMovieCredits(movieId).toDomain()

        override suspend fun getMovieRecommendations(movieId: Int): List<Movie> =
            apiService
                .getMovieRecommendations(movieId)
                .movies
                .map { it.toDomain() }
    }
