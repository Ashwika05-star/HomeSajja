package com.homesajja.app.ui.screens.recycle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.homesajja.app.di.LocalAppContainer
import com.homesajja.app.di.ViewModelFactory
import com.homesajja.app.ui.components.AppTopBar
import com.homesajja.app.ui.components.ErrorState
import com.homesajja.app.ui.components.ImageCarousel
import com.homesajja.app.ui.components.LoadingState
import com.homesajja.app.ui.components.OutlinedButton
import com.homesajja.app.ui.components.PrimaryButton
import com.homesajja.app.ui.components.ReviewPrompt
import com.homesajja.app.viewmodel.ReviewParams
import com.homesajja.app.data.model.EntityType
import com.homesajja.app.data.model.RecyclingStatus
import com.homesajja.app.ui.components.RecycleStatusTracker
import com.homesajja.app.viewmodel.RecycleAction
import com.homesajja.app.viewmodel.RecycleDetailUiState
import com.homesajja.app.viewmodel.RecycleDetailViewModel

/** One recycling request: photos, the problem, the tracking pipeline, and the actions the viewer may take. */
@Composable
fun RecycleDetailScreen(onBackClick: () -> Unit) {
    val viewModel: RecycleDetailViewModel = viewModel(factory = ViewModelFactory(LocalAppContainer.current))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingAction by remember { mutableStateOf<RecycleAction?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = { AppTopBar(title = "Recycling request", onBackClick = onBackClick) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            (state as? RecycleDetailUiState.Content)?.takeIf { it.actions.isNotEmpty() }?.let {
                ActionBar(content = it, onAction = { action -> pendingAction = action })
            }
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val current = state) {
                RecycleDetailUiState.Loading -> LoadingState()
                is RecycleDetailUiState.Error -> ErrorState(message = current.message, onRetry = viewModel::retry)
                is RecycleDetailUiState.Content -> DetailContent(current)
            }
        }
    }

    pendingAction?.let { action ->
        AlertDialog(
            onDismissRequest = { pendingAction = null },
            title = { Text(action.label) },
            text = { Text(confirmationText(action)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingAction = null
                    viewModel.perform(action)
                }) { Text("Yes") }
            },
            dismissButton = { TextButton(onClick = { pendingAction = null }) { Text("Not now") } },
        )
    }
}

private fun confirmationText(action: RecycleAction): String = when (action) {
    RecycleAction.ACCEPT -> "Accept this recycling request?"
    RecycleAction.REJECT -> "Reject this recycling request? The customer will see it as rejected."
    RecycleAction.SCHEDULE -> "Mark the pickup or drop-off as scheduled?"
    RecycleAction.COMPLETE -> "Mark this recycling request as completed?"
    RecycleAction.CANCEL -> "Cancel this recycling request?"
}

@Composable
private fun DetailContent(content: RecycleDetailUiState.Content) {
    val request = content.request
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ImageCarousel(images = request.images)

        Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${request.material.displayName} furniture", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "${request.method.displayName} · ${request.city}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Progress", style = MaterialTheme.typography.titleSmall)
                    RecycleStatusTracker(status = request.status)
                }
            }

            DetailRow("Condition", request.condition.displayName)
            DetailRow("Material", request.material.displayName)
            DetailRow("Handover", request.method.displayName)
            if (content.viewerIsRecycler) {
                DetailRow("Customer", request.userName)
            } else if (request.vendorName != null) {
                DetailRow("Recycler", request.vendorName)
            } else {
                DetailRow("Recycler", "Not assigned yet. A recycler in ${request.city} will pick this up.")
            }
            // Once it is done, the customer can review the recycler.
            val recyclerId = request.vendorId
            if (request.status == RecyclingStatus.COMPLETED && !content.viewerIsRecycler && recyclerId != null) {
                ReviewPrompt(ReviewParams(EntityType.RECYCLING_REQUEST, request.id, recyclerId, request.vendorName ?: "the recycler"))
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ActionBar(content: RecycleDetailUiState.Content, onAction: (RecycleAction) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            content.actions.forEachIndexed { index, action ->
                val isPrimary = index == 0 && action != RecycleAction.CANCEL && action != RecycleAction.REJECT
                if (isPrimary) {
                    PrimaryButton(text = action.label, onClick = { onAction(action) }, enabled = !content.isBusy, modifier = Modifier.weight(1f))
                } else {
                    OutlinedButton(text = action.label, onClick = { onAction(action) }, enabled = !content.isBusy, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
