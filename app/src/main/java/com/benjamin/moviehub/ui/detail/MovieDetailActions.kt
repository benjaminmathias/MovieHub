package com.benjamin.moviehub.ui.detail

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.benjamin.moviehub.R
import com.benjamin.moviehub.core.theme.ContentHorizontalPadding
import com.benjamin.moviehub.domain.model.Movie

@Composable
internal fun MovieDetailActions(
    movie: Movie,
    onToggleFavorite: (() -> Unit)?,
    onToggleWatchlist: (() -> Unit)?,
    onToggleWatched: (() -> Unit)?,
    onOpenTmdb: (() -> Unit)?,
) {
    val favoriteLabel = stringResource(if (movie.isFavorite) R.string.remove_favorite else R.string.favorite)
    val favoriteScale by animateFloatAsState(
        targetValue = if (movie.isFavorite) 1.25f else 1f,
        animationSpec =
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
        label = "favoritePop",
    )
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = ContentHorizontalPadding, end = ContentHorizontalPadding, top = 16.dp, bottom = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            onToggleFavorite?.let { toggle ->
                LibraryAction(
                    selected = movie.isFavorite,
                    label = stringResource(R.string.favorite_tab),
                    contentDescription = favoriteLabel,
                    icon = if (movie.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    onClick = toggle,
                    modifier = Modifier.weight(1f),
                    testTag = "detail_favorite",
                    iconModifier = Modifier.graphicsLayer { scaleX = favoriteScale; scaleY = favoriteScale },
                )
            }
            onToggleWatchlist?.let { toggle ->
                LibraryAction(
                    selected = movie.isWatchlist,
                    label = stringResource(R.string.watchlist_short),
                    contentDescription = stringResource(if (movie.isWatchlist) R.string.remove_watchlist_accessibility else R.string.add_watchlist_accessibility),
                    icon = if (movie.isWatchlist) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    onClick = toggle,
                    modifier = Modifier.weight(1f),
                    testTag = "detail_watchlist",
                )
            }
            onToggleWatched?.let { toggle ->
                LibraryAction(
                    selected = movie.isWatched,
                    label = stringResource(R.string.watched_short),
                    contentDescription = stringResource(if (movie.isWatched) R.string.mark_unwatched_accessibility else R.string.mark_watched_accessibility),
                    icon = if (movie.isWatched) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircleOutline,
                    onClick = toggle,
                    modifier = Modifier.weight(1f),
                    testTag = "detail_watched",
                )
            }
        }
        onOpenTmdb?.let { openTmdb ->
            TextButton(
                onClick = openTmdb,
                modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp).testTag("detail_tmdb"),
            ) {
                Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.open_tmdb), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun LibraryAction(
    selected: Boolean,
    label: String,
    contentDescription: String,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier,
) {
    val iconColor =
        if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    val labelColor =
        if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    val indicatorColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        label = "libraryActionIndicator",
    )
    Column(
        modifier =
            modifier
                .heightIn(min = 48.dp)
                .toggleable(
                    value = selected,
                    role = Role.Checkbox,
                    onValueChange = { onClick() },
                )
                .testTag(testTag)
                .semantics(mergeDescendants = true) {
                    this.contentDescription = contentDescription
                },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape).background(indicatorColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = iconModifier.size(20.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = labelColor,
            maxLines = 1,
        )
    }
}
