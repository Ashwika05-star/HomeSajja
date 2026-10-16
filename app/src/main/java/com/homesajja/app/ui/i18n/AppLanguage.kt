package com.homesajja.app.ui.i18n

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Supported app languages. [ENGLISH] is the default; [HINDI] renders all vendor
 * workflows, requests, dashboards and labels in natural Devanagari Hindi.
 */
enum class AppLanguage(val code: String, val displayName: String, val nativeName: String) {
    ENGLISH("en", "English", "English"),
    HINDI("hi", "Hindi", "हिन्दी"),
    ;

    val isHindi: Boolean get() = this == HINDI

    companion object {
        fun fromStored(stored: String?): AppLanguage =
            entries.firstOrNull { it.name.equals(stored, ignoreCase = true) || it.code.equals(stored, ignoreCase = true) } ?: ENGLISH
    }
}

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.ENGLISH }

val isAppHindi: Boolean
    @Composable
    @ReadOnlyComposable
    get() = LocalAppLanguage.current.isHindi

private val Context.languageDataStore by preferencesDataStore(name = "language_preferences")
private val LANGUAGE_KEY = stringPreferencesKey("app_language")

/**
 * Persists the user's language choice (English / हिन्दी) across restarts in DataStore.
 */
class LanguagePreference(private val context: Context) {

    val languageFlow: Flow<AppLanguage> = context.languageDataStore.data.map { prefs ->
        AppLanguage.fromStored(prefs[LANGUAGE_KEY])
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.languageDataStore.edit { it[LANGUAGE_KEY] = language.name }
    }
}
