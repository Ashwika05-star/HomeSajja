package com.homesajja.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.homesajja.app.di.LocalAppContainer
import com.homesajja.app.navigation.HomeSajjaNavHost
import com.homesajja.app.notification.NotificationRouting
import com.homesajja.app.ui.theme.HomeSajjaTheme
import com.homesajja.app.ui.theme.ThemeMode
import com.homesajja.app.ui.theme.resolveDark

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Content draws behind transparent system bars; each screen pads itself with window insets (Scaffold does it by itself).
        // Until the theme is known the bar icons follow the phone's setting; they are re-set below once the choice is read.
        enableEdgeToEdge()
        val container = (application as HomeSajjaApp).container
        // Opened from a tapped system notification: remember where it wanted to go.
        NotificationRouting.routeFrom(intent)?.let { container.pendingRoute.value = it }
        setContent {
            // null until the saved Light / Dark / System choice has been read, so a stored choice never flashes the wrong theme.
            val mode by container.themePreference.modeFlow.collectAsState(initial = null as ThemeMode?)
            val systemDark = isSystemInDarkTheme()
            val darkTheme = (mode ?: ThemeMode.SYSTEM).resolveDark(systemDark)

            // Status and navigation bar icons follow the app's theme, which can differ from the phone's.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(LIGHT_NAV_SCRIM, DARK_NAV_SCRIM) { darkTheme },
                )
                onDispose {}
            }

            CompositionLocalProvider(LocalAppContainer provides container) {
                HomeSajjaTheme(darkTheme = darkTheme) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        if (mode != null) HomeSajjaNavHost()
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        NotificationRouting.routeFrom(intent)?.let { (application as HomeSajjaApp).container.pendingRoute.value = it }
    }

    private companion object {
        // The scrims androidx.activity uses for the three-button navigation bar.
        val LIGHT_NAV_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val DARK_NAV_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
