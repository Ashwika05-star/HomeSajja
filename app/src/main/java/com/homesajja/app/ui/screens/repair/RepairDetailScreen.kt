package com.homesajja.app.ui.screens.repair

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
import com.homesajja.app.data.model.RepairStatus
import com.homesajja.app.ui.components.PaymentPanel
import com.homesajja.app.ui.components.QuoteCard
import com.homesajja.app.ui.components.QuoteDialog
import com.homesajja.app.ui.components.RepairStatusTracker
import com.homesajja.app.payment.paymentOpen
import com.homesajja.app.viewmodel.RepairAction
import com.homesajja.app.viewmodel.RepairDetailUiState
import com.homesajja.app.viewmodel.RepairDetailViewModel

/** One repair request: photos, the problem, the tracking pipeline, and the actions the viewer may take. */
@Composable
fun RepairDetailScreen(onBackClick: () -> Unit, onOpenChat: (String) -> Unit) {
    val viewModel: RepairDetailViewModel = viewModel(factory = ViewModelFactory(LocalAppContainer.current))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingAction by remember { mutableStateOf<RepairAction?>(null) }
    var quoteAction by remember { mutableStateOf<RepairAction?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }
    LaunchedEffect(viewModel) {
        viewModel.openChat.collect { onOpenChat(it) }
    }

    Scaffold(
        topBar = { AppTopBar(title = "Repair request", onBackClick = onBackClick) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            (state as? RepairDetailUiState.Content)?.takeIf { it.actions.isNotEmpty() }?.let {
                ActionBar(content = it, onAction = { action -> if (action.needsQuote) quoteAction = action else pendingAction = action })
            }
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val current = state) {
                RepairDetailUiState.Loading -> LoadingState()
                is RepairDetailUiState.Error -> ErrorState(message = current.message, onRetry = viewModel::retry)
                is RepairDetailUiState.Content -> DetailContent(
                    current,
                    onMessage = viewModel::openChat,
                    onMarkPaid = viewModel::markPaid,
                    onConfirmReceived = viewModel::confirmPayment,
                )
            }
        }
    }

    quoteAction?.let { action ->
        val content = state as? RepairDetailUiState.Content
        QuoteDialog(
            repair = true,
            revised = action == RepairAction.REVISE_QUOTE,
            initial = content?.request?.quote,
            vendorUpiId = content?.vendorUpiId,
            onSend = {
                quoteAction = null
                viewModel.sendQuote(it)
            },
            onDismiss = { quoteAction = null },
        )
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

private fun confirmationText(action: RepairAction): String = when (action) {
    RepairAction.SEND_QUOTE, RepairAction.REVISE_QUOTE -> "Send this quote?"
    RepairAction.ACCEPT_QUOTE -> "Accept this quote? The price is fixed from then on, and the vendor can start work."
    RepairAction.DECLINE_QUOTE -> "Decline this quote? The vendor can send a revised quote or close the request."
    RepairAction.REJECT -> "Reject this repair request? The customer will see it as rejected."
    RepairAction.CLOSE -> "Close this request? The customer will see it as rejected."
    RepairAction.START -> "Mark the repair as in progress?"
    RepairAction.MARK_READY -> "Mark the furniture as ready for pickup?"
    RepairAction.COMPLETE -> "Mark this repair as completed?"
    RepairAction.CANCEL -> "Cancel this repair request?"
}

@Composable
private fun DetailContent(
    content: RepairDetailUiState.Content,
    onMessage: () -> Unit,
    onMarkPaid: (com.homesajja.app.data.model.PaymentMethod, String?) -> Unit,
    onConfirmReceived: () -> Unit,
) {
    val request = content.request
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ImageCarousel(images = request.images)

        Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(request.furnitureTitle, style = MaterialTheme.typography.headlineSmall)
                Text(
                    "${request.furnitureCategory.displayName} · ${request.city}",
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
                    RepairStatusTracker(status = request.status)
                    waitingText(request.status, content.viewerIsVendor, request.userName, request.vendorName)?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            request.quote?.let { QuoteCard(quote = it, repair = true, agreedAmount = request.agreedAmount) }

            val payment = request.payment
            val amount = request.agreedAmount
            if (payment != null && amount != null && request.paymentOpen) {
                PaymentPanel(
                    amount = amount,
                    payment = payment,
                    viewerId = if (content.viewerIsVendor) request.vendorId else request.userId,
                    payerName = request.userName,
                    payeeName = request.vendorName,
                    paymentNote = "HomeSajja repair: ${request.furnitureTitle}",
                    cashLabel = "Cash at service",
                    busy = content.isBusy,
                    onMarkPaid = onMarkPaid,
                    onConfirmReceived = onConfirmReceived,
                )
            }

            DetailRow("Problem", request.problemType.displayName)
            DetailRow("Description", request.issueDescription)
            if (content.viewerIsVendor) {
                DetailRow("Customer", request.userName)
            } else {
                DetailRow("Repair provider", request.vendorName)
            }
            OutlinedButton(
                text = if (content.viewerIsVendor) "Message ${request.userName}" else "Message ${request.vendorName}",
                onClick = onMessage,
                modifier = Modifier.fillMaxWidth(),
            )
            // Once the repair is done, the customer can review the provider.
            if (request.status == RepairStatus.COMPLETED && !content.viewerIsVendor) {
                ReviewPrompt(ReviewParams(EntityType.REPAIR_REQUEST, request.id, request.vendorId, request.vendorName))
            }
        }
    }
}

/** One line saying who the request is waiting for, or null when there is nothing to wait for. */
private fun waitingText(status: RepairStatus, viewerIsVendor: Boolean, customer: String, vendor: String): String? = when (status) {
    RepairStatus.REQUESTED -> if (viewerIsVendor) "Send a quote so $customer can agree on a price." else "Waiting for $vendor to send a quote."
    RepairStatus.QUOTED -> if (viewerIsVendor) "Waiting for $customer to accept or decline your quote." else "Check the quote below, then accept or decline it."
    RepairStatus.DECLINED -> if (viewerIsVendor) "$customer declined the quote. Send a revised quote or close the request." else "You declined the quote. $vendor can send a revised quote."
    RepairStatus.AGREED, RepairStatus.ACCEPTED -> if (viewerIsVendor) "The price is agreed. You can start work." else "The price is agreed. Waiting for $vendor to start work."
    else -> null
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ActionBar(content: RepairDetailUiState.Content, onAction: (RepairAction) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Two buttons to a row, so a long label never gets squeezed.
            content.actions.withIndex().chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    row.forEach { (index, action) ->
                        val isPrimary = index == 0 && action != RepairAction.CANCEL && action != RepairAction.REJECT &&
                            action != RepairAction.CLOSE && action != RepairAction.DECLINE_QUOTE
                        if (isPrimary) {
                            PrimaryButton(text = action.label, onClick = { onAction(action) }, enabled = !content.isBusy, modifier = Modifier.weight(1f))
                        } else {
                            OutlinedButton(text = action.label, onClick = { onAction(action) }, enabled = !content.isBusy, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
