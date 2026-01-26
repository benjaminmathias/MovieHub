package com.benjamin.moviehub.data.mapper

import com.benjamin.moviehub.data.local.MovieEntity
import com.benjamin.moviehub.data.remote.MovieDto
import com.benjamin.moviehub.domain.model.Movie

/**
 * Convert a MovieDto (API response) to a MovieEntity(DB entity)
 */
fun MovieDto.toEntity(isFavorite: Boolean = false, isPopular: Boolean = false): MovieEntity {
    return MovieEntity(
        id = this.id,
        title = this.title,
        overview = this.description,
        posterPath = this.posterPath ?: "",
        backdropPath = this.backdropPath ?: "",
        voteAverage = this.voteAverage,
        releaseDate = this.releaseDate ?: "",
        isFavorite = isFavorite,
        isPopular = isPopular
    )
}

/**
 * Convert a MovieEntity (DB entity) to a Movie (Domain model)
 */
fun MovieEntity.toDomain(): Movie {
    return Movie(
        id = id,
        title = title,
        overview = overview,
        posterPath = "https://image.tmdb.org/t/p/w500$posterPath",
        backdropPath = "https://image.tmdb.org/t/p/w780$backdropPath",
        voteAverage = voteAverage,
        releaseDate = releaseDate,
        webUrl = "https://www.themoviedb.org/movie/$id",
        isFavorite = isFavorite
    )
}


/**
 * Convert a MovieDto (API response) to a Movie (Domain model)
 */
fun MovieDto.toDomain(): Movie {
    return Movie(
        id = this.id,
        title = this.title,
        overview = this.description,
        posterPath = "https://image.tmdb.org/t/p/w500${this.posterPath}",
        backdropPath = "https://image.tmdb.org/t/p/w780${this.backdropPath}",
        voteAverage = this.voteAverage,
        releaseDate = this.releaseDate ?: "",
        webUrl = "https://www.themoviedb.org/movie/${this.id}",
        isFavorite = false
    )
}

/**
 * Convert a Movie (Domain model) to a MovieEntity (DB entity)
 */
fun Movie.toEntity(isFavorite: Boolean, isPopular: Boolean): MovieEntity {
    return MovieEntity(
        id = this.id,
        title = this.title,
        overview = this.overview,
        posterPath = this.posterPath,
        backdropPath = this.backdropPath,
        releaseDate = this.releaseDate,
        voteAverage = this.voteAverage,
        isFavorite = isFavorite,
        isPopular = isPopular
    )
}