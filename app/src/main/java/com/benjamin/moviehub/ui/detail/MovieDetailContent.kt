package com.benjamin.moviehub.ui.detail

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.benjamin.moviehub.R
import com.benjamin.moviehub.domain.model.Actor
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.model.MovieCredits
import com.benjamin.moviehub.ui.components.ActorItem
import com.benjamin.moviehub.ui.components.MovieGenreTag

private val HeroHeight = 264.dp
private val PosterWidth = 120.dp
// Hauteur du poster (ratio 2/3) : le bloc texte s'y aligne.
private val PosterHeight = 180.dp
private val SummaryOverlap = 32.dp

@Composable
fun MovieDetailContent(
    movie: Movie,
    credits: MovieCredits,
    listState: LazyListState = rememberLazyListState(),
    onToggleFavorite: (() -> Unit)? = null,
    onOpenTmdb: (() -> Unit)? = null,
) {
    // Insets edge-to-edge : le bottom système passe en contentPadding, pas en Modifier,
    // pour que le contenu scrolle derrière les barres sans être rogné.
    val navigationInsets = WindowInsets.navigationBars.asPaddingValues()
    val layoutDirection = LocalLayoutDirection.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding =
            PaddingValues(
                start = navigationInsets.calculateLeftPadding(layoutDirection),
                end = navigationInsets.calculateRightPadding(layoutDirection),
                bottom = 24.dp + navigationInsets.calculateBottomPadding(),
            ),
    ) {
        item(key = "header") {
            // Le bloc résumé chevauche légèrement le hero : pas d'espace mort,
            // le fondu bas du hero assure la lisibilité sur la zone de recouvrement.
            Box(modifier = Modifier.fillMaxWidth()) {
                MovieDetailHero(backdropPath = movie.backdropPath)
                Column(modifier = Modifier.padding(top = HeroHeight - SummaryOverlap)) {
                    MovieDetailSummary(
                        movie = movie,
                        director = credits.director?.takeIf { it.isNotBlank() },
                    )
                    if (onToggleFavorite != null || onOpenTmdb != null) {
                        MovieDetailActions(
                            movie = movie,
                            onToggleFavorite = onToggleFavorite,
                            onOpenTmdb = onOpenTmdb,
                        )
                    }
                }
            }
        }

        if (movie.genres.isNotEmpty()) {
            item(key = "genres") {
                DetailSection(
                    title = stringResource(R.string.genres),
                    fullBleed = true,
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding =
                            PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                bottom = 4.dp,
                            ),
                    ) {
                        itemsIndexed(
                            items = movie.genres,
                            key = { index, genre -> "genre-$index-$genre" },
                        ) { _, genre ->
                            MovieGenreTag(name = genre)
                        }
                    }
                }
            }
        }

        if (movie.overview.isNotBlank()) {
            item(key = "synopsis") {
                DetailSection(title = stringResource(R.string.synopsis)) {
                    // Colonne dédiée : sans elle, les deux enfants se superposeraient
                    // dans le Box de DetailSection (le bouton tombait dans le texte).
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        var expanded by rememberSaveable(movie.id, movie.overview) {
                            mutableStateOf(false)
                        }
                        var hasVisualOverflow by remember(movie.id, movie.overview) {
                            mutableStateOf(false)
                        }
                        Text(
                            text = movie.overview,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = if (expanded) Int.MAX_VALUE else 4,
                            overflow = TextOverflow.Ellipsis,
                            onTextLayout = { result ->
                                if (!expanded && hasVisualOverflow != result.hasVisualOverflow) {
                                    hasVisualOverflow = result.hasVisualOverflow
                                }
                            },
                        )
                        if (expanded || hasVisualOverflow) {
                            TextButton(
                                onClick = { expanded = !expanded },
                                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    text =
                                        stringResource(
                                            if (expanded) R.string.show_less else R.string.read_more,
                                        ),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        }

        if (credits.actors.isNotEmpty()) {
            item(key = "cast") {
                DetailSection(
                    title = stringResource(R.string.cast_principal),
                    fullBleed = true,
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    ) {
                        items(credits.actors, key = Actor::id) { actor ->
                            ActorItem(actor = actor)
                        }
                    }
                }
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
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .testTag("detail_hero"),
    ) {
        // Placeholder sous l'image : visible pendant le chargement Coil
        // ou si aucun backdrop n'est disponible.
        Icon(
            imageVector = Icons.Filled.Movie,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.align(Alignment.Center).size(48.dp),
        )
        AsyncImage(
            model = backdropPath?.takeIf(String::isNotBlank),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center,
        )

        // Scrim haut : lisibilité des boutons toolbar sur image claire.
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    Color.Black.copy(alpha = 0.32f),
                                    Color.Transparent,
                                ),
                        ),
                    ),
        )

        // Fondu bas vers la surface pour une transition hero -> contenu.
        // Assez haut (140dp) pour couvrir la zone de chevauchement du résumé.
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.surface,
                                ),
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
                // Textes à gauche alignés à 16dp comme le reste du contenu,
                // poster à droite : les deux partent du même top, sur la même ligne.
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 0.dp),
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
                if (metadata.isNotEmpty() || director != null) {
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
            modifier =
                Modifier
                    .width(PosterWidth)
                    .aspectRatio(2f / 3f)
                    .testTag("detail_poster"),
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 2.dp,
            shadowElevation = 4.dp,
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = movie.posterPath?.takeIf(String::isNotBlank),
                    contentDescription =
                        movie.posterPath
                            ?.takeIf(String::isNotBlank)
                            ?.let { stringResource(R.string.poster_description, movie.title) },
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MovieDetailActions(
    movie: Movie,
    onToggleFavorite: (() -> Unit)?,
    onOpenTmdb: (() -> Unit)?,
) {
    val favoriteLabel =
        stringResource(
            if (movie.isFavorite) R.string.remove_favorite else R.string.favorite,
        )
    val favoriteScale by animateFloatAsState(
        targetValue = if (movie.isFavorite) 1.25f else 1f,
        animationSpec =
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
        label = "favoritePop",
    )
    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    ) {
        val stackActions = LocalDensity.current.fontScale >= 1.3f || maxWidth < 360.dp

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            maxItemsInEachRow = if (stackActions) 1 else 2,
        ) {
            onToggleFavorite?.let { toggleFavorite ->
                Button(
                    onClick = toggleFavorite,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("detail_favorite")
                            .semantics { this.contentDescription = favoriteLabel },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = if (movie.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                            modifier =
                                Modifier
                                    .size(20.dp)
                                    .graphicsLayer {
                                        scaleX = favoriteScale
                                        scaleY = favoriteScale
                                    },
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = favoriteLabel,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            onOpenTmdb?.let { openTmdb ->
                OutlinedButton(
                    onClick = openTmdb,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    modifier = Modifier.weight(1f).height(52.dp).testTag("detail_tmdb"),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.open_tmdb),
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
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
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            movie.voteCount?.takeIf { it > 0 }?.let { voteCount ->
                Text(
                    text = stringResource(R.string.vote_count, voteCount),
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

@Composable
private fun DetailSection(
    title: String,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 16.dp,
    fullBleed: Boolean = false,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier =
                Modifier
                    .padding(horizontal = horizontalPadding)
                    .semantics { heading() },
        )
        if (fullBleed) {
            content()
        } else {
            Box(modifier = Modifier.padding(horizontal = horizontalPadding)) {
                content()
            }
        }
    }
}
