package com.benjamin.moviehub.data.repository

import com.benjamin.moviehub.data.local.MovieDao
import com.benjamin.moviehub.data.mapper.toDomain
import com.benjamin.moviehub.data.mapper.toEntity
import com.benjamin.moviehub.domain.model.LibraryFlag
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
        override suspend fun setLibraryFlag(
            movie: Movie,
            flag: LibraryFlag,
            value: Boolean,
        ) = movieDao.setLibraryFlag(movie.toEntity(), flag, value)

        override suspend fun toggleFavorite(movie: Movie) = movieDao.toggleFavorite(movie.toEntity())

        override fun getLibraryMovies(): Flow<List<Movie>> =
            movieDao.getLibraryMoviesFlow().map { entities ->
                val names = movieDao.getGenres().associate { it.id to it.name }
                entities.map { it.toDomain(names) }
            }
    }
