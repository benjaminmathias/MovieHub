package com.benjamin.moviehub.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage

@Composable
internal fun MoviePosterArtwork(
    model: String?,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
) {
    MovieArtwork(
        model = model,
        contentScale = ContentScale.Crop,
        modifier = modifier.clip(shape),
    )
}

@Composable
internal fun MovieBackdropArtwork(
    model: String?,
    modifier: Modifier = Modifier,
) {
    MovieArtwork(
        model = model,
        contentScale = ContentScale.Crop,
        modifier = modifier,
    )
}

@Composable
private fun MovieArtwork(
    model: String?,
    contentScale: ContentScale,
    modifier: Modifier,
) {
    val normalizedModel = model?.takeIf(String::isNotBlank)
    var loadState by
        remember(normalizedModel) {
            mutableStateOf(
                if (normalizedModel == null) {
                    MovieArtworkLoadState.EMPTY
                } else {
                    MovieArtworkLoadState.LOADING
                },
            )
        }

    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (loadState != MovieArtworkLoadState.SUCCESS) {
            Icon(
                imageVector = Icons.Filled.Movie,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.fillMaxSize(0.28f),
            )
        }
        normalizedModel?.let { imageModel ->
            AsyncImage(
                model = imageModel,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                onLoading = { loadState = MovieArtworkLoadState.LOADING },
                onSuccess = { loadState = MovieArtworkLoadState.SUCCESS },
                onError = { loadState = MovieArtworkLoadState.ERROR },
            )
        }
    }
}

private enum class MovieArtworkLoadState {
    EMPTY,
    LOADING,
    SUCCESS,
    ERROR,
}
