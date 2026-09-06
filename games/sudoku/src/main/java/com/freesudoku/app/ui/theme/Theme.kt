package com.freesudoku.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.freesudoku.app.data.settings.ThemeMode

private val ApexDarkColorScheme = darkColorScheme(
    primary = ApexPrimary,
    onPrimary = ApexOnPrimary,
    primaryContainer = ApexPrimaryContainer,
    onPrimaryContainer = ApexOnPrimaryContainer,
    inversePrimary = ApexInversePrimary,
    secondary = ApexSecondary,
    onSecondary = ApexOnSecondary,
    secondaryContainer = ApexSecondaryContainer,
    onSecondaryContainer = ApexOnSecondaryContainer,
    tertiary = ApexTertiary,
    onTertiary = ApexOnTertiary,
    tertiaryContainer = ApexTertiaryContainer,
    onTertiaryContainer = ApexOnTertiaryContainer,
    background = ApexBackground,
    onBackground = ApexOnBackground,
    surface = ApexSurface,
    onSurface = ApexOnSurface,
    surfaceVariant = ApexSurfaceVariant,
    onSurfaceVariant = ApexOnSurfaceVariant,
    surfaceDim = ApexSurfaceDim,
    surfaceBright = ApexSurfaceBright,
    surfaceContainerLowest = ApexSurfaceContainerLowest,
    surfaceContainerLow = ApexSurfaceContainerLow,
    surfaceContainer = ApexSurfaceContainer,
    surfaceContainerHigh = ApexSurfaceContainerHigh,
    surfaceContainerHighest = ApexSurfaceContainerHighest,
    outline = ApexOutline,
    outlineVariant = ApexOutlineVariant,
    inverseSurface = ApexInverseSurface,
    inverseOnSurface = ApexInverseOnSurface,
    error = ApexError,
    onError = ApexOnError,
    errorContainer = ApexErrorContainer,
    onErrorContainer = ApexOnErrorContainer,
)

private val ApexLightColorScheme = lightColorScheme(
    primary = ApexLightPrimary,
    onPrimary = ApexLightOnPrimary,
    primaryContainer = ApexLightPrimaryContainer,
    onPrimaryContainer = ApexLightOnPrimaryContainer,
    inversePrimary = ApexLightInversePrimary,
    secondary = ApexLightSecondary,
    onSecondary = ApexLightOnSecondary,
    secondaryContainer = ApexLightSecondaryContainer,
    onSecondaryContainer = ApexLightOnSecondaryContainer,
    tertiary = ApexLightTertiary,
    onTertiary = ApexLightOnTertiary,
    tertiaryContainer = ApexLightTertiaryContainer,
    onTertiaryContainer = ApexLightOnTertiaryContainer,
    background = ApexLightBackground,
    onBackground = ApexLightOnBackground,
    surface = ApexLightSurface,
    onSurface = ApexLightOnSurface,
    surfaceVariant = ApexLightSurfaceVariant,
    onSurfaceVariant = ApexLightOnSurfaceVariant,
    surfaceDim = ApexLightSurfaceDim,
    surfaceBright = ApexLightSurfaceBright,
    surfaceContainerLowest = ApexLightSurfaceContainerLowest,
    surfaceContainerLow = ApexLightSurfaceContainerLow,
    surfaceContainer = ApexLightSurfaceContainer,
    surfaceContainerHigh = ApexLightSurfaceContainerHigh,
    surfaceContainerHighest = ApexLightSurfaceContainerHighest,
    outline = ApexLightOutline,
    outlineVariant = ApexLightOutlineVariant,
    inverseSurface = ApexLightInverseSurface,
    inverseOnSurface = ApexLightInverseOnSurface,
    error = ApexLightError,
    onError = ApexLightOnError,
    errorContainer = ApexLightErrorContainer,
    onErrorContainer = ApexLightOnErrorContainer,
)

/** "Soft Technical" shape scale: milled-hardware radii, no pill shapes (aside from Switch). */
private val ApexShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(10.dp),
)

@Composable
fun FreeSudokuTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    // No dynamic (wallpaper-derived) color: the palette is a deliberately restrained, hand-picked
    // set of accents, not ambient decoration ("Zero-Glow Discipline" / "Restrained Focus").
    val colorScheme = if (darkTheme) ApexDarkColorScheme else ApexLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = ApexShapes,
        content = content,
    )
}
