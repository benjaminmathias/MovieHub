package com.benjamin.moviehub.domain.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class Movie(
    val id: Int,
    val title: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val voteAverage: Double,
    val releaseDate: String,
    val webUrl: String?,
    val isFavorite: Boolean,
    val genreIds: ImmutableList<Int>,
    val genres: ImmutableList<String>,
    val isWatchlist: Boolean = false,
    val isWatched: Boolean = false,
    val runtimeMinutes: Int? = null,
    val voteCount: Int? = null,
)

data class Actor(
    val id: Int,
    val name: String,
    val character: String,
    val profileUrl: String,
)

data class MovieCredits(
    val actors: ImmutableList<Actor> = persistentListOf(),
    val director: String? = null,
)

/**
 * The TMDB home movie feeds supported by MovieHub.
 *
 * [key] is the stable value persisted in `movie_categories.category` and used as
 * the [com.benjamin.moviehub.data.local.RemoteKey.type], so it must not change
 * even if the enum entry is renamed.
 */
enum class MovieCategory(
    val key: String,
) {
    POPULAR("POPULAR"),
    NOW_PLAYING("NOW_PLAYING"),
    UPCOMING("UPCOMING"),
    TOP_RATED("TOP_RATED"),
}

enum class DiscoverSortOption(
    val queryValue: String,
) {
    POPULARITY("popularity.desc"),
    RATING("vote_average.desc"),
    RELEASE_DATE("primary_release_date.desc"),
}

data class DiscoverFilters(
    val genreId: Int? = null,
    val releaseYear: Int? = null,
    val minimumVoteAverage: Double? = null,
    val sort: DiscoverSortOption = DiscoverSortOption.POPULARITY,
)

data class MovieGenre(
    val id: Int,
    val name: String,
)
