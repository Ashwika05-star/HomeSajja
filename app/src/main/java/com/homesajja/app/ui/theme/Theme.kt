package com.homesajja.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Light theme only for now — dark theme is a later phase.
private val HomeSajjaLightColorScheme = lightColorScheme(
    primary = CabernetPrimary,
    onPrimary = OnCabernetPrimary,
    primaryContainer = CabernetPrimaryContainer,
    onPrimaryContainer = OnCabernetPrimaryContainer,
    secondary = PoignantPinkSecondary,
    onSecondary = OnPoignantPinkSecondary,
    secondaryContainer = PoignantPinkSecondaryContainer,
    onSecondaryContainer = OnPoignantPinkSecondaryContainer,
    tertiary = RoseTertiary,
    onTertiary = OnRoseTertiary,
    tertiaryContainer = RoseTertiaryContainer,
    onTertiaryContainer = OnRoseTertiaryContainer,
    background = PearlBackground,
    onBackground = OnPearlBackground,
    surface = PearlSurface,
    onSurface = OnPearlSurface,
    surfaceVariant = PearlSurfaceVariant,
    onSurfaceVariant = OnPearlSurfaceVariant,
    error = ErrorRed,
    onError = OnErrorRed,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    outline = OutlineNeutral,
    outlineVariant = OutlineVariantNeutral,
)

@Composable
fun HomeSajjaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HomeSajjaLightColorScheme,
        typography = Typography,
        content = content,
    )
}
