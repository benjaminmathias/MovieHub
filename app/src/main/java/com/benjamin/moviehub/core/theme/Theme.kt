package com.benjamin.moviehub.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// --- Color tokens ---------------------------------------------------------------

private val MovieHubBackground = Color(0xFF0F1413)
private val MovieHubSurface = Color(0xFF141A18)
private val MovieHubSurfaceLow = Color(0xFF1A211F)
private val MovieHubSurfaceContainer = Color(0xFF202A27)
private val MovieHubSurfaceHigh = Color(0xFF293630)
private val MovieHubPrimary = Color(0xFFA8EFAF)
private val MovieHubSecondary = Color(0xFFA9CBD0)
private val MovieHubRating = Color(0xFFFFC27D)
private val MovieHubOnSurface = Color(0xFFEDF5F0)
private val MovieHubOnSurfaceVariant = Color(0xFFB4C2BA)
private val MovieHubOutline = Color(0xFF7D8E84)
private val MovieHubOutlineVariant = Color(0xFF3B4942)

private val MovieHubLightBackground = Color(0xFFF7FBF8)
private val MovieHubLightSurface = Color(0xFFF0F6F1)
private val MovieHubLightSurfaceLow = Color(0xFFE8F0EA)
private val MovieHubLightSurfaceContainer = Color(0xFFDEE9E0)
private val MovieHubLightSurfaceHigh = Color(0xFFD3E0D5)
private val MovieHubLightPrimary = Color(0xFF266A37)
private val MovieHubLightSecondary = Color(0xFF2F6670)
private val MovieHubLightRating = Color(0xFF8A4900)
private val MovieHubLightOnSurface = Color(0xFF17201A)
private val MovieHubLightOnSurfaceVariant = Color(0xFF465449)
private val MovieHubLightOutline = Color(0xFF6E7D71)
private val MovieHubLightOutlineVariant = Color(0xFFC2CEC4)

private val DarkColorScheme =
    darkColorScheme(
        primary = MovieHubPrimary,
        onPrimary = Color(0xFF00390F),
        primaryContainer = MovieHubPrimary,
        onPrimaryContainer = Color(0xFF00390F),
        secondary = MovieHubSecondary,
        onSecondary = Color(0xFF003549),
        secondaryContainer = MovieHubSurfaceHigh,
        onSecondaryContainer = MovieHubOnSurface,
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
        secondaryContainer = MovieHubLightSurfaceHigh,
        onSecondaryContainer = MovieHubLightOnSurface,
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

// --- Layout tokens --------------------------------------------------------------

internal val ContentHorizontalPadding = 16.dp
internal const val POSTER_ASPECT_RATIO = 2f / 3f
internal val MovieGridMinCellSize = 144.dp
internal val HomeMovieCardWidth = 128.dp

// --- Typography -----------------------------------------------------------------

private val Typography =
    Typography(
        headlineLarge =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                lineHeight = 36.sp,
            ),
        headlineMedium =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                lineHeight = 32.sp,
            ),
        titleLarge =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                lineHeight = 28.sp,
            ),
        titleMedium =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            ),
        bodyLarge =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
        labelLarge =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp,
            ),
        labelMedium =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            ),
        labelSmall =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.5.sp,
            ),
    )

private val MovieHubShapes =
    Shapes(
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(24.dp),
        extraLarge = RoundedCornerShape(28.dp),
    )

@Composable
fun MovieHubTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        shapes = MovieHubShapes,
        content = content,
    )
}
