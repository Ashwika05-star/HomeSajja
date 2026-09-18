package com.homesajja.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homesajja.app.data.model.UserRole
import com.homesajja.app.repository.AuthRepository
import com.homesajja.app.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface SplashDestination {
    data object Welcome : SplashDestination
    data object UserHome : SplashDestination
    data object VendorHome : SplashDestination
}

class SplashViewModel(
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private val _destination = MutableStateFlow<SplashDestination?>(null)
    val destination: StateFlow<SplashDestination?> = _destination

    init {
        viewModelScope.launch {
            val uid = authRepository.currentUserId
            _destination.value = if (uid == null) {
                SplashDestination.Welcome
            } else {
                when (sessionRepository.roleFlow.first()) {
                    UserRole.USER -> SplashDestination.UserHome
                    UserRole.VENDOR -> SplashDestination.VendorHome
                    // Signed in with Firebase Auth but no cached role — a broken
                    // local state we can't safely route from, so sign out and
                    // send back through Welcome/Auth rather than guess.
                    null -> {
                        authRepository.signOut()
                        SplashDestination.Welcome
                    }
                }
            }
        }
    }
}
