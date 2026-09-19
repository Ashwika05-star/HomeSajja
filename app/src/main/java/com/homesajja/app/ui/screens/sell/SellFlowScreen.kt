package com.homesajja.app.ui.screens.sell

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
import com.homesajja.app.ui.components.AskHomeSajjaBanner
import com.homesajja.app.ui.components.ErrorState
import com.homesajja.app.ui.components.InlineErrorBanner
import com.homesajja.app.ui.components.LoadingState
import com.homesajja.app.ui.components.OutlinedButton
import com.homesajja.app.ui.components.PrimaryButton
import com.homesajja.app.viewmodel.PublishState
import com.homesajja.app.viewmodel.SellScreenState
import com.homesajja.app.viewmodel.SellStep
import com.homesajja.app.viewmodel.SellViewModel

/** Multi-step sell (or edit) flow. All state lives in [SellViewModel]; this only renders the current step. */
@Composable
fun SellFlowScreen(
    onExit: () -> Unit,
    onPublished: (listingId: String) -> Unit,
    onAskAi: () -> Unit,
) {
    val viewModel: SellViewModel = viewModel(factory = ViewModelFactory(LocalAppContainer.current))
    val ready = viewModel.screenState is SellScreenState.Ready
    val canGoBack = ready && viewModel.step != SellStep.CATEGORY
    val publishState = viewModel.publishState

    BackHandler(enabled = canGoBack && publishState is PublishState.Idle) { viewModel.back() }

    LaunchedEffect(publishState) {
        if (publishState is PublishState.Published) onPublished(publishState.listingId)
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (viewModel.isEditing) "Edit listing" else "Sell an item",
                onBackClick = { if (canGoBack) viewModel.back() else onExit() },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val screen = viewModel.screenState) {
                SellScreenState.Loading -> LoadingState()
                is SellScreenState.Error -> ErrorState(message = screen.message, onRetry = viewModel::load)
                SellScreenState.Ready -> StepContainer(viewModel, onAskAi)
            }
        }
    }

    PublishDialogs(publishState, onRetry = viewModel::next, onDismiss = viewModel::dismissPublishError)
}

@Composable
private fun StepContainer(viewModel: SellViewModel, onAskAi: () -> Unit) {
    val step = viewModel.step
    val steps = SellStep.entries
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
            if (step == SellStep.CATEGORY && !viewModel.isEditing) AskHomeSajjaBanner(onClick = onAskAi)
            SellStepContent(viewModel)
            viewModel.stepError?.let { InlineErrorBanner(message = it) }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (step != SellStep.CATEGORY) {
                OutlinedButton(text = "Back", onClick = viewModel::back, modifier = Modifier.weight(1f))
            }
            PrimaryButton(
                text = when {
                    !isLast -> "Next"
                    viewModel.isEditing -> "Save changes"
                    else -> "Publish"
                },
                onClick = viewModel::next,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PublishDialogs(state: PublishState, onRetry: () -> Unit, onDismiss: () -> Unit) {
    when (state) {
        is PublishState.Publishing -> AlertDialog(
            onDismissRequest = {},
            title = { Text("Publishing") },
            text = {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                    Text(state.progress)
                }
            },
            confirmButton = {},
        )
        is PublishState.Failed -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Couldn't publish") },
            text = { Text(state.message) },
            confirmButton = { TextButton(onClick = onRetry) { Text("Try again") } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        )
        PublishState.Idle, is PublishState.Published -> Unit
    }
}
