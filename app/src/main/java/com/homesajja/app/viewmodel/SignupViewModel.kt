package com.homesajja.app.viewmodel

import android.util.Patterns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homesajja.app.data.model.UserProfile
import com.homesajja.app.data.model.UserRole
import com.homesajja.app.data.model.VendorBusinessType
import com.homesajja.app.data.model.VendorProfile
import com.homesajja.app.repository.AuthRepository
import com.homesajja.app.repository.SessionRepository
import com.homesajja.app.repository.UserRepository
import com.homesajja.app.repository.VendorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SignupFormState(
    val role: UserRole = UserRole.USER,
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val phone: String = "",
    val city: String = "",
    val businessName: String = "",
    val businessType: VendorBusinessType? = null,
    val isGoogleAuthenticated: Boolean = false,
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val phoneError: String? = null,
    val cityError: String? = null,
    val businessNameError: String? = null,
    val businessTypeError: String? = null,
)

class SignupViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val vendorRepository: VendorRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    var form by mutableStateOf(SignupFormState())
        private set

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState

    private val _resolvedRole = MutableStateFlow<UserRole?>(null)
    val resolvedRole: StateFlow<UserRole?> = _resolvedRole

    fun onRoleChange(role: UserRole) {
        form = form.copy(role = role)
    }

    fun onNameChange(value: String) {
        form = form.copy(name = value, nameError = null)
    }

    fun onEmailChange(value: String) {
        form = form.copy(email = value, emailError = null)
    }

    fun onPasswordChange(value: String) {
        form = form.copy(password = value, passwordError = null)
    }

    fun onPhoneChange(value: String) {
        form = form.copy(phone = value, phoneError = null)
    }

    fun onCityChange(value: String) {
        form = form.copy(city = value, cityError = null)
    }

    fun onBusinessNameChange(value: String) {
        form = form.copy(businessName = value, businessNameError = null)
    }

    fun onBusinessTypeChange(value: VendorBusinessType) {
        form = form.copy(businessType = value, businessTypeError = null)
    }

    /** Called once Google Identity returns an ID token; authenticates immediately
     * so name/email are known, but the rest of the profile still needs [submit]. */
    fun onGoogleIdToken(idToken: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.signInWithGoogle(idToken).fold(
                onSuccess = {
                    form = form.copy(
                        name = authRepository.currentUserDisplayName ?: form.name,
                        email = authRepository.currentUserEmail ?: form.email,
                        isGoogleAuthenticated = true,
                    )
                    _uiState.value = AuthUiState.Idle
                },
                onFailure = { _uiState.value = AuthUiState.Error(mapAuthError(it)) },
            )
        }
    }

    fun onGoogleSignInFailed(message: String) {
        _uiState.value = AuthUiState.Error(message)
    }

    fun submit() {
        if (!validate()) return

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            val uidResult = if (form.isGoogleAuthenticated) {
                Result.success(authRepository.currentUserId ?: "")
            } else {
                authRepository.signUpWithEmail(form.name, form.email, form.password)
            }

            uidResult.fold(
                onSuccess = { uid -> createProfileAndFinish(uid) },
                onFailure = { _uiState.value = AuthUiState.Error(mapAuthError(it)) },
            )
        }
    }

    private suspend fun createProfileAndFinish(uid: String) {
        runCatching {
            when (form.role) {
                UserRole.USER -> userRepository.createUserProfile(
                    UserProfile(uid = uid, name = form.name, email = form.email, phone = form.phone, city = form.city),
                )
                UserRole.VENDOR -> vendorRepository.createVendorProfile(
                    VendorProfile(
                        uid = uid,
                        name = form.name,
                        email = form.email,
                        phone = form.phone,
                        businessName = form.businessName,
                        businessType = form.businessType?.name.orEmpty(),
                        city = form.city,
                    ),
                )
            }
            sessionRepository.saveRole(form.role)
        }.fold(
            onSuccess = {
                _resolvedRole.value = form.role
                _uiState.value = AuthUiState.Success
            },
            onFailure = { _uiState.value = AuthUiState.Error(mapAuthError(it)) },
        )
    }

    private fun validate(): Boolean {
        val nameError = if (form.name.isBlank()) "Enter your name" else null
        val emailError = if (!Patterns.EMAIL_ADDRESS.matcher(form.email).matches()) "Enter a valid email address" else null
        val passwordError = if (!form.isGoogleAuthenticated && form.password.length < 6) {
            "Password must be at least 6 characters"
        } else {
            null
        }
        val phoneDigits = form.phone.filter { it.isDigit() }
        val phoneError = if (phoneDigits.length != 10) "Enter a valid 10-digit phone number" else null
        val cityError = if (form.city.isBlank()) "Select your city" else null
        val businessNameError = if (form.role == UserRole.VENDOR && form.businessName.isBlank()) {
            "Enter your business name"
        } else {
            null
        }
        val businessTypeError = if (form.role == UserRole.VENDOR && form.businessType == null) {
            "Select your business type"
        } else {
            null
        }

        form = form.copy(
            nameError = nameError,
            emailError = emailError,
            passwordError = passwordError,
            phoneError = phoneError,
            cityError = cityError,
            businessNameError = businessNameError,
            businessTypeError = businessTypeError,
        )

        return listOfNotNull(
            nameError, emailError, passwordError, phoneError, cityError, businessNameError, businessTypeError,
        ).isEmpty()
    }
}
