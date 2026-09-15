package com.benjamin.moviehub.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.benjamin.moviehub.data.local.GenreEntity
import com.benjamin.moviehub.data.local.MovieDao
import com.benjamin.moviehub.data.local.MovieDatabase
import com.benjamin.moviehub.data.local.SearchQueryKey
import com.benjamin.moviehub.data.mapper.toDomain
import com.benjamin.moviehub.data.mapper.toEntity
import com.benjamin.moviehub.data.mapper.withGenreNames
import com.benjamin.moviehub.data.mapper.withLocalFlags
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
        // Genre names are read once per emitted page rather than combined as a live flow:
        // a Room write to `genres` would otherwise re-emit the same PagingData instance and
        // PageFetcher rejects collecting it twice.
        @OptIn(ExperimentalPagingApi::class)
        override fun getCategoryMovies(category: MovieCategory): Flow<PagingData<Movie>> =
            Pager(
                config = moviePagingConfig,
                remoteMediator = MovieRemoteMediator(apiService, database, category),
                pagingSourceFactory = { movieDao.getCategoryMoviesPaging(category.key) },
            ).flow.map { pagingData ->
                val genreNames = movieDao.getGenres().toNameMap()
                pagingData.map { entity -> entity.toDomain(genreNames) }
            }

        @OptIn(ExperimentalPagingApi::class)
        override fun searchMovies(query: String): Flow<PagingData<Movie>> {
            val queryKey = SearchQueryKey.normalize(query)

            return Pager(
                config = moviePagingConfig,
                remoteMediator = SearchMovieRemoteMediator(apiService, database, query),
                pagingSourceFactory = { movieDao.searchMoviesPaging(queryKey) },
            ).flow.map { pagingData ->
                val genreNames = movieDao.getGenres().toNameMap()
                pagingData.map { entity -> entity.toDomain(genreNames) }
            }
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
                        val genreNames = movieDao.getGenres().toNameMap()
                        pagingData.map { movie ->
                            movie.withGenreNames(genreNames).withLocalFlags(localById[movie.id])
                        }
                    }.collect(::emit)
                }
            }

        override suspend fun getMovieGenres(): List<MovieGenre> {
            val genres =
                apiService
                    .getMovieGenres()
                    .genres
                    .mapNotNull { genre ->
                        genre.name?.trim()?.takeIf(String::isNotEmpty)?.let { name ->
                            MovieGenre(id = genre.id, name = name)
                        }
                    }
            movieDao.upsertGenres(genres.map { GenreEntity(id = it.id, name = it.name) })
            return genres
        }

        override fun getHeroMovie(category: MovieCategory): Flow<Movie?> =
            movieDao
                .getHeroMovieFlow(category.key)
                .map { entity -> entity?.toDomain(movieDao.getGenres().toNameMap()) }

        override suspend fun getMovieDetails(movieId: Int): Movie {
            val genreNames = movieDao.getGenres().toNameMap()

            return try {
                val dto = apiService.getMovieDetails(movieId = movieId)

                val remoteMovieEntity = dto.toEntity()
                val savedMovie = movieDao.upsertMovieDetails(remoteMovieEntity)

                dto.toDomain(savedMovie.toDomain(genreNames))
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                movieDao.getMovieById(movieId)?.toDomain(genreNames) ?: throw e
            } catch (e: HttpException) {
                if (e.code() >= 500 || e.code() == 408 || e.code() == 429) {
                    movieDao.getMovieById(movieId)?.toDomain(genreNames) ?: throw e
                } else {
                    throw e
                }
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
            movieDao
                .getLibraryMoviesFlow()
                .map { entities ->
                    val genreNames = movieDao.getGenres().toNameMap()
                    entities.map { it.toDomain(genreNames) }
                }

        override suspend fun getMovieCredits(movieId: Int) = apiService.getMovieCredits(movieId).toDomain()

        override suspend fun getMovieRecommendations(movieId: Int): List<Movie> =
            apiService.getMovieRecommendations(movieId).movies.let { dtos ->
                val localById = movieDao.getMoviesByIds(dtos.map { it.id }).associateBy { it.id }
                val genreNames = movieDao.getGenres().toNameMap()
                dtos.map { dto ->
                    dto.toDomain(genreNames).withLocalFlags(localById[dto.id])
                }
            }
    }

private fun List<GenreEntity>.toNameMap(): Map<Int, String> = associate { it.id to it.name }
