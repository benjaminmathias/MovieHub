package com.benjamin.moviehub.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
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
private val PosterWidth = 108.dp
// Hauteur du poster (ratio 2/3) : le bloc texte s'y aligne.
private val PosterHeight = 162.dp
private val SummaryOverlap = 32.dp

@Composable
fun MovieDetailContent(
    movie: Movie,
    credits: MovieCredits,
    listState: LazyListState = rememberLazyListState(),
) {
    // Insets edge-to-edge : le bottom système passe en contentPadding, pas en Modifier,
    // pour que le contenu scrolle derrière les barres sans être rogné.
    val navigationBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding =
            PaddingValues(
                bottom = 24.dp + navigationBottom,
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
                }
            }
        }

        if (movie.genres.isNotEmpty()) {
            item(key = "genres") {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding =
                        PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 12.dp,
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

        if (movie.overview.isNotBlank()) {
            item(key = "synopsis") {
                DetailSection(title = stringResource(R.string.synopsis)) {
                    // Colonne dédiée : sans elle, les deux enfants se superposeraient
                    // dans le Box de DetailSection (le bouton tombait dans le texte).
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        var expanded by rememberSaveable { mutableStateOf(false) }
                        var showToggle by rememberSaveable { mutableStateOf(false) }
                        Text(
                            text = movie.overview,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = if (expanded) Int.MAX_VALUE else 4,
                            overflow = TextOverflow.Ellipsis,
                            onTextLayout = { result ->
                                if (result.hasVisualOverflow && !showToggle) {
                                    showToggle = true
                                }
                            },
                        )
                        if (showToggle) {
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
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // Le bloc texte remplit la hauteur du poster : marges réparties
        // (SpaceBetween) quand toutes les lignes sont présentes, espacement
        // fixe élargi sinon pour éviter un trou béant s'il manque des lignes.
        val hasFullInfo = metadata.isNotEmpty() && movie.voteAverage > 0 && director != null
        Column(
            modifier =
                if (hasFullInfo) {
                    Modifier.weight(1f).height(PosterHeight)
                } else {
                    Modifier.weight(1f)
                },
            verticalArrangement =
                if (hasFullInfo) {
                    Arrangement.SpaceBetween
                } else {
                    Arrangement.spacedBy(8.dp)
                },
        ) {
            Text(
                text = movie.title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (metadata.isNotEmpty() || director != null) {
                MetaFlowRow(
                    metadata = metadata,
                    director = director,
                )
            }
            if (movie.voteAverage > 0) {
                MovieDetailRating(movie)
            }
        }

        Surface(
            modifier =
                Modifier
                    .width(PosterWidth)
                    .aspectRatio(2f / 3f)
                    .testTag("detail_poster"),
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 4.dp,
            shadowElevation = 8.dp,
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

// Bloc meta compact (année • durée • réalisateur) : un seul groupe visuel
// au lieu de deux lignes, chaque texte garde son nœud exact pour l'a11y.
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MetaFlowRow(
    metadata: List<String>,
    director: String?,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        if (metadata.isNotEmpty()) {
            Text(
                text = metadata.joinToString(" • "),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        director?.let {
            Text(
                text = stringResource(R.string.director_format, it),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun MovieDetailRating(movie: Movie) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = stringResource(R.string.rating_out_of_ten, movie.voteAverage),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.tertiary,
        )
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
            modifier = Modifier.padding(horizontal = horizontalPadding),
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
