package com.homesajja.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.homesajja.app.data.model.Cities
import com.homesajja.app.data.model.UserRole
import com.homesajja.app.data.model.VendorBusinessType
import com.homesajja.app.di.LocalAppContainer
import com.homesajja.app.di.ViewModelFactory
import com.homesajja.app.ui.components.AppDropdownField
import com.homesajja.app.ui.components.AppTextField
import com.homesajja.app.ui.components.AppTopBar
import com.homesajja.app.ui.components.InlineErrorBanner
import com.homesajja.app.ui.components.OutlinedButton
import com.homesajja.app.ui.components.PrimaryButton
import com.homesajja.app.viewmodel.AuthUiState
import com.homesajja.app.viewmodel.SignupViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(
    onSignupSuccess: (UserRole) -> Unit,
    onNavigateToLogin: () -> Unit,
    onBackClick: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: SignupViewModel = viewModel(factory = ViewModelFactory(container))
    val form = viewModel.form
    val uiState by viewModel.uiState.collectAsState()
    val resolvedRole by viewModel.resolvedRole.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState, resolvedRole) {
        if (uiState is AuthUiState.Success) {
            resolvedRole?.let(onSignupSuccess)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "Create Account", onBackClick = onBackClick)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("I am a...", style = MaterialTheme.typography.titleMedium)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = form.role == UserRole.USER,
                    onClick = { viewModel.onRoleChange(UserRole.USER) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                ) {
                    Text("User")
                }
                SegmentedButton(
                    selected = form.role == UserRole.VENDOR,
                    onClick = { viewModel.onRoleChange(UserRole.VENDOR) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                ) {
                    Text("Vendor")
                }
            }

            if (uiState is AuthUiState.Error) {
                InlineErrorBanner(message = (uiState as AuthUiState.Error).message)
            }

            AppTextField(
                value = form.name,
                onValueChange = viewModel::onNameChange,
                label = "Full name",
                isError = form.nameError != null,
                errorMessage = form.nameError,
                modifier = Modifier.fillMaxWidth(),
            )
            AppTextField(
                value = form.email,
                onValueChange = viewModel::onEmailChange,
                label = "Email",
                isError = form.emailError != null,
                errorMessage = form.emailError,
                keyboardType = KeyboardType.Email,
                modifier = Modifier.fillMaxWidth(),
            )
            if (!form.isGoogleAuthenticated) {
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
            }
            AppTextField(
                value = form.phone,
                onValueChange = viewModel::onPhoneChange,
                label = "Phone number",
                isError = form.phoneError != null,
                errorMessage = form.phoneError,
                keyboardType = KeyboardType.Phone,
                modifier = Modifier.fillMaxWidth(),
            )
            AppDropdownField(
                label = "City",
                selectedOption = form.city,
                options = Cities.ALL,
                onOptionSelected = viewModel::onCityChange,
                isError = form.cityError != null,
                errorMessage = form.cityError,
                modifier = Modifier.fillMaxWidth(),
            )

            if (form.role == UserRole.VENDOR) {
                AppTextField(
                    value = form.businessName,
                    onValueChange = viewModel::onBusinessNameChange,
                    label = "Business name",
                    isError = form.businessNameError != null,
                    errorMessage = form.businessNameError,
                    modifier = Modifier.fillMaxWidth(),
                )
                AppDropdownField(
                    label = "Business type",
                    selectedOption = form.businessType?.displayName.orEmpty(),
                    options = VendorBusinessType.entries.map { it.displayName },
                    onOptionSelected = { selected ->
                        VendorBusinessType.entries.firstOrNull { it.displayName == selected }
                            ?.let(viewModel::onBusinessTypeChange)
                    },
                    isError = form.businessTypeError != null,
                    errorMessage = form.businessTypeError,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            PrimaryButton(
                text = if (uiState is AuthUiState.Loading) "Creating account..." else "Create Account",
                onClick = viewModel::submit,
                enabled = uiState !is AuthUiState.Loading,
                modifier = Modifier.fillMaxWidth(),
            )

            if (!form.isGoogleAuthenticated) {
                OutlinedButton(
                    text = "Sign up with Google",
                    enabled = uiState !is AuthUiState.Loading,
                    onClick = {
                        scope.launch {
                            container.googleSignInManager(context).requestIdToken().fold(
                                onSuccess = viewModel::onGoogleIdToken,
                                onFailure = { viewModel.onGoogleSignInFailed(it.message ?: "Google sign-in failed.") },
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            TextButton(onClick = onNavigateToLogin, modifier = Modifier.fillMaxWidth()) {
                Text("Already have an account? Log in")
            }
        }
    }
}
