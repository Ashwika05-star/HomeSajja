package com.homesajja.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.homesajja.app.navigation.HomeSajjaNavHost
import com.homesajja.app.ui.theme.HomeSajjaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HomeSajjaTheme {
                HomeSajjaNavHost()
            }
        }
    }
}
