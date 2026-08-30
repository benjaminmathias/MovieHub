package com.benjamin.moviehub.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
    val posterWidth = if (compact) 64.dp else 80.dp
    val posterHeight = if (compact) 96.dp else 112.dp
    val rowHeight = posterHeight + 16.dp

    Card(
        modifier =
            Modifier
                .testTag("movie_item")
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clickable { onMovieClick(movie.id) },
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = rowHeight)
                    .padding(8.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(width = posterWidth, height = posterHeight)
                        .clip(RoundedCornerShape(4.dp)),
            ) {
                AsyncImage(
                    model = movie.posterPath,
                    // placeholder = painterResource(R.drawable.placeholder_loading),
                    // error = painterResource(R.drawable.placeholder_error),
                    contentDescription = stringResource(R.string.poster_description, movie.title),
                    modifier =
                        Modifier
                            .width(posterWidth)
                            .fillMaxHeight(),
                    contentScale = ContentScale.Crop,
                )
            }
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .heightIn(min = posterHeight)
                        .padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                val releaseYear = movie.releaseDate.take(4)
                Text(
                    text = releaseYear.ifBlank { stringResource(R.string.not_available) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.tertiary,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "%.1f".format(movie.voteAverage),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }

            Box(
                modifier = Modifier.width(48.dp).fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (movie.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    tint = if (movie.isFavorite) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(24.dp),
                )
            }
        }
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
