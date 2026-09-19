package com.homesajja.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.homesajja.app.di.LocalAppContainer
import com.homesajja.app.di.ViewModelFactory
import com.homesajja.app.viewmodel.DeleteAccountState
import com.homesajja.app.viewmodel.DeleteAccountViewModel

/** The "Delete my account" button and its confirmation, for both users and vendors. [onDeleted] runs once the account is gone. */
@Composable
fun DeleteAccountSection(onDeleted: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel: DeleteAccountViewModel = viewModel(factory = ViewModelFactory(LocalAppContainer.current))
    var password by remember { mutableStateOf("") }
    val state = viewModel.state

    LaunchedEffect(state) {
        if (state == DeleteAccountState.Deleted) onDeleted()
    }

    OutlinedButton(text = "Delete my account", onClick = viewModel::open, modifier = modifier.fillMaxWidth())

    if (viewModel.dialogOpen) {
        AlertDialog(
            onDismissRequest = viewModel::close,
            title = { Text("Delete your account?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "This removes your account, profile, listings, saved items and notifications. It can't be undone. " +
                            "Chats, requests and reviews you shared with other people stay for them.",
                    )
                    if (viewModel.needsPassword) {
                        AppTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = "Enter your password to confirm",
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    (state as? DeleteAccountState.Failed)?.let {
                        Text(it.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    }
                    if (state == DeleteAccountState.Working) CircularProgressIndicator()
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.delete(password) },
                    enabled = state != DeleteAccountState.Working && (!viewModel.needsPassword || password.isNotEmpty()),
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = viewModel::close, enabled = state != DeleteAccountState.Working) { Text("Cancel") } },
        )
    }
}
