package com.homesajja.app.ui.screens.exchange

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import com.homesajja.app.ui.components.ExchangeStatusTracker
import com.homesajja.app.ui.components.ItemComparison
import com.homesajja.app.ui.components.LoadingState
import com.homesajja.app.ui.components.OutlinedButton
import com.homesajja.app.ui.components.PrimaryButton
import com.homesajja.app.viewmodel.ExchangeAction
import com.homesajja.app.viewmodel.ExchangeDetailUiState
import com.homesajja.app.viewmodel.ExchangeDetailViewModel

/** One exchange request: both items compared, its status, and the actions the viewer may take. */
@Composable
fun ExchangeDetailScreen(onBackClick: () -> Unit, onOpenListing: (String) -> Unit) {
    val viewModel: ExchangeDetailViewModel = viewModel(factory = ViewModelFactory(LocalAppContainer.current))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingAction by remember { mutableStateOf<ExchangeAction?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = { AppTopBar(title = "Exchange request", onBackClick = onBackClick) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            (state as? ExchangeDetailUiState.Content)?.takeIf { it.actions.isNotEmpty() }?.let {
                ActionBar(content = it, onAction = { action -> pendingAction = action })
            }
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val current = state) {
                ExchangeDetailUiState.Loading -> LoadingState()
                is ExchangeDetailUiState.Error -> ErrorState(message = current.message, onRetry = viewModel::retry)
                is ExchangeDetailUiState.Content -> DetailContent(current, onOpenListing)
            }
        }
    }

    pendingAction?.let { action ->
        AlertDialog(
            onDismissRequest = { pendingAction = null },
            title = { Text(action.confirmTitle()) },
            text = { Text(action.confirmText()) },
            confirmButton = {
                TextButton(onClick = {
                    pendingAction = null
                    viewModel.perform(action)
                }) { Text(action.label) }
            },
            dismissButton = { TextButton(onClick = { pendingAction = null }) { Text("Not now") } },
        )
    }
}

@Composable
private fun DetailContent(content: ExchangeDetailUiState.Content, onOpenListing: (String) -> Unit) {
    val request = content.request
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    if (content.isSender) "Sent to ${request.receiverName}" else "Sent by ${request.senderName}",
                    style = MaterialTheme.typography.titleSmall,
                )
                ExchangeStatusTracker(status = request.status)
            }
        }

        ItemComparison(
            left = request.offeredItem(content.offered),
            right = request.requestedItem(content.requested),
            leftLabel = if (content.isSender) "Your item" else "Their item",
            rightLabel = if (content.isSender) "Their item" else "Your item",
            onItemClick = onOpenListing,
        )

        if (request.message.isNotBlank()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Message", style = MaterialTheme.typography.titleMedium)
                Text(request.message, style = MaterialTheme.typography.bodyLarge)
            }
        }

        if (content.chatReady) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    "A chat thread for this exchange is ready. The chat screen is coming soon.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ActionBar(content: ExchangeDetailUiState.Content, onAction: (ExchangeAction) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background, shadowElevation = 8.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            content.actions.forEachIndexed { index, action ->
                if (index == 0 && action != ExchangeAction.CANCEL) {
                    PrimaryButton(
                        text = action.label,
                        onClick = { onAction(action) },
                        enabled = !content.isBusy,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    OutlinedButton(
                        text = action.label,
                        onClick = { onAction(action) },
                        enabled = !content.isBusy,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

private fun ExchangeAction.confirmTitle() = when (this) {
    ExchangeAction.ACCEPT -> "Accept this exchange?"
    ExchangeAction.DECLINE -> "Decline this exchange?"
    ExchangeAction.CANCEL -> "Cancel your request?"
    ExchangeAction.COMPLETE -> "Mark as completed?"
}

private fun ExchangeAction.confirmText() = when (this) {
    ExchangeAction.ACCEPT -> "Both items will be reserved for this swap and a chat thread will be opened."
    ExchangeAction.DECLINE -> "The sender will see that you declined. This can't be undone."
    ExchangeAction.CANCEL -> "The request will be withdrawn. This can't be undone."
    ExchangeAction.COMPLETE -> "Confirm that the swap has happened. Both items will be marked as exchanged."
}
