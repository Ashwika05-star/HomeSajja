package com.homesajja.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homesajja.app.repository.AuthRepository
import com.homesajja.app.repository.DeviceTokenRepository
import com.homesajja.app.repository.SessionRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Shared by UserHome and VendorHome — both just need to greet the signed-in
 * account and log out; the role itself is implicit in which screen is showing. */
class HomeViewModel(
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository,
    private val deviceTokenRepository: DeviceTokenRepository,
) : ViewModel() {

    val displayName: String
        get() = authRepository.currentUserDisplayName
            ?: authRepository.currentUserEmail
            ?: "there"

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            // Stop pushes for this person reaching this phone; give up quickly if offline.
            withTimeoutOrNull(TOKEN_TIMEOUT_MILLIS) { runCatching { deviceTokenRepository.unregisterCurrentToken() } }
            authRepository.signOut()
            sessionRepository.clearSession()
            onLoggedOut()
        }
    }

    private companion object {
        const val TOKEN_TIMEOUT_MILLIS = 2_000L
    }
}
