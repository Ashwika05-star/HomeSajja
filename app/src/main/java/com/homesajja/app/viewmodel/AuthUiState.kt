package com.homesajja.app.viewmodel

/** Submission state shared by Signup and Login — success just triggers
 * navigation, so no payload is needed beyond the resolved role (each
 * ViewModel exposes that separately). */
sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data object Success : AuthUiState
    data class Error(val message: String) : AuthUiState
}
