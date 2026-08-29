package com.benjamin.moviehub.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
    darkColorScheme(
        primary = MovieHubPrimary,
        onPrimary = Color(0xFF00390F),
        secondary = MovieHubSecondary,
        tertiary = MovieHubRating,
        background = MovieHubBackground,
        onBackground = MovieHubOnSurface,
        surface = MovieHubSurface,
        surfaceContainerLow = MovieHubSurfaceLow,
        surfaceContainer = MovieHubSurfaceContainer,
        surfaceContainerHigh = MovieHubSurfaceHigh,
        onSurface = MovieHubOnSurface,
        surfaceVariant = MovieHubSurfaceLow,
        onSurfaceVariant = MovieHubOnSurfaceVariant,
        outline = MovieHubOutline,
        outlineVariant = MovieHubOutlineVariant,
    )

private val LightColorScheme =
    lightColorScheme(
        primary = MovieHubLightPrimary,
        secondary = MovieHubLightSecondary,
        tertiary = MovieHubRating,
        background = MovieHubLightBackground,
        surface = MovieHubLightSurface,
        surfaceContainerLow = Color(0xFFEAF0ED),
        surfaceContainer = Color(0xFFE5EBE8),
        surfaceContainerHigh = Color(0xFFDDE5E1),
        surfaceVariant = Color(0xFFE5EBE8),
        outline = Color(0xFF68736F),
        outlineVariant = Color(0xFFC5CEC9),
    )

@Composable
fun MovieHubTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
