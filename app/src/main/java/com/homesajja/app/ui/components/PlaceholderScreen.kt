package com.homesajja.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Temporary stand-in used by every route until each screen gets its real UI
 * in a later phase. Confirms Compose + Navigation are wired correctly.
 */
@Composable
fun PlaceholderScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "HomeSajja", style = MaterialTheme.typography.headlineMedium)
    }
}
