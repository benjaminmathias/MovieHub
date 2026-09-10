package com.benjamin.moviehub.data.mapper

import com.benjamin.moviehub.data.local.MovieEntity
import com.benjamin.moviehub.data.remote.ActorDto
import com.benjamin.moviehub.data.remote.MovieCreditsDto
import com.benjamin.moviehub.data.remote.MovieDto
import com.benjamin.moviehub.domain.model.Actor
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.model.MovieCredits

private const val TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/"

private val tmdbGenreNames =
    mapOf(
        "Action" to 28,
        "Aventure" to 12,
        "Animation" to 16,
        "Comédie" to 35,
        "Crime" to 80,
        "Documentaire" to 99,
        "Drame" to 18,
        "Familial" to 10751,
        "Fantastique" to 14,
        "Histoire" to 36,
        "Horreur" to 27,
        "Musique" to 10402,
        "Mystère" to 9648,
        "Romance" to 10749,
        "Science-Fiction" to 878,
        "Téléfilm" to 10770,
        "Thriller" to 53,
        "Guerre" to 10752,
        "Western" to 37,
    ).entries.associate { (name, id) -> id to name }

private fun normalizeImagePath(path: String?): String? {
    val value = path?.trim().orEmpty()
    if (value.isEmpty()) return null

    val tmdbPath = value.substringAfter("image.tmdb.org/t/p/", missingDelimiterValue = "")
    if (tmdbPath.isNotEmpty()) {
        return tmdbPath.substringAfter('/', missingDelimiterValue = "").takeIf { it.isNotEmpty() }?.let { "/$it" }
    }

    return value.takeIf { !it.startsWith("http://") && !it.startsWith("https://") }?.let {
        if (it.startsWith('/')) it else "/$it"
    } ?: value
}

private fun toTmdbImageUrl(
    path: String?,
    size: String,
): String {
    val normalizedPath = normalizeImagePath(path) ?: return ""
    if (normalizedPath.startsWith("http://") || normalizedPath.startsWith("https://")) {
        return normalizedPath
    }
    return "$TMDB_IMAGE_BASE_URL$size$normalizedPath"
}

/**
 * Convert a MovieDto (API response) to a MovieEntity(DB entity)
 */
fun MovieDto.toEntity(
    isFavorite: Boolean = false,
    isSearchResult: Boolean = false,
    runtimeMinutesOverride: Int? = null,
): MovieEntity {
    val finalGenreIds =
        this.genreIds
            ?: this.genres?.map { it.id }
            ?: emptyList()

    return MovieEntity(
        id = this.id,
        title = this.title.orEmpty(),
        overview = this.description.orEmpty(),
        posterPath = normalizeImagePath(this.posterPath),
        backdropPath = normalizeImagePath(this.backdropPath),
        voteAverage = this.voteAverage,
        releaseDate = this.releaseDate ?: "",
        genreIds = finalGenreIds,
        isFavorite = isFavorite,
        isSearchResult = isSearchResult,
        runtimeMinutes = runtimeMinutesOverride ?: runtimeMinutes,
    )
}

fun ActorDto.toDomain(): Actor = Actor(id, name.orEmpty(), character.orEmpty(), toTmdbImageUrl(profilePath, "w185"))

fun MovieCreditsDto.toDomain(): MovieCredits =
    MovieCredits(
        actors = cast.take(15).map { it.toDomain() },
        director =
            crew.firstOrNull { it.job == "Director" && !it.name.isNullOrBlank() }?.name?.trim(),
    )

/**
 * Convert a standalone MovieDto (list endpoints such as recommendations) to a Movie.
 */
fun MovieDto.toDomain(): Movie = toEntity().toDomain()

fun MovieDto.toDomain(baseMovie: Movie): Movie {
    val detailGenres = genres.orEmpty().mapNotNull { it.name?.trim()?.takeIf(String::isNotEmpty) }

    return baseMovie.copy(
        genres = detailGenres.ifEmpty { baseMovie.genres },
        voteCount = voteCount?.takeIf { it > 0 },
    )
}

/**
 * Convert a MovieEntity (DB entity) to a Movie (Domain model)
 */
fun MovieEntity.toDomain(): Movie =
    Movie(
        id = id,
        title = title,
        overview = overview,
        posterPath = toTmdbImageUrl(posterPath, "w500"),
        backdropPath = toTmdbImageUrl(backdropPath, "w780"),
        voteAverage = voteAverage,
        releaseDate = releaseDate,
        webUrl = "https://www.themoviedb.org/movie/$id",
        isFavorite = isFavorite,
        genreIds = genreIds,
        genres = genreIds.mapNotNull { tmdbGenreNames[it] },
        runtimeMinutes = runtimeMinutes,
        posterPathSmall = toTmdbImageUrl(posterPath, "w185"),
    )

/**
 * Convert a Movie (Domain model) to a MovieEntity (DB entity)
 */
fun Movie.toEntity(isFavorite: Boolean): MovieEntity =
    MovieEntity(
        id = this.id,
        title = this.title,
        overview = this.overview,
        posterPath = normalizeImagePath(this.posterPath),
        backdropPath = normalizeImagePath(this.backdropPath),
        releaseDate = this.releaseDate,
        voteAverage = this.voteAverage,
        genreIds = this.genreIds,
        isFavorite = isFavorite,
        runtimeMinutes = runtimeMinutes,
    )
