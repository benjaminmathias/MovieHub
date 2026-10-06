package com.benjamin.moviehub.data.repository

import com.benjamin.moviehub.data.local.MovieDao
import com.benjamin.moviehub.data.mapper.toDomain
import com.benjamin.moviehub.data.mapper.toEntity
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LibraryRepositoryImpl
    @Inject
    constructor(
        private val movieDao: MovieDao,
    ) : LibraryRepository {
        override suspend fun setFavorite(
            movie: Movie,
            isFavorite: Boolean,
        ) = movieDao.setLibraryFlag(movie.toEntity(isFavorite = isFavorite), isFavorite = isFavorite)

        override suspend fun toggleFavorite(movie: Movie) = movieDao.toggleFavorite(movie.toEntity())

        override suspend fun setWatchlist(
            movie: Movie,
            isWatchlist: Boolean,
        ) = movieDao.setLibraryFlag(movie.toEntity(isWatchlist = isWatchlist), isWatchlist = isWatchlist)

        override suspend fun setWatched(
            movie: Movie,
            isWatched: Boolean,
        ) = movieDao.setLibraryFlag(movie.toEntity(isWatched = isWatched), isWatched = isWatched)

        override fun getLibraryMovies(): Flow<List<Movie>> =
            movieDao.getLibraryMoviesFlow().map { entities ->
                val names = movieDao.getGenres().associate { it.id to it.name }
                entities.map { it.toDomain(names) }
            }
    }
