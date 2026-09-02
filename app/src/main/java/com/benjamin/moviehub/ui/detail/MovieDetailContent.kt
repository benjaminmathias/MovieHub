package com.benjamin.moviehub.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.benjamin.moviehub.R
import com.benjamin.moviehub.domain.model.Actor
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.model.MovieCredits
import com.benjamin.moviehub.ui.components.ActorItem

@Composable
fun MovieDetailContent(
    movie: Movie,
    credits: MovieCredits,
    onToggleFavorite: () -> Unit,
    listState: LazyListState = rememberLazyListState(),
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
            state = listState,
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            item(key = "header") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    MovieDetailHero(
                        backdropPath = movie.backdropPath,
                        posterPath = movie.posterPath,
                        title = movie.title,
                    )
                    MovieDetailSummary(movie = movie)
                }
            }

            if (movie.overview.isNotBlank()) {
                item(key = "synopsis") {
                    DetailSection(title = stringResource(R.string.synopsis)) {
                        Text(
                            text = movie.overview,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            credits.director?.trim()?.takeIf(String::isNotEmpty)?.let { director ->
                item(key = "director") {
                    Text(
                        text = stringResource(R.string.director_format, director),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }

            if (credits.actors.isNotEmpty()) {
                item(key = "cast") {
                    DetailSection(title = stringResource(R.string.cast_principal)) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 4.dp),
                        ) {
                            items(credits.actors, key = Actor::id) { actor ->
                                ActorItem(actor)
                            }
                        }
                    }
                }
            }

            if (movie.genres.isNotEmpty()) {
                item(key = "genres") {
                    DetailSection(title = stringResource(R.string.genres)) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 4.dp),
                        ) {
                            itemsIndexed(
                                items = movie.genres,
                                key = { index, genre -> "genre-$index-$genre" },
                            ) { _, genre ->
                                Text(
                                    text = genre,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MovieDetailHero(
    backdropPath: String?,
    posterPath: String?,
    title: String,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .testTag("detail_hero"),
    ) {
        AsyncImage(
            model = backdropPath?.takeIf(String::isNotBlank),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center,
        )

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.38f)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
                                ),
                        ),
                    ),
        )

        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp)
                    .offset(y = 72.dp)
                    .width(96.dp)
                    .aspectRatio(2f / 3f)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .testTag("detail_poster"),
        ) {
            AsyncImage(
                model = posterPath?.takeIf(String::isNotBlank),
                contentDescription =
                    posterPath
                        ?.takeIf(String::isNotBlank)
                        ?.let { stringResource(R.string.poster_description, title) },
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun MovieDetailSummary(
    movie: Movie,
) {
    val metadata = buildList {
        movie.releaseDate.take(4).takeIf { it.length == 4 }?.let(::add)
        movie.runtimeMinutes?.takeIf { it > 0 }?.let {
            add(stringResource(R.string.runtime_format, it / 60, it % 60))
        }
    }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = movie.title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (metadata.isNotEmpty()) {
                Text(
                    text = metadata.joinToString(" • "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (movie.voteAverage > 0) {
                MovieDetailRating(movie)
            }
        }

        Spacer(modifier = Modifier.width(96.dp))
    }
}

@Composable
private fun MovieDetailRating(movie: Movie) {
    Row(
        modifier = Modifier.padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = stringResource(R.string.rating_out_of_ten, movie.voteAverage),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.tertiary,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun DetailSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        content()
    }
}
