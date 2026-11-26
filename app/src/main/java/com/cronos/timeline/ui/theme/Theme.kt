package com.cronos.timeline.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CronosDarkColorScheme = darkColorScheme(
    primary = CyanChronos,
    onPrimary = ObsidianVoid,
    primaryContainer = CyanChronosDark,
    onPrimaryContainer = TextPrimary,
    secondary = AmberChronos,
    onSecondary = ObsidianVoid,
    secondaryContainer = AmberChronosDark,
    onSecondaryContainer = TextPrimary,
    tertiary = MintChronos,
    onTertiary = ObsidianVoid,
    background = ObsidianVoid,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = ObsidianVoid,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurfaceElevated,
    surfaceContainerHigh = ObsidianSurfaceHighest,
    outline = SlateBorder,
    outlineVariant = SlateGridLine,
    error = CoralChronos,
    onError = ObsidianVoid
)

@Composable
fun CronosSystemTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CronosDarkColorScheme,
        typography = CronosTypography,
        content = content
    )
}
