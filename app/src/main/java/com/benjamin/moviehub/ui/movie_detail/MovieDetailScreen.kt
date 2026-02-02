package com.benjamin.moviehub.ui.movie_detail

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.benjamin.moviehub.R
import com.benjamin.moviehub.core.util.shareMovie
import com.benjamin.moviehub.domain.model.Movie

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailScreen(
    uiState: MovieDetailUiState,
    onBackClick: () -> Unit,
    onToggleFavorite: (Movie) -> Unit
) {

    val context = LocalContext.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                {
                    Text(
                        text = stringResource(R.string.details)
                    )
                },
                navigationIcon = {
                    IconButton(onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    if (uiState is MovieDetailUiState.Success) {
                        IconButton(onClick = {
                            shareMovie(context, uiState.movie)
                        }) {
                            Icon(Icons.Outlined.Share, contentDescription = "Partager")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState is MovieDetailUiState.Success) {
                val movie = uiState.movie
                FloatingActionButton(
                    onClick = { onToggleFavorite(movie) },
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    val scale by animateFloatAsState(
                        targetValue = if (uiState.movie.isFavorite) 1.2f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "favoriteScale"
                    )

                    Icon(
                        imageVector = if (movie.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = stringResource(R.string.favorite),
                        tint = if (movie.isFavorite) Color.Red else Color.Gray,
                        modifier = Modifier.scale(scale)
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState) {
                is MovieDetailUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Chargement des détails...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }
                    // CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is MovieDetailUiState.Success -> {
                    MovieDetailContent(
                        movie = uiState.movie,
                        actors = uiState.actors
                    )
                }

                is MovieDetailUiState.Error -> {
                    Text(
                        text = uiState.errorMessage.asString(),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }

    }
}
