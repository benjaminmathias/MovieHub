package com.benjamin.moviehub.data.local

import com.benjamin.moviehub.domain.model.LibraryFlag

/**
 * Applies one Library flag change. Watchlist and watched are mutually exclusive:
 * setting one true clears the other, setting one false leaves the other unchanged.
 */
internal fun MovieEntity.withLibraryFlag(
    flag: LibraryFlag,
    value: Boolean,
): MovieEntity =
    when (flag) {
        LibraryFlag.FAVORITE -> copy(isFavorite = value)
        LibraryFlag.WATCHLIST -> copy(isWatchlist = value, isWatched = isWatched && !value)
        LibraryFlag.WATCHED -> copy(isWatchlist = isWatchlist && !value, isWatched = value)
    }

/**
 * Merges a network row with its cached copy: local Library flags always win, a
 * known runtime is never erased by a payload without one, and a row that ends up
 * both watched and in the watchlist keeps only watched.
 */
internal fun MovieEntity.mergedWithLocal(local: MovieEntity?): MovieEntity {
    val watched = local?.isWatched ?: isWatched
    return copy(
        isFavorite = local?.isFavorite ?: isFavorite,
        isWatchlist = (local?.isWatchlist ?: isWatchlist) && !watched,
        isWatched = watched,
        runtimeMinutes = runtimeMinutes ?: local?.runtimeMinutes,
    )
}
