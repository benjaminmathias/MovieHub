package com.benjamin.moviehub.data.repository

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import androidx.room.withTransaction
import com.benjamin.moviehub.BuildConfig
import com.benjamin.moviehub.data.local.MovieDao
import com.benjamin.moviehub.data.local.MovieDatabase
import com.benjamin.moviehub.data.local.MovieRemoteKey
import com.benjamin.moviehub.data.local.SearchQueryKey
import com.benjamin.moviehub.data.mapper.toDomain
import com.benjamin.moviehub.data.mapper.toEntity
import com.benjamin.moviehub.data.paging.INITIAL_LOAD_SIZE
import com.benjamin.moviehub.data.paging.MovieRemoteMediator
import com.benjamin.moviehub.data.paging.PAGE_SIZE
import com.benjamin.moviehub.data.paging.POPULAR_REMOTE_KEY_TYPE
import com.benjamin.moviehub.data.paging.PREFETCH_DISTANCE
import com.benjamin.moviehub.data.paging.SearchMovieRemoteMediator
import com.benjamin.moviehub.data.remote.MovieApiService
import com.benjamin.moviehub.data.remote.isEndOfPagination
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.repository.MovieRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
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
        override fun getPagedMovies(query: String?): Flow<PagingData<Movie>> {
            val effectiveQuery = query?.trim()
            val isSearch = !effectiveQuery.isNullOrEmpty()
            val queryKey = effectiveQuery?.let(SearchQueryKey::normalize)

            return Pager(
                config =
                    PagingConfig(
                        pageSize = PAGE_SIZE,
                        prefetchDistance = PREFETCH_DISTANCE,
                        initialLoadSize = INITIAL_LOAD_SIZE,
                        enablePlaceholders = false,
                    ),
                remoteMediator =
                    if (isSearch) {
                        SearchMovieRemoteMediator(apiService, database, requireNotNull(effectiveQuery))
                    } else {
                        MovieRemoteMediator(apiService, database)
                    },
                pagingSourceFactory = {
                    if (isSearch) {
                        movieDao.searchMoviesPaging(requireNotNull(queryKey))
                    } else {
                        movieDao.getPopularMoviesPaging()
                    }
                },
            ).flow
                .map { pagingData ->
                    pagingData.map { entity -> entity.toDomain() }
                }
        }

        override suspend fun getMovieDetails(movieId: Int): Movie {
            return try {
                // API call
                val dto =
                    apiService.getMovieDetails(
                        movieId = movieId,
                        apiKey = BuildConfig.TMDB_API_KEY,
                    )

                val remoteMovieEntity = dto.toEntity()
                val savedMovie = movieDao.upsertMovieDetails(remoteMovieEntity)

                dto.toDomain(savedMovie.toDomain())
            } catch (e: kotlinx.coroutines.CancellationException) {
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
            movieDao.setFavorite(movie.toEntity(isFavorite = isFavorite, isPopular = false), isFavorite)
        }

        override fun getFavoriteMovies(): Flow<List<Movie>> =
            movieDao
                .getFavoriteMoviesFlow()
                .map { entities ->
                    entities.map { it.toDomain() }
                }.flowOn(Dispatchers.IO)

        override suspend fun getMovieCredits(movieId: Int) =
            try {
                val response = apiService.getMovieCredits(movieId, BuildConfig.TMDB_API_KEY)
                Result.success(response.toDomain())
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }

        override suspend fun syncPopularMoviesCache() {
            try {
                val response =
                    apiService.getPopularMovies(
                        apiKey = BuildConfig.TMDB_API_KEY,
                        page = 1,
                    )

                database.withTransaction {
                    val movieIds = response.movies.map { it.id }
                    val localMovies =
                        if (movieIds.isEmpty()) {
                            emptyMap()
                        } else {
                            movieDao.getMoviesByIds(movieIds).associateBy { it.id }
                        }

                    val isFirstPageOnly = response.isEndOfPagination(page = 1, pageSize = PAGE_SIZE)
                    if (isFirstPageOnly) {
                        movieDao.clearRemoteKeysByType(POPULAR_REMOTE_KEY_TYPE)
                        movieDao.clearPopularMovies()
                    } else {
                        val cachedFirstPageIds = movieDao.getPopularFirstPage(PAGE_SIZE).map { it.id }
                        movieDao.clearPopularFirstPage(PAGE_SIZE)
                        if (cachedFirstPageIds.isNotEmpty()) {
                            movieDao.clearRemoteKeysForMovies(cachedFirstPageIds, POPULAR_REMOTE_KEY_TYPE)
                        }
                    }

                    val remoteEntities =
                        response.movies.mapIndexed { index, dto ->
                            val localMovie = localMovies[dto.id]

                            dto.toEntity(
                                isFavorite = localMovie?.isFavorite ?: false,
                                isPopular = true,
                                isSearchResult = localMovie?.isSearchResult ?: false,
                                pageOrder = index,
                                runtimeMinutesOverride = localMovie?.runtimeMinutes,
                            )
                        }
                    val remoteKeys =
                        response.movies.map { dto ->
                            MovieRemoteKey(
                                movieId = dto.id,
                                prevKey = null,
                                nextKey = if (isFirstPageOnly) null else 2,
                                type = POPULAR_REMOTE_KEY_TYPE,
                            )
                        }

                    movieDao.insertAllKeys(remoteKeys)
                    movieDao.upsertMovies(remoteEntities)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("SyncWorker", "Échec de la synchronisation en arrière-plan", e)
                throw e
            }
        }
    }
