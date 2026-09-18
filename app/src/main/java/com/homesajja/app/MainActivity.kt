package com.homesajja.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.homesajja.app.di.LocalAppContainer
import com.homesajja.app.navigation.HomeSajjaNavHost
import com.homesajja.app.ui.theme.HomeSajjaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as HomeSajjaApp).container
        setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                HomeSajjaTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        HomeSajjaNavHost()
                    }
                }
            }
        }
    }
}
