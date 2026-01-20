package com.benjamin.moviehub.data.mapper

import com.benjamin.moviehub.data.local.MovieEntity
import com.benjamin.moviehub.data.remote.MovieDto
import com.benjamin.moviehub.domain.model.Movie

// API to DB
fun MovieDto.toEntity(isFavorite: Boolean = false): MovieEntity {
    return MovieEntity(
        id = this.id,
        title = this.title,
        overview = this.description,
        posterPath = this.posterPath ?: "",
        backdropPath = this.backdropPath ?: "",
        voteAverage = this.voteAverage,
        releaseDate = this.releaseDate ?: "",
        isFavorite = isFavorite
    )
}

// DB to Domain
fun MovieEntity.toDomain(): Movie {
    return Movie(
        id = id,
        title = title,
        overview = overview,
        posterPath = "https://image.tmdb.org/t/p/w500$posterPath",
        backdropPath = "https://image.tmdb.org/t/p/w780$backdropPath",
        voteAverage = voteAverage,
        releaseDate = releaseDate,
        isFavorite = isFavorite
    )
}


// API to Domain
fun MovieDto.toDomain(): Movie {
    return Movie(
        id = this.id,
        title = this.title,
        overview = this.description,
        posterPath = "https://image.tmdb.org/t/p/w500${this.posterPath}",
        backdropPath = "https://image.tmdb.org/t/p/w780${this.backdropPath}",
        voteAverage = this.voteAverage,
        releaseDate = this.releaseDate ?: "",
        isFavorite = false
    )
}