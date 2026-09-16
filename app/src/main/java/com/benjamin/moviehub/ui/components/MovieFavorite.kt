package com.benjamin.moviehub.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.benjamin.moviehub.R

/** Filled heart when favorited, outline otherwise. */
internal fun favoriteIcon(isFavorite: Boolean): ImageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder

/** Status description announced by a card that exposes its favorite state. */
@Composable
internal fun favoriteStateDescription(isFavorite: Boolean): String =
    stringResource(if (isFavorite) R.string.favorite_state else R.string.not_favorite_state)

/** Label of the favorite toggle, used as its content description. */
@Composable
internal fun favoriteActionLabel(isFavorite: Boolean): String =
    stringResource(if (isFavorite) R.string.remove_favorite else R.string.favorite)
