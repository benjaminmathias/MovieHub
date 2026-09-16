package com.benjamin.moviehub.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.RemoteMediator
import androidx.paging.cachedIn
import androidx.paging.map
import com.benjamin.moviehub.data.local.GenreEntity
import com.benjamin.moviehub.data.local.MovieDao
import com.benjamin.moviehub.data.local.MovieDatabase
import com.benjamin.moviehub.data.local.MovieEntity
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

@OptIn(ExperimentalPagingApi::class)
class MovieRepositoryImpl
    @Inject
    constructor(
        private val apiService: MovieApiService,
        private val database: MovieDatabase,
        private val movieDao: MovieDao,
    ) : MovieRepository {
        override fun getCategoryMovies(category: MovieCategory): Flow<PagingData<Movie>> =
            offlineFeed(
                mediator = MovieRemoteMediator(apiService, database, category),
                sourceFactory = { movieDao.getCategoryMoviesPaging(category.key) },
            )

        override fun searchMovies(query: String): Flow<PagingData<Movie>> =
            offlineFeed(
                mediator = SearchMovieRemoteMediator(apiService, database, query),
                sourceFactory = { movieDao.searchMoviesPaging(SearchQueryKey.normalize(query)) },
            )

        override fun getDiscoverMovies(filters: DiscoverFilters): Flow<PagingData<Movie>> =
            flow {
                coroutineScope {
                    // Scope the cache to each collection so a Room refresh can safely
                    // re-emit the same PagingData instance without double collection.
                    val pager =
                        Pager(config = moviePagingConfig) { DiscoverMoviePagingSource(apiService, filters) }
                            .flow
                            .cachedIn(this)
                    combine(pager, movieDao.getLibraryMoviesFlow()) { pagingData, libraryMovies ->
                        val localById = libraryMovies.associateBy { it.id }
                        val names = genreNameMap()
                        pagingData.map { movie -> movie.withGenreNames(names).withLocalFlags(localById[movie.id]) }
                    }.collect(::emit)
                }
            }

        override suspend fun getMovieGenres(): List<MovieGenre> {
            val genres =
                apiService
                    .getMovieGenres()
                    .genres
                    .mapNotNull { genre ->
                        genre.name
                            ?.trim()
                            ?.takeIf(String::isNotEmpty)
                            ?.let { name -> MovieGenre(id = genre.id, name = name) }
                    }
            movieDao.upsertGenres(genres.map { GenreEntity(id = it.id, name = it.name) })
            return genres
        }

        override fun getHeroMovie(category: MovieCategory): Flow<Movie?> =
            movieDao.getHeroMovieFlow(category.key).map { entity -> entity?.toDomain(genreNameMap()) }

        override suspend fun getMovieDetails(movieId: Int): Movie {
            val names = genreNameMap()
            return try {
                val dto = apiService.getMovieDetails(movieId)
                val saved = movieDao.upsertMovieDetails(dto.toEntity())
                dto.toDomain(saved.toDomain(names))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (!e.isRecoverable()) throw e
                movieDao.getMovieById(movieId)?.toDomain(names) ?: throw e
            }
        }

        override suspend fun setFavorite(
            movie: Movie,
            isFavorite: Boolean,
        ) = movieDao.setLibraryFlag(movie.toEntity(), isFavorite = isFavorite)

        override suspend fun setWatchlist(
            movie: Movie,
            isWatchlist: Boolean,
        ) = movieDao.setLibraryFlag(movie.toEntity(), isWatchlist = isWatchlist)

        override suspend fun setWatched(
            movie: Movie,
            isWatched: Boolean,
        ) = movieDao.setLibraryFlag(movie.toEntity(), isWatched = isWatched)

        override fun getLibraryMovies(): Flow<List<Movie>> =
            movieDao.getLibraryMoviesFlow().map { entities ->
                val names = genreNameMap()
                entities.map { it.toDomain(names) }
            }

        override suspend fun getMovieCredits(movieId: Int) = apiService.getMovieCredits(movieId).toDomain()

        override suspend fun getMovieRecommendations(movieId: Int): List<Movie> {
            val dtos = apiService.getMovieRecommendations(movieId).movies
            val localById = movieDao.getMoviesByIds(dtos.map { it.id }).associateBy { it.id }
            val names = genreNameMap()
            return dtos.map { dto -> dto.toDomain(names).withLocalFlags(localById[dto.id]) }
        }

        private fun offlineFeed(
            mediator: RemoteMediator<Int, MovieEntity>,
            sourceFactory: () -> PagingSource<Int, MovieEntity>,
        ): Flow<PagingData<Movie>> =
            Pager(
                config = moviePagingConfig,
                remoteMediator = mediator,
                pagingSourceFactory = sourceFactory,
            ).flow.mapEntities()

        private fun Flow<PagingData<MovieEntity>>.mapEntities(): Flow<PagingData<Movie>> =
            map { pagingData ->
                val names = genreNameMap()
                pagingData.map { entity -> entity.toDomain(names) }
            }

        private suspend fun genreNameMap(): Map<Int, String> = movieDao.getGenres().toNameMap()

        private fun Exception.isRecoverable(): Boolean =
            when (this) {
                is IOException -> true
                is HttpException -> code() >= 500 || code() == 408 || code() == 429
                else -> false
            }
    }

private fun List<GenreEntity>.toNameMap(): Map<Int, String> = associate { it.id to it.name }
