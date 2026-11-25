package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val CallidusDarkColorScheme = darkColorScheme(
    primary = CallidusCyan,
    onPrimary = ObsidianVoid,
    primaryContainer = ObsidianPanelElevated,
    onPrimaryContainer = TextPrimary,
    secondary = CallidusGold,
    onSecondary = ObsidianVoid,
    secondaryContainer = ObsidianPanel,
    onSecondaryContainer = TextPrimary,
    tertiary = CallidusCrimson,
    onTertiary = TextPrimary,
    background = ObsidianVoid,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianPanel,
    onSurfaceVariant = TextSecondary,
    outline = ObsidianBorder,
    error = CallidusCrimson,
    onError = TextPrimary
)

val CallidusShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun CallidusTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CallidusDarkColorScheme,
        typography = CallidusTypography,
        shapes = CallidusShapes,
        content = content
    )
}
