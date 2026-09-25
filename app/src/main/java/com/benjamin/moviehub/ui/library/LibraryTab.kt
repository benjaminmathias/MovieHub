package com.benjamin.moviehub.ui.library

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.ui.graphics.vector.ImageVector
import com.benjamin.moviehub.R
import com.benjamin.moviehub.domain.model.LibraryFlag

/**
 * The three local movie collections. Each entry owns its labels, icons and the
 * [LibraryFlag] that selects its movies and removes one from it.
 */
enum class LibraryTab(
    @param:StringRes val labelRes: Int,
    @param:StringRes val emptyMessageRes: Int,
    @param:StringRes val removeLabelRes: Int,
    val emptyIcon: ImageVector,
    val removeIcon: ImageVector,
    val flag: LibraryFlag,
) {
    WATCHLIST(
        labelRes = R.string.watchlist_tab,
        emptyMessageRes = R.string.no_watchlist_added,
        removeLabelRes = R.string.remove_watchlist_accessibility,
        emptyIcon = Icons.Outlined.BookmarkBorder,
        removeIcon = Icons.Filled.BookmarkRemove,
        flag = LibraryFlag.WATCHLIST,
    ),
    FAVORITES(
        labelRes = R.string.favorite_tab,
        emptyMessageRes = R.string.no_favorite_added,
        removeLabelRes = R.string.remove_favorite_accessibility,
        emptyIcon = Icons.Outlined.FavoriteBorder,
        removeIcon = Icons.Filled.HeartBroken,
        flag = LibraryFlag.FAVORITE,
    ),
    WATCHED(
        labelRes = R.string.watched_tab,
        emptyMessageRes = R.string.no_watched_added,
        removeLabelRes = R.string.remove_watched_accessibility,
        emptyIcon = Icons.Filled.CheckCircleOutline,
        removeIcon = Icons.Filled.VisibilityOff,
        flag = LibraryFlag.WATCHED,
    ),
}
