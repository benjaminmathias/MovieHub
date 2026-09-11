package com.benjamin.moviehub.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.benjamin.moviehub.R
import com.benjamin.moviehub.core.theme.MovieHubTheme
import com.benjamin.moviehub.domain.model.Movie
import com.valentinilk.shimmer.shimmer

@Composable
fun HeroMovieBanner(
    movie: Movie,
    onMovieClick: (Int) -> Unit,
    onToggleFavorite: (Movie, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = { onMovieClick(movie.id) },
        modifier = modifier.fillMaxWidth().padding(8.dp).testTag("hero_movie"),
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Box(modifier = Modifier.fillMaxWidth().heightIn(min = 300.dp)) {
            MovieBackdropArtwork(
                model = (movie.backdropPath?.takeIf(String::isNotBlank) ?: movie.posterPath),
                modifier = Modifier.matchParentSize(),
            )

            Box(
                modifier =
                    Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors =
                                    listOf(
                                        MaterialTheme.colorScheme.background.copy(alpha = 0.55f),
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.background.copy(alpha = 0.75f),
                                        MaterialTheme.colorScheme.background,
                                    ),
                            ),
                        ),
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SpotlightBadge()
                Spacer(modifier = Modifier.weight(1f))
                if (movie.voteAverage > 0) {
                    RatingBadge(value = movie.voteAverage)
                }
            }

            Column(
                modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    movie.releaseDate.take(4).takeIf { it.length == 4 }?.let { year ->
                        MetadataChip(text = year)
                    }
                    movie.genres.firstOrNull()?.let { genre ->
                        MetadataChip(text = genre)
                    }
                }

                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                if (movie.overview.isNotBlank()) {
                    Text(
                        text = movie.overview,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = { onMovieClick(movie.id) },
                        shape = CircleShape,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(text = stringResource(R.string.view_details))
                    }

                    FilledTonalIconButton(
                        onClick = { onToggleFavorite(movie, !movie.isFavorite) },
                    ) {
                        Icon(
                            imageVector = if (movie.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription =
                                stringResource(
                                    if (movie.isFavorite) R.string.remove_favorite else R.string.favorite,
                                ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HeroMovieShimmer(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(8.dp)
                .heightIn(min = 300.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
                .shimmer(),
    )
}

@PreviewLightDark
@PreviewFontScale
@Composable
private fun HeroMovieBannerPreview() {
    MovieHubTheme {
        HeroMovieBanner(
            movie = previewMovie(),
            onMovieClick = {},
            onToggleFavorite = { _, _ -> },
        )
    }
}
