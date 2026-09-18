package com.homesajja.app.di

import androidx.compose.runtime.staticCompositionLocalOf

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer not provided — wrap content with CompositionLocalProvider(LocalAppContainer provides ...)")
}
