package com.homesajja.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Placeholder color schemes only — replaced with HomeSajja's full Material 3
// scheme (built from the warm/natural palette) in Phase 1. Dynamic color is
// intentionally not used, since the brand palette should not be overridden
// by the device wallpaper.
private val DarkColorScheme = darkColorScheme(
    primary = PlaceholderPrimaryDark
)

private val LightColorScheme = lightColorScheme(
    primary = PlaceholderPrimaryLight
)

@Composable
fun HomeSajjaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
