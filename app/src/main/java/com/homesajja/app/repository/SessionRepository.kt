package com.homesajja.app.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.homesajja.app.data.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore by preferencesDataStore(name = "session")
private val ROLE_KEY = stringPreferencesKey("role")

/** Caches the signed-in user's role locally so app restarts can route straight
 * to the right home screen without a Firestore read. Firebase Auth already
 * persists the sign-in itself; this only fills the gap it doesn't cover. */
class SessionRepository(private val context: Context) {

    val roleFlow: Flow<UserRole?> = context.sessionDataStore.data.map { prefs ->
        prefs[ROLE_KEY]?.let { stored -> runCatching { UserRole.valueOf(stored) }.getOrNull() }
    }

    suspend fun saveRole(role: UserRole) {
        context.sessionDataStore.edit { it[ROLE_KEY] = role.name }
    }

    suspend fun clearSession() {
        context.sessionDataStore.edit { it.clear() }
    }
}
