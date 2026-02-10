package com.benjamin.moviehub.data.mapper

import com.benjamin.moviehub.data.local.MovieEntity
import com.benjamin.moviehub.data.remote.MovieDto
import com.benjamin.moviehub.domain.model.Movie

/**
 * Convert a MovieDto (API response) to a MovieEntity(DB entity)
 */
fun MovieDto.toEntity(isFavorite: Boolean = false, isPopular: Boolean = false, isSearchResult: Boolean = false, pageOrder: Int = 0): MovieEntity {
    return MovieEntity(
        id = this.id,
        title = this.title,
        overview = this.description,
        posterPath = this.posterPath ?: "",
        backdropPath = this.backdropPath ?: "",
        voteAverage = this.voteAverage,
        releaseDate = this.releaseDate ?: "",
        genreIds = this.genreIds ?: emptyList(),
        isFavorite = isFavorite,
        isPopular = isPopular,
        isSearchResult = isSearchResult,
        pageOrder = pageOrder
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
        isFavorite = isFavorite,
        genreIds = genreIds
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
        posterPath = if (this.posterPath != null ) "https://image.tmdb.org/t/p/w500${this.posterPath}" else "",
        backdropPath = if (this.backdropPath != null) "https://image.tmdb.org/t/p/w780${this.backdropPath}" else "",
        voteAverage = this.voteAverage,
        releaseDate = this.releaseDate ?: "",
        webUrl = "https://www.themoviedb.org/movie/${this.id}",
        isFavorite = false,
        genreIds = genreIds ?: emptyList()
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
        genreIds = this.genreIds,
        isFavorite = isFavorite,
        isPopular = isPopular
    )
}