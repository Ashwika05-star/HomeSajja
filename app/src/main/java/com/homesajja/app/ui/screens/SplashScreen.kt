package com.homesajja.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.homesajja.app.di.LocalAppContainer
import com.homesajja.app.di.ViewModelFactory
import com.homesajja.app.viewmodel.SplashDestination
import com.homesajja.app.viewmodel.SplashViewModel
import kotlinx.coroutines.delay

private const val SPLASH_ANIM_MILLIS = 500
private const val SPLASH_HOLD_MILLIS = 1200L

@Composable
fun SplashScreen(onNavigate: (SplashDestination) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: SplashViewModel = viewModel(factory = ViewModelFactory(container))
    val destination by viewModel.destination.collectAsState()

    var visible by remember { mutableStateOf(false) }
    var minHoldElapsed by remember { mutableStateOf(false) }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(SPLASH_ANIM_MILLIS),
        label = "splashAlpha",
    )
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.85f,
        animationSpec = tween(SPLASH_ANIM_MILLIS),
        label = "splashScale",
    )

    LaunchedEffect(Unit) {
        visible = true
        delay(SPLASH_ANIM_MILLIS + SPLASH_HOLD_MILLIS)
        minHoldElapsed = true
    }

    LaunchedEffect(destination, minHoldElapsed) {
        val resolved = destination
        if (minHoldElapsed && resolved != null) {
            onNavigate(resolved)
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.alpha(alpha).scale(scale),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "HomeSajja",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Give furniture a new life.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
