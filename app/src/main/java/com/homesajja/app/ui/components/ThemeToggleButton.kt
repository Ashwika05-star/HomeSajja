package com.homesajja.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.homesajja.app.di.LocalAppContainer
import com.homesajja.app.ui.theme.LocalDarkTheme
import com.homesajja.app.ui.theme.ThemeMode
import kotlinx.coroutines.launch

/**
 * One tap to switch between the light and dark theme: a sun while the app is dark (tap for light), a moon while it is light
 * (tap for dark). The pick is saved like the Appearance card's, so it is remembered across restarts. The Appearance card on
 * Profile still offers System as well.
 */
@Composable
fun ThemeToggleButton(modifier: Modifier = Modifier) {
    val preference = LocalAppContainer.current.themePreference
    val isDark = LocalDarkTheme.current
    val scope = rememberCoroutineScope()
    IconButton(
        onClick = { scope.launch { preference.setMode(if (isDark) ThemeMode.LIGHT else ThemeMode.DARK) } },
        modifier = modifier,
    ) {
        Icon(
            imageVector = if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
            contentDescription = if (isDark) "Switch to light mode" else "Switch to dark mode",
        )
    }
}
