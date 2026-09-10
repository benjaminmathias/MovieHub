package com.benjamin.moviehub.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.benjamin.moviehub.R
import com.benjamin.moviehub.domain.model.Movie
import com.valentinilk.shimmer.shimmer

/** Fixed width used by the compact cards shown in the home category rows. */
val RowMovieItemWidth = 160.dp

@Composable
fun RowMovieItem(
    movie: Movie,
    onMovieClick: (Int) -> Unit,
    onToggleFavorite: (Movie, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val favoriteStateDescription =
        stringResource(
            if (movie.isFavorite) R.string.favorite_state else R.string.not_favorite_state,
        )

    Card(
        onClick = { onMovieClick(movie.id) },
        modifier =
            modifier
                .testTag("movie_item")
                .semantics {
                    stateDescription = favoriteStateDescription
                },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f)) {
                AsyncImage(
                    model = (movie.posterPathSmall?.takeIf(String::isNotBlank) ?: movie.posterPath),
                    placeholder = painterResource(R.drawable.ic_launcher_foreground),
                    error = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = stringResource(R.string.poster_description, movie.title),
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Crop,
                )

                IconButton(
                    onClick = { onToggleFavorite(movie, !movie.isFavorite) },
                    modifier = Modifier.align(Alignment.TopEnd).padding(2.dp),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (movie.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription =
                                stringResource(
                                    if (movie.isFavorite) R.string.remove_favorite else R.string.favorite,
                                ),
                            tint =
                                if (movie.isFavorite) {
                                    MaterialTheme.colorScheme.tertiary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    movie.releaseDate.take(4).takeIf { it.length == 4 }?.let { year ->
                        Text(
                            text = year,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    Box(modifier = Modifier.weight(1f))
                    if (movie.voteAverage > 0) {
                        MovieRating(
                            value = movie.voteAverage,
                            iconSize = 14.dp,
                            textStyle = MaterialTheme.typography.labelMedium,
                        )
                    }
                }

                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                val genres = movie.genres.take(2).joinToString(" • ")
                if (genres.isNotBlank()) {
                    Text(
                        text = genres,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
fun RowMovieShimmerItem(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.shimmer(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(2f / 3f)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Column(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 14.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp)),
                )
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth(0.8f)
                            .heightIn(min = 14.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp)),
                )
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth(0.5f)
                            .heightIn(min = 14.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp)),
                )
            }
        }
    }
}
