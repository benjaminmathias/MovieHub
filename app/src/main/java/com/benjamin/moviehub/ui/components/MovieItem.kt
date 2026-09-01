package com.benjamin.moviehub.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.benjamin.moviehub.R
import com.benjamin.moviehub.domain.model.Movie

@Composable
fun MovieItem(
    movie: Movie,
    onMovieClick: (Int) -> Unit,
    compact: Boolean = false,
) {
    if (compact) {
        CompactMovieItem(movie, onMovieClick)
    } else {
        PosterMovieItem(movie, onMovieClick)
    }
}

@Composable
private fun PosterMovieItem(
    movie: Movie,
    onMovieClick: (Int) -> Unit,
) {
    Card(
        modifier =
            Modifier
                .testTag("movie_item")
                .fillMaxWidth()
                .clickable { onMovieClick(movie.id) },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column {
            Box(
                modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(MaterialTheme.shapes.medium),
            ) {
                AsyncImage(
                    model = movie.posterPath?.takeIf(String::isNotBlank),
                    placeholder = androidx.compose.ui.res.painterResource(R.drawable.ic_launcher_foreground),
                    error = androidx.compose.ui.res.painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = stringResource(R.string.poster_description, movie.title),
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                    contentScale = ContentScale.Crop,
                )

                if (movie.isFavorite) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(8.dp).size(20.dp),
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                MovieMetadata(movie)
            }
        }
    }
}

@Composable
private fun CompactMovieItem(
    movie: Movie,
    onMovieClick: (Int) -> Unit,
) {
    val posterWidth = 64.dp
    val posterHeight = 96.dp

    Card(
        modifier =
            Modifier
                .testTag("movie_item")
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clickable { onMovieClick(movie.id) },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = posterHeight + 16.dp).padding(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            AsyncImage(
                model = movie.posterPath?.takeIf(String::isNotBlank),
                placeholder = androidx.compose.ui.res.painterResource(R.drawable.ic_launcher_foreground),
                error = androidx.compose.ui.res.painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = stringResource(R.string.poster_description, movie.title),
                modifier = Modifier.size(width = posterWidth, height = posterHeight).clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop,
            )

            Column(
                modifier = Modifier.weight(1f).padding(start = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                MovieMetadata(movie)
            }

            Icon(
                imageVector = if (movie.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = null,
                tint =
                    if (movie.isFavorite) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                modifier = Modifier.padding(8.dp).size(24.dp),
            )
        }
    }
}

@Composable
private fun MovieMetadata(movie: Movie) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        movie.releaseDate.take(4).takeIf { it.length == 4 }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        if (movie.voteAverage > 0) {
            MovieRating(movie.voteAverage)
        }
    }
}

@Composable
private fun MovieRating(value: Double) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.tertiary,
        )
        Text(
            text = stringResource(R.string.rating_value, value),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.tertiary,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MovieItemPreview() {
    val fakeMovie =
        Movie(
            id = 1,
            title = "Avatar : De feu et de cendres",
            posterPath = "",
            voteAverage = 7.3,
            releaseDate = "2025-12-20",
            isFavorite = true,
            overview = "Test",
            backdropPath = "",
            webUrl = "",
            genreIds = emptyList(),
            genres = emptyList(),
        )

    MovieItem(
        movie = fakeMovie,
        onMovieClick = {},
    )
}
