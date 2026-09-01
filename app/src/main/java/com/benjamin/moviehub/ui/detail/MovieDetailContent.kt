package com.benjamin.moviehub.ui.detail

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.benjamin.moviehub.R
import com.benjamin.moviehub.domain.model.Actor
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.model.MovieCredits
import com.benjamin.moviehub.ui.components.ActorItem
import com.benjamin.moviehub.ui.components.MovieGenreTag
import java.text.NumberFormat
import java.util.Locale

@Composable
fun MovieDetailContent(
    movie: Movie,
    credits: MovieCredits,
    onToggleFavorite: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        val hasAdditionalInformation =
            credits.director?.isNotBlank() == true ||
                movie.originalLanguage?.isNotBlank() == true ||
                movie.status?.isNotBlank() == true ||
                movie.productionCountries.any(String::isNotBlank) ||
                movie.budget?.let { it > 0 } == true ||
                movie.revenue?.let { it > 0 } == true

        LazyColumn(
            modifier = Modifier.fillMaxWidth().widthIn(max = 900.dp).navigationBarsPadding(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item(key = "hero") {
                MovieDetailHero(backdropPath = movie.backdropPath)
            }

            item(key = "summary") {
                MovieDetailSummary(
                    movie = movie,
                        onToggleFavorite = onToggleFavorite,
                )
            }

            if (movie.voteAverage > 0 || movie.voteCount?.let { it > 0 } == true) {
                item(key = "rating") {
                    MovieDetailRating(movie)
                }
            }

            if (movie.overview.isNotBlank()) {
                item(key = "synopsis") {
                    DetailSection(title = stringResource(R.string.synopsis)) {
                        Text(
                            text = movie.overview,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (hasAdditionalInformation) {
                item(key = "information") {
                    MovieDetailInformation(movie = movie, director = credits.director)
                }
            }

            item(key = "cast") {
                DetailSection(title = stringResource(R.string.cast_principal)) {
                    if (credits.actors.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(bottom = 8.dp),
                        ) {
                            items(credits.actors, key = Actor::id) { actor ->
                                ActorItem(actor)
                            }
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.cast_unavailable),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MovieDetailHero(backdropPath: String?) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val heroHeight = (maxWidth * 0.56f).coerceIn(180.dp, 260.dp)

        AsyncImage(
            model = backdropPath?.takeIf(String::isNotBlank),
            placeholder = painterResource(R.drawable.ic_launcher_foreground),
            error = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().height(heroHeight),
            contentScale = ContentScale.Crop,
        )

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(heroHeight)
                    .background(
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.35f),
                                    MaterialTheme.colorScheme.background,
                                ),
                        ),
                    ),
        )
    }
}

@Composable
private fun MovieDetailSummary(
    movie: Movie,
    onToggleFavorite: () -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        val posterWidth = if (maxWidth >= 600.dp) 144.dp else 112.dp
        val originalTitle =
            movie.originalTitle
                ?.trim()
                ?.takeIf { it.isNotEmpty() && !it.equals(movie.title.trim(), ignoreCase = true) }
        val releaseYear = movie.releaseDate.take(4).takeIf { it.length == 4 }
        val metadata = buildList {
            releaseYear?.let(::add)
            movie.runtimeMinutes?.takeIf { it > 0 }?.let {
                add(stringResource(R.string.runtime_format, it / 60, it % 60))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            AsyncImage(
                model = movie.posterPath?.takeIf(String::isNotBlank),
                placeholder = painterResource(R.drawable.ic_launcher_foreground),
                error = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = stringResource(R.string.poster_description, movie.title),
                modifier = Modifier.width(posterWidth).aspectRatio(2f / 3f).clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop,
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = movie.title,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                        originalTitle?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(48.dp),
                    ) {
                        val favoriteScale by animateFloatAsState(
                            targetValue = if (movie.isFavorite) 1.2f else 1f,
                            animationSpec =
                                spring(
                                    dampingRatio = Spring.DampingRatioHighBouncy,
                                    stiffness = Spring.StiffnessMedium,
                                ),
                            label = "favorite_spring_anim",
                        )
                        Icon(
                            imageVector =
                                if (movie.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription =
                                stringResource(
                                    if (movie.isFavorite) R.string.remove_favorite else R.string.favorite,
                                ),
                            tint =
                                if (movie.isFavorite) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            modifier = Modifier.scale(favoriteScale),
                        )
                    }
                }

                if (metadata.isNotEmpty()) {
                    Text(
                        text = metadata.joinToString(" • "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (movie.genres.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(movie.genres) { genre -> MovieGenreTag(name = genre) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MovieDetailRating(movie: Movie) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(22.dp),
            )
            if (movie.voteAverage > 0) {
                Text(
                    text = stringResource(R.string.rating_out_of_ten, movie.voteAverage),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold,
                )
            }
            movie.voteCount?.takeIf { it > 0 }?.let {
                Text(
                    text = stringResource(R.string.vote_count_format, it),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MovieDetailInformation(
    movie: Movie,
    director: String?,
) {
    val originalLanguage = movie.originalLanguage?.trim()?.takeIf(String::isNotEmpty)?.uppercase(Locale.ROOT)
    val status = movie.status?.trim()?.takeIf(String::isNotEmpty)
    val countries = movie.productionCountries.map(String::trim).filter(String::isNotEmpty).distinct().joinToString(", ")
    val budget = movie.budget?.takeIf { it > 0 }?.let(::formatCurrency)
    val revenue = movie.revenue?.takeIf { it > 0 }?.let(::formatCurrency)

    DetailSection(title = stringResource(R.string.additional_information)) {
        director?.takeIf(String::isNotBlank)?.let {
            DetailInfoRow(stringResource(R.string.director), it)
        }
        originalLanguage?.let {
            DetailInfoRow(stringResource(R.string.original_language), it)
        }
        status?.let {
            DetailInfoRow(stringResource(R.string.movie_status), it)
        }
        countries.takeIf(String::isNotEmpty)?.let {
            DetailInfoRow(stringResource(R.string.production_countries), it)
        }
        budget?.let {
            DetailInfoRow(stringResource(R.string.budget), it)
        }
        revenue?.let {
            DetailInfoRow(stringResource(R.string.revenue), it)
        }
    }
}

@Composable
private fun DetailSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        content()
    }
}

@Composable
private fun DetailInfoRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.6f),
        )
    }
}

private fun formatCurrency(value: Long): String = NumberFormat.getCurrencyInstance(Locale.US).format(value)
