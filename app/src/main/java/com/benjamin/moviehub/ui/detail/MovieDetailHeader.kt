package com.benjamin.moviehub.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.benjamin.moviehub.R
import com.benjamin.moviehub.core.theme.ContentHorizontalPadding
import com.benjamin.moviehub.core.theme.PosterAspectRatio
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.ui.components.MovieBackdropArtwork
import com.benjamin.moviehub.ui.components.MoviePosterArtwork
import java.text.NumberFormat
import java.util.Locale

private val HeroHeight = 264.dp
private val PosterHeight = 180.dp
private val PosterWidth = 120.dp
private val SummaryOverlap = 32.dp

@Composable
internal fun MovieDetailHeader(
    movie: Movie,
    director: String?,
    onToggleFavorite: (() -> Unit)?,
    onToggleWatchlist: (() -> Unit)?,
    onToggleWatched: (() -> Unit)?,
    onOpenTmdb: (() -> Unit)?,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        MovieDetailHero(backdropPath = movie.backdropPath)
        Column(modifier = Modifier.padding(top = HeroHeight - SummaryOverlap)) {
            MovieDetailSummary(movie = movie, director = director)
            if (onToggleFavorite != null || onToggleWatchlist != null || onToggleWatched != null || onOpenTmdb != null) {
                MovieDetailActions(
                    movie = movie,
                    onToggleFavorite = onToggleFavorite,
                    onToggleWatchlist = onToggleWatchlist,
                    onToggleWatched = onToggleWatched,
                    onOpenTmdb = onOpenTmdb,
                )
            }
        }
    }
}

@Composable
private fun MovieDetailHero(backdropPath: String?) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(HeroHeight)
                .testTag("detail_hero"),
    ) {
        MovieBackdropArtwork(model = backdropPath, modifier = Modifier.fillMaxSize())
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.32f), Color.Transparent),
                        ),
                    ),
        )
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, MaterialTheme.colorScheme.surface),
                        ),
                    ),
        )
    }
}

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
    val metadata = listOfNotNull(year, runtime)

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = ContentHorizontalPadding, end = ContentHorizontalPadding, top = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.weight(1f).heightIn(min = PosterHeight),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (metadata.isNotEmpty() || director != null || movie.genres.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (metadata.isNotEmpty()) {
                            Text(
                                text = metadata.joinToString(" • "),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        director?.let {
                            Text(
                                text = stringResource(R.string.director_format, it),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (movie.genres.isNotEmpty()) {
                            Text(
                                text = movie.genres.joinToString(" • "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            MovieDetailRating(movie)
        }

        Surface(
            modifier = Modifier.width(PosterWidth).aspectRatio(PosterAspectRatio).testTag("detail_poster"),
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 2.dp,
            shadowElevation = 4.dp,
        ) {
            MoviePosterArtwork(model = movie.posterPath, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun MovieDetailRating(movie: Movie) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(18.dp),
        )
        if (movie.voteAverage > 0) {
            Text(
                text = stringResource(R.string.rating_out_of_ten, movie.voteAverage),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.tertiary,
                maxLines = 1,
            )
            movie.voteCount?.takeIf { it > 0 }?.let { voteCount ->
                Text(
                    text =
                        stringResource(
                            R.string.vote_count,
                            NumberFormat.getIntegerInstance(Locale.FRANCE).format(voteCount),
                        ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else {
            Text(
                text = stringResource(R.string.rating_not_available),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
