package com.homesajja.app.ui.screens.requests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.homesajja.app.data.model.PurchaseRequest
import com.homesajja.app.data.model.PurchaseStatus
import com.homesajja.app.di.LocalAppContainer
import com.homesajja.app.di.ViewModelFactory
import com.homesajja.app.ui.components.EmptyState
import com.homesajja.app.ui.components.ErrorState
import com.homesajja.app.ui.components.LoadingState
import com.homesajja.app.ui.components.OutlinedButton
import com.homesajja.app.ui.components.PrimaryButton
import com.homesajja.app.ui.components.AppTextField
import com.homesajja.app.ui.util.formatPrice
import com.homesajja.app.payment.UpiLaunchResult
import com.homesajja.app.payment.UpiPayment
import androidx.compose.ui.platform.LocalContext
import com.homesajja.app.ui.components.ReviewPrompt
import com.homesajja.app.viewmodel.ReviewParams
import com.homesajja.app.data.model.EntityType
import com.homesajja.app.ui.components.PurchaseStatusTracker
import com.homesajja.app.ui.util.formatPrice
import com.homesajja.app.viewmodel.MyRequestsUiState
import com.homesajja.app.viewmodel.MyRequestsViewModel
import com.homesajja.app.viewmodel.SellerAction
import com.homesajja.app.viewmodel.sellerActionsFor

/** Buy/Sell requests: Sent (I'm the buyer) and Received (someone wants my item). */
@Composable
fun MyRequestsScreen(
    snackbarHostState: SnackbarHostState,
    onOpenListing: (String) -> Unit,
    modifier: Modifier = Modifier,
    receivedOnly: Boolean = false,
) {
    val viewModel: MyRequestsViewModel = viewModel(factory = ViewModelFactory(LocalAppContainer.current))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var chosenTab by rememberSaveable { mutableIntStateOf(0) }
    // Vendors only ever receive purchase requests, so they get the Received list without the tabs.
    val selectedTab = if (receivedOnly) 1 else chosenTab

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    // Coming back from another screen (detail, sell, exchange...): reload if the data is old.
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshIfStale()
        onPauseOrDispose {}
    }

    Column(modifier = modifier.fillMaxSize()) {
        val content = state as? MyRequestsUiState.Content
        if (!receivedOnly) {
            TabRow(selectedTabIndex = selectedTab, containerColor = MaterialTheme.colorScheme.background) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { chosenTab = 0 },
                    text = { Text("Sent${content?.let { " (${it.sent.size})" }.orEmpty()}") },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { chosenTab = 1 },
                    text = { Text("Received${content?.let { " (${it.received.size})" }.orEmpty()}") },
                )
            }
        }

        when (val current = state) {
            MyRequestsUiState.Loading -> LoadingState()
            is MyRequestsUiState.Error -> ErrorState(message = current.message, onRetry = viewModel::retry)
            is MyRequestsUiState.Content -> {
                val isSent = selectedTab == 0
                val requests = if (isSent) current.sent else current.received
                if (requests.isEmpty()) {
                    EmptyState(
                        icon = if (isSent) Icons.Filled.ShoppingBag else Icons.Filled.Inventory2,
                        title = if (isSent) "No requests sent" else "No requests received",
                        subtitle = if (isSent) {
                            "When you tap Buy or Make offer on a listing, it shows up here."
                        } else {
                            "Buy requests and offers on your listings show up here."
                        },
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(requests, key = { it.id }) { request ->
                            RequestCard(
                                request = request,
                                isSent = isSent,
                                busy = viewModel.busyRequestId == request.id,
                                onClick = { onOpenListing(request.listingId) },
                                onCancel = { viewModel.cancelRequest(request) },
                                onSellerAction = { action, upiId -> viewModel.performSellerAction(request, action, upiId) },
                                onPay = { payWithGpay(context, request, viewModel::say) },
                                onMarkPaid = { viewModel.markPaid(request) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RequestCard(
    request: PurchaseRequest,
    isSent: Boolean,
    busy: Boolean,
    onClick: () -> Unit,
    onCancel: () -> Unit,
    onSellerAction: (SellerAction, String?) -> Unit,
    onPay: () -> Unit,
    onMarkPaid: () -> Unit,
) {
    var askUpiId by remember { mutableStateOf(false) }
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(request.listingTitle, style = MaterialTheme.typography.titleSmall)
                Text(
                    buildString {
                        append(formatPrice(request.offeredPrice))
                        if (!isSent) append(" · from ${request.buyerName}")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            PurchaseStatusTracker(status = request.status)
            PaymentSection(request, isSent, busy, onPay, onMarkPaid)

            if (isSent) {
                if (request.status == PurchaseStatus.REQUESTED) {
                    OutlinedButton(
                        text = "Cancel request",
                        onClick = onCancel,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                // Once the sale is done, the buyer can review the seller.
                if (request.status == PurchaseStatus.COMPLETED) {
                    ReviewPrompt(ReviewParams(EntityType.PURCHASE_REQUEST, request.id, request.sellerId, "the seller"))
                }
            } else {
                val actions = sellerActionsFor(request.status)
                if (actions.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        actions.forEachIndexed { index, action ->
                            // Accepting is where the seller says how they want to be paid, so it asks first.
                            val onActionClick = { if (action == SellerAction.ACCEPT) askUpiId = true else onSellerAction(action, null) }
                            if (index == 0) {
                                PrimaryButton(
                                    text = action.label,
                                    onClick = onActionClick,
                                    enabled = !busy,
                                    modifier = Modifier.weight(1f),
                                )
                            } else {
                                OutlinedButton(
                                    text = action.label,
                                    onClick = onActionClick,
                                    enabled = !busy,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (askUpiId) {
        AcceptDialog(
            price = request.offeredPrice,
            onConfirm = { upiId ->
                askUpiId = false
                onSellerAction(SellerAction.ACCEPT, upiId)
            },
            onDismiss = { askUpiId = false },
        )
    }
}

/** Opens Google Pay with the seller's UPI id and the agreed price; says so if there is no UPI app to open. */
private fun payWithGpay(context: android.content.Context, request: PurchaseRequest, say: (String) -> Unit) {
    val upiId = request.upiId ?: return
    val link = UpiPayment.buildLink(upiId, request.sellerName, request.offeredPrice, "HomeSajja: ${request.listingTitle}")
    if (UpiPayment.launch(context, link) == UpiLaunchResult.NO_UPI_APP) {
        say("Google Pay isn't installed on this phone. Install it, or pay the seller another way and tap \"I've paid\".")
    }
}

/** How this purchase gets paid: the buyer's GPay/paid buttons, or the seller's view of the payment. */
@Composable
private fun PaymentSection(request: PurchaseRequest, isSent: Boolean, busy: Boolean, onPay: () -> Unit, onMarkPaid: () -> Unit) {
    if (!request.paid && !request.canMarkPaid) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when {
            request.paid -> Text(
                if (isSent) "✓ You marked this as paid." else "✓ Marked as paid.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            isSent -> {
                Text(
                    if (request.upiId != null) "Pay ${formatPrice(request.offeredPrice)} to ${request.sellerName.ifBlank { "the seller" }} (${request.upiId})."
                    else "The seller will collect payment in person.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (request.canPayWithUpi) {
                    PrimaryButton(text = "Pay ${formatPrice(request.offeredPrice)} via GPay", onClick = onPay, enabled = !busy, modifier = Modifier.fillMaxWidth())
                }
                OutlinedButton(text = "I've paid", onClick = onMarkPaid, enabled = !busy, modifier = Modifier.fillMaxWidth())
            }
            else -> {
                Text(
                    if (request.upiId != null) "Buyer pays to ${request.upiId}." else "Payment in person.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedButton(text = "Mark as paid", onClick = onMarkPaid, enabled = !busy, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/** The seller confirms acceptance and can add the UPI id the buyer should pay to (leave empty to be paid in person). */
@Composable
private fun AcceptDialog(price: Long, onConfirm: (String?) -> Unit, onDismiss: () -> Unit) {
    var upiId by remember { mutableStateOf("") }
    val entered = upiId.trim()
    val invalid = entered.isNotEmpty() && !UpiPayment.isValidUpiId(entered)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Accept this request?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("The buyer will pay ${formatPrice(price)}. Add your UPI id so they can pay with Google Pay, or leave it empty to be paid in person.")
                AppTextField(
                    value = upiId,
                    onValueChange = { upiId = it },
                    label = "Your UPI id (optional)",
                    placeholder = "name@bank",
                    isError = invalid,
                    errorMessage = if (invalid) "That doesn't look like a UPI id, e.g. name@okhdfcbank" else null,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(entered.takeIf { it.isNotEmpty() }) }, enabled = !invalid) { Text("Accept") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
