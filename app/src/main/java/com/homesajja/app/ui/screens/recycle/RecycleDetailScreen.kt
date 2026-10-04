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
import com.homesajja.app.data.model.PayDirection
import com.homesajja.app.data.model.PaymentMethod
import com.homesajja.app.payment.paymentOpen
import com.homesajja.app.data.model.CancelContext
import com.homesajja.app.payment.refundRequired
import com.homesajja.app.ui.components.CancellationCard
import com.homesajja.app.ui.components.PaymentPanel
import com.homesajja.app.ui.components.VendorCancelDialog
import com.homesajja.app.ui.components.PayoutUpiDialog
import com.homesajja.app.ui.components.QuoteCard
import com.homesajja.app.ui.components.QuoteDialog

/** One recycling request: photos, the problem, the tracking pipeline, and the actions the viewer may take. */
@Composable
fun RecycleDetailScreen(onBackClick: () -> Unit) {
    val viewModel: RecycleDetailViewModel = viewModel(factory = ViewModelFactory(LocalAppContainer.current))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingAction by remember { mutableStateOf<RecycleAction?>(null) }
    var quoteAction by remember { mutableStateOf<RecycleAction?>(null) }
    var cancelling by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = { AppTopBar(title = "Recycling request", onBackClick = onBackClick) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            (state as? RecycleDetailUiState.Content)?.takeIf { it.actions.isNotEmpty() }?.let {
                ActionBar(
                    content = it,
                    onAction = { action ->
                        when {
                            action.needsCancellation -> cancelling = true
                            action.needsQuote -> quoteAction = action
                            else -> pendingAction = action
                        }
                    },
                )
            }
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val current = state) {
                RecycleDetailUiState.Loading -> LoadingState()
                is RecycleDetailUiState.Error -> ErrorState(message = current.message, onRetry = viewModel::retry)
                is RecycleDetailUiState.Content -> DetailContent(current, onMarkPaid = viewModel::markPaid, onConfirmReceived = viewModel::confirmPayment)
            }
        }
    }

    if (cancelling) {
        val content = state as? RecycleDetailUiState.Content
        if (content == null) {
            cancelling = false
        } else {
            VendorCancelDialog(
                context = CancelContext.RECYCLING,
                title = "Cancel this job?",
                otherName = content.request.userName,
                refundAmount = if (refundRequired(content.request.payment, content.request.vendorId)) content.request.agreedAmount else null,
                onConfirm = { reason, note, refundDone ->
                    cancelling = false
                    viewModel.cancelByVendor(reason, note, refundDone)
                },
                onDismiss = { cancelling = false },
            )
        }
    }

    quoteAction?.let { action ->
        val content = state as? RecycleDetailUiState.Content
        QuoteDialog(
            repair = false,
            revised = action == RecycleAction.REVISE_QUOTE,
            initial = content?.request?.quote,
            vendorUpiId = content?.myUpiId,
            onSend = {
                quoteAction = null
                viewModel.sendQuote(it)
            },
            onDismiss = { quoteAction = null },
        )
    }

    pendingAction?.let { action ->
        val content = state as? RecycleDetailUiState.Content
        val quote = content?.request?.quote
        // Accepting a quote where the recycler pays: the customer says where to send the money first.
        if (action == RecycleAction.ACCEPT_QUOTE && quote?.direction == PayDirection.VENDOR_PAYS_USER) {
            PayoutUpiDialog(
                amount = quote.amount,
                initialUpiId = content.myUpiId,
                onConfirm = {
                    pendingAction = null
                    viewModel.perform(action, it)
                },
                onDismiss = { pendingAction = null },
            )
            return@let
        }
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
    RecycleAction.ACCEPT -> "Accept this recycling request for free?"
    RecycleAction.SEND_QUOTE, RecycleAction.REVISE_QUOTE -> "Send this quote?"
    RecycleAction.ACCEPT_QUOTE -> "Accept this quote? The amount is fixed from then on."
    RecycleAction.DECLINE_QUOTE -> "Decline this quote? The recycler can send a revised quote or close the request."
    RecycleAction.CLOSE -> "Close this request? The customer will see it as rejected."
    RecycleAction.CANCEL_BY_VENDOR -> "Cancel this job? You'll be asked for a reason."
    RecycleAction.REJECT -> "Reject this recycling request? The customer will see it as rejected."
    RecycleAction.SCHEDULE -> "Mark the pickup or drop-off as scheduled?"
    RecycleAction.COMPLETE -> "Mark this recycling request as completed?"
    RecycleAction.CANCEL -> "Cancel this recycling request?"
}

@Composable
private fun DetailContent(
    content: RecycleDetailUiState.Content,
    onMarkPaid: (PaymentMethod, String?) -> Unit,
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
                    RecycleStatusTracker(status = request.status, hasQuote = request.quote != null)
                    waitingText(request.status, content.viewerIsRecycler, request.userName, request.vendorName.orEmpty().ifBlank { "the recycler" })?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            request.quote?.let { QuoteCard(quote = it, repair = false, agreedAmount = request.agreedAmount) }

            request.cancellation?.let {
                CancellationCard(
                    vendorName = request.vendorName.orEmpty().ifBlank { "the recycler" },
                    cancellation = it,
                    agreedAmount = request.agreedAmount,
                    payment = request.payment,
                    refund = request.refund,
                    viewerIsPayer = request.payment?.payerId == request.userId && !content.viewerIsRecycler,
                )
            }

            val payment = request.payment
            val amount = request.agreedAmount
            if (payment != null && amount != null && request.paymentOpen) {
                val customerIsPayer = payment.payerId == request.userId
                PaymentPanel(
                    amount = amount,
                    payment = payment,
                    viewerId = if (content.viewerIsRecycler) request.vendorId else request.userId,
                    payerName = if (customerIsPayer) request.userName else request.vendorName.orEmpty().ifBlank { "The recycler" },
                    payeeName = if (customerIsPayer) request.vendorName.orEmpty().ifBlank { "The recycler" } else request.userName,
                    paymentNote = "HomeSajja recycling",
                    cashLabel = if (request.method == com.homesajja.app.data.model.RecycleMethod.PICKUP) "Cash on pickup" else "Cash at drop-off",
                    busy = content.isBusy,
                    onMarkPaid = onMarkPaid,
                    onConfirmReceived = onConfirmReceived,
                )
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

/** One line saying who the request is waiting for, or null when there is nothing to wait for. */
private fun waitingText(status: RecyclingStatus, viewerIsRecycler: Boolean, customer: String, recycler: String): String? = when (status) {
    RecyclingStatus.QUOTED -> if (viewerIsRecycler) "Waiting for $customer to accept or decline your quote." else "Check the quote below, then accept or decline it."
    RecyclingStatus.DECLINED -> if (viewerIsRecycler) "$customer declined the quote. Send a revised quote or close the request." else "You declined the quote. $recycler can send a revised quote."
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
private fun ActionBar(content: RecycleDetailUiState.Content, onAction: (RecycleAction) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Two buttons to a row, so a long label never gets squeezed.
            content.actions.withIndex().chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    row.forEach { (index, action) ->
                        val isPrimary = index == 0 && action != RecycleAction.CANCEL && action != RecycleAction.REJECT &&
                            action != RecycleAction.CLOSE && action != RecycleAction.DECLINE_QUOTE && action != RecycleAction.CANCEL_BY_VENDOR
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
