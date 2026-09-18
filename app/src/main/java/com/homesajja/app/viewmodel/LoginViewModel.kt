package com.homesajja.app.viewmodel

import android.util.Patterns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homesajja.app.data.model.UserRole
import com.homesajja.app.repository.AuthRepository
import com.homesajja.app.repository.SessionRepository
import com.homesajja.app.repository.UserRepository
import com.homesajja.app.repository.VendorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class LoginFormState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
)

class LoginViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val vendorRepository: VendorRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    var form by mutableStateOf(LoginFormState())
        private set

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState

    private val _resolvedRole = MutableStateFlow<UserRole?>(null)
    val resolvedRole: StateFlow<UserRole?> = _resolvedRole

    fun onEmailChange(value: String) {
        form = form.copy(email = value, emailError = null)
    }

    fun onPasswordChange(value: String) {
        form = form.copy(password = value, passwordError = null)
    }

    fun login() {
        if (!validate()) return

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.signInWithEmail(form.email, form.password).fold(
                onSuccess = { uid -> resolveRoleAndFinish(uid) },
                onFailure = { _uiState.value = AuthUiState.Error(mapAuthError(it)) },
            )
        }
    }

    fun onGoogleIdToken(idToken: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.signInWithGoogle(idToken).fold(
                onSuccess = { uid -> resolveRoleAndFinish(uid) },
                onFailure = { _uiState.value = AuthUiState.Error(mapAuthError(it)) },
            )
        }
    }

    fun onGoogleSignInFailed(message: String) {
        _uiState.value = AuthUiState.Error(message)
    }

    private suspend fun resolveRoleAndFinish(uid: String) {
        runCatching {
            val userProfile = userRepository.getUserProfile(uid)
            if (userProfile != null) {
                UserRole.USER
            } else {
                val vendorProfile = vendorRepository.getVendorProfile(uid)
                vendorProfile?.let { UserRole.VENDOR }
                    ?: error("No account profile found for this login. Please sign up first.")
            }
        }.fold(
            onSuccess = { role ->
                sessionRepository.saveRole(role)
                _resolvedRole.value = role
                _uiState.value = AuthUiState.Success
            },
            onFailure = { _uiState.value = AuthUiState.Error(mapAuthError(it)) },
        )
    }

    private fun validate(): Boolean {
        val emailError = if (!Patterns.EMAIL_ADDRESS.matcher(form.email).matches()) "Enter a valid email address" else null
        val passwordError = if (form.password.isBlank()) "Enter your password" else null

        form = form.copy(emailError = emailError, passwordError = passwordError)

        return listOfNotNull(emailError, passwordError).isEmpty()
    }
}
