package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val RadarDarkColorScheme = darkColorScheme(
    primary = RadarCyan,
    onPrimary = OnRadarCyan,
    primaryContainer = RadarCyanContainer,
    onPrimaryContainer = RadarCyan,
    secondary = SignalGreen,
    onSecondary = OnSignalGreen,
    secondaryContainer = SignalGreenContainer,
    onSecondaryContainer = SignalGreen,
    tertiary = SignalYellow,
    onTertiary = OnSignalYellow,
    tertiaryContainer = SignalYellowContainer,
    onTertiaryContainer = SignalYellow,
    error = SignalRed,
    onError = OnSignalRed,
    errorContainer = SignalRedContainer,
    onErrorContainer = SignalRed,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = CockpitSurface,
    onSurface = TextPrimary,
    surfaceVariant = CockpitSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = ObsidianBg,
    surfaceContainerLow = CockpitSurface,
    surfaceContainer = CockpitSurfaceElevated,
    surfaceContainerHigh = CockpitSurfaceBright,
    surfaceContainerHighest = CockpitSurfaceBright,
    outline = CockpitBorder,
    outlineVariant = CockpitBorder
)

val RadarShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun RadarDriverTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = RadarDarkColorScheme,
        typography = RadarTypography,
        shapes = RadarShapes,
        content = content
    )
}
