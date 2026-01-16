package com.benjamin.moviehub.domain.data

import com.benjamin.moviehub.domain.model.Movie

fun MovieDto.toDomain() : Movie {
    return Movie(
        id = this.id,
        title = this.title,
        overview = this.description,
        posterPath = "https://image.tmdb.org/t/p/w500${this.posterPath}",
        backdropPath = "https://image.tmdb.org/t/p/w780${this.backdropPath}",
        voteAverage = this.voteAverage,
        releaseDate = ""
    )
}