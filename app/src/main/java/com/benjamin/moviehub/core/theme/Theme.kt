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
        primaryContainer = MovieHubPrimary,
        onPrimaryContainer = Color(0xFF00390F),
        secondary = MovieHubSecondary,
        onSecondary = Color(0xFF003549),
        tertiary = MovieHubRating,
        onTertiary = Color(0xFF502400),
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
        onPrimary = Color.White,
        primaryContainer = Color(0xFFB4F5C2),
        onPrimaryContainer = Color(0xFF002109),
        secondary = MovieHubLightSecondary,
        onSecondary = Color.White,
        tertiary = MovieHubLightRating,
        onTertiary = Color.White,
        background = MovieHubLightBackground,
        onBackground = MovieHubLightOnSurface,
        surface = MovieHubLightSurface,
        onSurface = MovieHubLightOnSurface,
        surfaceContainerLow = MovieHubLightSurfaceLow,
        surfaceContainer = MovieHubLightSurfaceContainer,
        surfaceContainerHigh = MovieHubLightSurfaceHigh,
        surfaceVariant = MovieHubLightSurfaceContainer,
        onSurfaceVariant = MovieHubLightOnSurfaceVariant,
        outline = MovieHubLightOutline,
        outlineVariant = MovieHubLightOutlineVariant,
    )

@Composable
fun MovieHubTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content,
    )
}
