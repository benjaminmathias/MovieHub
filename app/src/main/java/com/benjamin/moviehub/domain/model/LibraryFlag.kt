package com.benjamin.moviehub.domain.model

/**
 * One library flag and its responsibilities: read it, apply it optimistically
 * (watchlist and watched stay mutually exclusive) and restore the previous flags when
 * persistence fails. Persistence itself goes through
 * [com.benjamin.moviehub.domain.repository.setLibraryFlag].
 */
enum class LibraryFlag(
    val isSet: (Movie) -> Boolean,
    val apply: (Movie, Boolean) -> Movie,
    val restore: (Movie, Movie, Boolean) -> Movie,
) {
    FAVORITE(
        isSet = Movie::isFavorite,
        apply = { movie, value -> movie.copy(isFavorite = value) },
        restore = { movie, previous, _ -> movie.copy(isFavorite = previous.isFavorite) },
    ),
    WATCHLIST(
        isSet = Movie::isWatchlist,
        apply = { movie, value -> movie.copy(isWatchlist = value, isWatched = if (value) false else movie.isWatched) },
        restore = { movie, previous, value ->
            movie.copy(isWatchlist = previous.isWatchlist, isWatched = if (value) previous.isWatched else movie.isWatched)
        },
    ),
    WATCHED(
        isSet = Movie::isWatched,
        apply = { movie, value -> movie.copy(isWatched = value, isWatchlist = if (value) false else movie.isWatchlist) },
        restore = { movie, previous, value ->
            movie.copy(isWatched = previous.isWatched, isWatchlist = if (value) previous.isWatchlist else movie.isWatchlist)
        },
    ),
}
