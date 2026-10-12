package com.homesajja.app.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** What the person picked for the app's appearance. [SYSTEM] follows the phone's light / dark setting, and is the default. */
enum class ThemeMode(val displayName: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("System"),
    ;

    companion object {
        /** The stored value back to a mode; anything missing or unrecognised is [SYSTEM]. */
        fun fromStored(stored: String?): ThemeMode = entries.firstOrNull { it.name == stored } ?: SYSTEM
    }
}

private val Context.themeDataStore by preferencesDataStore(name = "appearance")
private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")

/** Remembers the Light / Dark / System choice across restarts. It is a phone setting, so it is not cleared on sign-out. */
class ThemePreference(private val context: Context) {

    val modeFlow: Flow<ThemeMode> = context.themeDataStore.data.map { prefs -> ThemeMode.fromStored(prefs[THEME_MODE_KEY]) }

    suspend fun setMode(mode: ThemeMode) {
        context.themeDataStore.edit { it[THEME_MODE_KEY] = mode.name }
    }
}
