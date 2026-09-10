package com.benjamin.moviehub.domain.model

/**
 * The TMDB home movie feeds supported by MovieHub.
 *
 * [key] is the stable value persisted in `movie_categories.category` and used as
 * the [com.benjamin.moviehub.data.local.MovieRemoteKey.type], so it must not
 * change even if the enum entry is renamed.
 */
enum class MovieCategory(
    val key: String,
) {
    POPULAR("POPULAR"),
    NOW_PLAYING("NOW_PLAYING"),
    UPCOMING("UPCOMING"),
    TOP_RATED("TOP_RATED"),
}
