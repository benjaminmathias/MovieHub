package com.benjamin.moviehub.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.benjamin.moviehub.R
import com.benjamin.moviehub.core.theme.ContentHorizontalPadding
import com.benjamin.moviehub.core.theme.POSTER_ASPECT_RATIO
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.ui.components.MovieBackdropArtwork
import com.benjamin.moviehub.ui.components.MoviePosterArtwork
import com.benjamin.moviehub.ui.components.MovieRating
import com.benjamin.moviehub.ui.components.favoriteActionLabel
import com.benjamin.moviehub.ui.components.favoriteIcon
import java.text.NumberFormat
import java.util.Locale

/** Maximum width of the detail content, centered so the layout stays readable on wide screens. */
internal val DetailContentMaxWidth = 840.dp

/** Height of the backdrop hero that the summary overlaps. */
internal val DetailHeroHeight = 232.dp

private val SummaryOverlap = 32.dp
private val SummaryTopPadding = 16.dp
private val SummaryPosterSpacing = 16.dp

private val PosterWidthCompact = 96.dp
private val PosterWidth = 112.dp
private val PosterWidthWide = 128.dp

private val SummaryStackBreakpoint = 360.dp
private const val SUMMARY_STACK_FONT_SCALE = 1.15f

private val HeroTopScrim =
    Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.32f), Color.Transparent))

@Composable
internal fun MovieDetailHeader(
    movie: Movie,
    director: String?,
    libraryActionsEnabled: Boolean,
    onToggleFavorite: (() -> Unit)?,
    onToggleWatchlist: (() -> Unit)?,
    onToggleWatched: (() -> Unit)?,
    onOpenTmdb: (() -> Unit)?,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        MovieDetailHero(backdropPath = movie.backdropPath)
        Box(
            modifier = Modifier.fillMaxWidth().padding(top = DetailHeroHeight - SummaryOverlap),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(modifier = Modifier.widthIn(max = DetailContentMaxWidth).fillMaxWidth()) {
                MovieDetailSummary(movie = movie, director = director)
                if (onToggleFavorite != null || onToggleWatchlist != null || onToggleWatched != null || onOpenTmdb != null) {
                    MovieDetailActions(
                        movie = movie,
                        libraryActionsEnabled = libraryActionsEnabled,
                        onToggleFavorite = onToggleFavorite,
                        onToggleWatchlist = onToggleWatchlist,
                        onToggleWatched = onToggleWatched,
                        onOpenTmdb = onOpenTmdb,
                    )
                }
            }
        }
    }
}

@Composable
private fun MovieDetailHero(backdropPath: String?) {
    val surface = MaterialTheme.colorScheme.surface
    val bottomScrim = remember(surface) { Brush.verticalGradient(listOf(Color.Transparent, surface)) }
    Box(
        modifier = Modifier.fillMaxWidth().height(DetailHeroHeight).testTag("detail_hero"),
    ) {
        MovieBackdropArtwork(model = backdropPath, modifier = Modifier.fillMaxSize())
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .align(Alignment.TopCenter)
                    .background(HeroTopScrim),
        )
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .align(Alignment.BottomCenter)
                    .background(bottomScrim),
        )
    }
}

/**
 * Poster and title block. Side by side by default, stacked under a narrow width or a large
 * font scale so neither the poster nor the text is squeezed.
 */
@Composable
private fun MovieDetailSummary(
    movie: Movie,
    director: String?,
) {
    val year = movie.releaseDate.take(4).takeIf { it.length == 4 }
    val runtime =
        movie.runtimeMinutes?.takeIf { it > 0 }?.let { minutes ->
            stringResource(R.string.runtime_format, minutes / 60, minutes % 60)
        }
    val metadata = listOfNotNull(year, runtime).joinToString(" • ")
    val genres = movie.genres.joinToString(" • ")

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val stacked = maxWidth < SummaryStackBreakpoint || LocalDensity.current.fontScale > SUMMARY_STACK_FONT_SCALE
        val posterWidth =
            when {
                maxWidth < SummaryStackBreakpoint -> PosterWidthCompact
                maxWidth >= 600.dp -> PosterWidthWide
                else -> PosterWidth
            }
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = ContentHorizontalPadding, end = ContentHorizontalPadding, top = SummaryTopPadding),
        ) {
            if (stacked) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(SummaryPosterSpacing),
                ) {
                    MovieDetailPoster(posterPath = movie.posterPath, width = posterWidth)
                    MovieDetailSummaryText(
                        movie = movie,
                        metadata = metadata,
                        director = director,
                        genres = genres,
                        centered = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SummaryPosterSpacing),
                    verticalAlignment = Alignment.Top,
                ) {
                    MovieDetailPoster(posterPath = movie.posterPath, width = posterWidth)
                    MovieDetailSummaryText(
                        movie = movie,
                        metadata = metadata,
                        director = director,
                        genres = genres,
                        centered = false,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun MovieDetailPoster(
    posterPath: String?,
    width: Dp,
) {
    Surface(
        modifier = Modifier.width(width).aspectRatio(POSTER_ASPECT_RATIO).testTag("detail_poster"),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
        shadowElevation = 4.dp,
    ) {
        MoviePosterArtwork(model = posterPath, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun MovieDetailSummaryText(
    movie: Movie,
    metadata: String,
    director: String?,
    genres: String,
    centered: Boolean,
    modifier: Modifier = Modifier,
) {
    val textAlign = if (centered) TextAlign.Center else TextAlign.Start
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = movie.title,
            style = MaterialTheme.typography.headlineSmall.copy(lineBreak = LineBreak.Heading),
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = textAlign,
            modifier = Modifier.fillMaxWidth(),
        )
        if (metadata.isNotEmpty() || director != null || genres.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (metadata.isNotEmpty()) {
                    Text(
                        text = metadata,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = textAlign,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                director?.let {
                    Text(
                        text = stringResource(R.string.director_format, it),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = textAlign,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (genres.isNotEmpty()) {
                    Text(
                        text = genres,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = textAlign,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        MovieDetailRating(movie = movie, centered = centered)
    }
}

@Composable
private fun MovieDetailRating(
    movie: Movie,
    centered: Boolean,
) {
    val integerFormat = remember { NumberFormat.getIntegerInstance(Locale.FRANCE) }
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.spacedBy(8.dp, if (centered) Alignment.CenterHorizontally else Alignment.Start),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        if (movie.voteAverage > 0) {
            MovieRating(
                value = movie.voteAverage,
                iconSize = 18.dp,
                textStyle = MaterialTheme.typography.titleMedium,
                textRes = R.string.rating_out_of_ten,
            )
            movie.voteCount?.takeIf { it > 0 }?.let { voteCount ->
                Text(
                    text =
                        stringResource(
                            R.string.vote_count,
                            integerFormat.format(voteCount),
                        ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.rating_not_available),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MovieDetailActions(
    movie: Movie,
    libraryActionsEnabled: Boolean,
    onToggleFavorite: (() -> Unit)?,
    onToggleWatchlist: (() -> Unit)?,
    onToggleWatched: (() -> Unit)?,
    onOpenTmdb: (() -> Unit)?,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = ContentHorizontalPadding, end = ContentHorizontalPadding, top = 16.dp),
    ) {
        val actions =
            listOfNotNull(
                onToggleFavorite?.let { toggle ->
                    LibraryActionSpec(
                        selected = movie.isFavorite,
                        label = stringResource(R.string.favorite_tab),
                        contentDescription = favoriteActionLabel(movie.isFavorite),
                        icon = favoriteIcon(movie.isFavorite),
                        onClick = toggle,
                        testTag = "detail_favorite",
                    )
                },
                onToggleWatchlist?.let { toggle ->
                    LibraryActionSpec(
                        selected = movie.isWatchlist,
                        label = stringResource(R.string.watchlist_short),
                        contentDescription =
                            stringResource(
                                if (movie.isWatchlist) R.string.remove_watchlist_accessibility else R.string.add_watchlist_accessibility,
                            ),
                        icon = if (movie.isWatchlist) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        onClick = toggle,
                        testTag = "detail_watchlist",
                    )
                },
                onToggleWatched?.let { toggle ->
                    LibraryActionSpec(
                        selected = movie.isWatched,
                        label = stringResource(R.string.watched_short),
                        contentDescription =
                            stringResource(
                                if (movie.isWatched) R.string.mark_unwatched_accessibility else R.string.mark_watched_accessibility,
                            ),
                        icon = if (movie.isWatched) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircleOutline,
                        onClick = toggle,
                        testTag = "detail_watched",
                    )
                },
            )
        if (actions.isNotEmpty()) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val stacked = maxWidth < SummaryStackBreakpoint || LocalDensity.current.fontScale > SUMMARY_STACK_FONT_SCALE
                LibraryActionGroup(
                    actions = actions,
                    enabled = libraryActionsEnabled,
                    stacked = stacked,
                )
            }
        }
        onOpenTmdb?.let { openTmdb ->
            TextButton(
                onClick = openTmdb,
                modifier = Modifier.align(Alignment.Start).heightIn(min = 48.dp).testTag("detail_tmdb"),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.open_tmdb), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

private data class LibraryActionSpec(
    val selected: Boolean,
    val label: String,
    val contentDescription: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val testTag: String,
)

@Composable
private fun LibraryActionGroup(
    actions: List<LibraryActionSpec>,
    enabled: Boolean,
    stacked: Boolean,
) {
    if (stacked) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            actions.forEach { action ->
                LibraryAction(
                    selected = action.selected,
                    label = action.label,
                    contentDescription = action.contentDescription,
                    icon = action.icon,
                    onClick = action.onClick,
                    testTag = action.testTag,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled,
                )
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            actions.forEach { action ->
                LibraryAction(
                    selected = action.selected,
                    label = action.label,
                    contentDescription = action.contentDescription,
                    icon = action.icon,
                    onClick = action.onClick,
                    testTag = action.testTag,
                    modifier = Modifier.weight(1f),
                    enabled = enabled,
                )
            }
        }
    }
}

/**
 * Equal-weight library toggle: icon above its label, so the three actions stay balanced and
 * legible at any width and font scale. The fill/outline icon carries the selected state
 * alongside the container color and the checkbox semantics.
 */
@Composable
private fun LibraryAction(
    selected: Boolean,
    label: String,
    contentDescription: String,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shape = MaterialTheme.shapes.small
    val containerColor =
        if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow
    val contentColor =
        if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier =
            modifier
                .alpha(if (enabled) 1f else 0.5f)
                .heightIn(min = 48.dp)
                .clip(shape)
                .background(containerColor)
                .border(
                    width = 1.dp,
                    color = if (selected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                    shape = shape,
                ).toggleable(
                    value = selected,
                    enabled = enabled,
                    role = Role.Checkbox,
                    onValueChange = { onClick() },
                ).testTag(testTag)
                .semantics(mergeDescendants = true) { this.contentDescription = contentDescription }
                .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
