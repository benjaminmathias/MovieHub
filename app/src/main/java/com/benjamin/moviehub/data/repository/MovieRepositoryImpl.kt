package com.benjamin.moviehub.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingData
import androidx.paging.cachedIn
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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.io.IOException
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
            flow {
                coroutineScope {
                    // Scope the cache to each collection so a Room refresh can safely
                    // re-emit the same PagingData instance without double collection.
                    val pagerFlow =
                        Pager(
                            config = moviePagingConfig,
                            pagingSourceFactory = { DiscoverMoviePagingSource(apiService, filters) },
                        ).flow.cachedIn(this)
                    combine(
                        pagerFlow,
                        movieDao.getLibraryMoviesFlow(),
                    ) { pagingData, libraryMovies ->
                        val localById = libraryMovies.associateBy { it.id }
                        pagingData.map { movie ->
                            localById[movie.id]?.let { local ->
                                movie.copy(
                                    isFavorite = local.isFavorite,
                                    isWatchlist = local.isWatchlist,
                                    isWatched = local.isWatched,
                                )
                            } ?: movie
                        }
                    }.collect(::emit)
                }
            }

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

        override suspend fun getMovieDetails(movieId: Int): Movie =
            try {
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

        override suspend fun setFavorite(
            movie: Movie,
            isFavorite: Boolean,
        ) {
            movieDao.setFavorite(movie.toEntity(isFavorite = isFavorite), isFavorite)
        }

        override suspend fun setWatchlist(
            movie: Movie,
            isWatchlist: Boolean,
        ) {
            movieDao.setWatchlist(movie.toEntity(), isWatchlist)
        }

        override suspend fun setWatched(
            movie: Movie,
            isWatched: Boolean,
        ) {
            movieDao.setWatched(movie.toEntity(), isWatched)
        }

        override fun getLibraryMovies(): Flow<List<Movie>> =
            movieDao.getLibraryMoviesFlow().map { entities -> entities.map { it.toDomain() } }

        override suspend fun getMovieCredits(movieId: Int) = apiService.getMovieCredits(movieId).toDomain()

        override suspend fun getMovieRecommendations(movieId: Int): List<Movie> =
            apiService.getMovieRecommendations(movieId).movies.let { dtos ->
                val localById = movieDao.getMoviesByIds(dtos.map { it.id }).associateBy { it.id }
                dtos.map { dto ->
                    val remote = dto.toDomain()
                    localById[dto.id]?.let { local ->
                        remote.copy(
                            isFavorite = local.isFavorite,
                            isWatchlist = local.isWatchlist,
                            isWatched = local.isWatched,
                        )
                    } ?: remote
                }
            }
    }
