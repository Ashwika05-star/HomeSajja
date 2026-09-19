package com.homesajja.app.ui.screens.recycle

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.homesajja.app.di.LocalAppContainer
import com.homesajja.app.di.ViewModelFactory
import com.homesajja.app.ui.components.AppTopBar
import com.homesajja.app.ui.components.ErrorState
import com.homesajja.app.ui.components.InlineErrorBanner
import com.homesajja.app.ui.components.LoadingState
import com.homesajja.app.ui.components.OutlinedButton
import com.homesajja.app.ui.components.PrimaryButton
import com.homesajja.app.viewmodel.RecycleRequestViewModel
import com.homesajja.app.viewmodel.RecycleScreenState
import com.homesajja.app.viewmodel.RecycleStep
import com.homesajja.app.viewmodel.RecycleSendState

/** Multi-step "Recycle furniture" flow. All state lives in [RecycleRequestViewModel]; this renders the current step. */
@Composable
fun RecycleRequestScreen(
    onExit: () -> Unit,
    onSent: (requestId: String) -> Unit,
) {
    val viewModel: RecycleRequestViewModel = viewModel(factory = ViewModelFactory(LocalAppContainer.current))
    val ready = viewModel.screenState is RecycleScreenState.Ready
    val canGoBack = ready && viewModel.step != RecycleStep.PHOTOS
    val sendState = viewModel.sendState

    BackHandler(enabled = canGoBack && sendState is RecycleSendState.Idle) { viewModel.back() }

    LaunchedEffect(sendState) {
        if (sendState is RecycleSendState.Sent) onSent(sendState.requestId)
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Recycle furniture",
                onBackClick = { if (canGoBack) viewModel.back() else onExit() },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val screen = viewModel.screenState) {
                RecycleScreenState.Loading -> LoadingState()
                is RecycleScreenState.Error -> ErrorState(message = screen.message, onRetry = viewModel::load)
                RecycleScreenState.Ready -> StepContainer(viewModel)
            }
        }
    }

    SendDialogs(sendState, onRetry = viewModel::next, onDismiss = viewModel::dismissSendError)
}

@Composable
private fun StepContainer(viewModel: RecycleRequestViewModel) {
    val step = viewModel.step
    val steps = RecycleStep.entries
    val isLast = step == steps.last()

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text(
                "Step ${step.ordinal + 1} of ${steps.size} · ${step.title}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LinearProgressIndicator(
                progress = { (step.ordinal + 1f) / steps.size },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            RecycleStepContent(viewModel)
            viewModel.stepError?.let { InlineErrorBanner(message = it) }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (step != RecycleStep.PHOTOS) {
                OutlinedButton(text = "Back", onClick = viewModel::back, modifier = Modifier.weight(1f))
            }
            PrimaryButton(
                text = if (isLast) "Submit request" else "Next",
                onClick = viewModel::next,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SendDialogs(state: RecycleSendState, onRetry: () -> Unit, onDismiss: () -> Unit) {
    when (state) {
        is RecycleSendState.Sending -> AlertDialog(
            onDismissRequest = {},
            title = { Text("Sending") },
            text = {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                    Text(state.progress)
                }
            },
            confirmButton = {},
        )
        is RecycleSendState.Failed -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Couldn't send") },
            text = { Text(state.message) },
            confirmButton = { TextButton(onClick = onRetry) { Text("Try again") } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        )
        RecycleSendState.Idle, is RecycleSendState.Sent -> Unit
    }
}
