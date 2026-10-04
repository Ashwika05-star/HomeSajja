package com.homesajja.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.homesajja.app.data.model.UserRole
import com.homesajja.app.di.LocalAppContainer
import com.homesajja.app.di.ViewModelFactory
import com.homesajja.app.ui.components.AppTextField
import com.homesajja.app.ui.components.AppTopBar
import com.homesajja.app.ui.components.InlineErrorBanner
import com.homesajja.app.ui.components.OutlinedButton
import com.homesajja.app.ui.components.GoogleSignInButton
import com.homesajja.app.ui.components.PrimaryButton
import com.homesajja.app.viewmodel.AuthUiState
import com.homesajja.app.viewmodel.LoginViewModel

@Composable
fun LoginScreen(
    onLoginSuccess: (UserRole) -> Unit,
    onNavigateToSignup: () -> Unit,
    onBackClick: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: LoginViewModel = viewModel(factory = ViewModelFactory(container))
    val form = viewModel.form
    val uiState by viewModel.uiState.collectAsState()
    val resolvedRole by viewModel.resolvedRole.collectAsState()

    LaunchedEffect(uiState, resolvedRole) {
        if (uiState is AuthUiState.Success) {
            resolvedRole?.let(onLoginSuccess)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "Log In", onBackClick = onBackClick)

        Column(
            modifier = Modifier
                .fillMaxSize()
                // Keep the form above the navigation bar and the keyboard.
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (uiState is AuthUiState.Error) {
                InlineErrorBanner(message = (uiState as AuthUiState.Error).message)
            }

            AppTextField(
                value = form.email,
                onValueChange = viewModel::onEmailChange,
                label = "Email",
                isError = form.emailError != null,
                errorMessage = form.emailError,
                keyboardType = KeyboardType.Email,
                modifier = Modifier.fillMaxWidth(),
            )
            AppTextField(
                value = form.password,
                onValueChange = viewModel::onPasswordChange,
                label = "Password",
                isError = form.passwordError != null,
                errorMessage = form.passwordError,
                visualTransformation = PasswordVisualTransformation(),
                keyboardType = KeyboardType.Password,
                modifier = Modifier.fillMaxWidth(),
            )

            PrimaryButton(
                text = if (uiState is AuthUiState.Loading) "Logging in..." else "Log In",
                onClick = viewModel::login,
                enabled = uiState !is AuthUiState.Loading,
                modifier = Modifier.fillMaxWidth(),
            )

            GoogleSignInButton(
                text = "Log in with Google",
                enabled = uiState !is AuthUiState.Loading,
                error = viewModel.googleError,
                onStarted = viewModel::onGoogleSignInStarted,
                onIdToken = viewModel::onGoogleIdToken,
                onFailed = viewModel::onGoogleSignInFailed,
            )

            TextButton(onClick = onNavigateToSignup, modifier = Modifier.fillMaxWidth()) {
                Text("Don't have an account? Sign up")
            }
        }
    }
}
