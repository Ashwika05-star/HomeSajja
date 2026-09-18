package com.homesajja.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homesajja.app.repository.AuthRepository
import com.homesajja.app.repository.SessionRepository
import kotlinx.coroutines.launch

/** Shared by UserHome and VendorHome — both just need to greet the signed-in
 * account and log out; the role itself is implicit in which screen is showing. */
class HomeViewModel(
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    val displayName: String
        get() = authRepository.currentUserDisplayName
            ?: authRepository.currentUserEmail
            ?: "there"

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut()
            sessionRepository.clearSession()
            onLoggedOut()
        }
    }
}
