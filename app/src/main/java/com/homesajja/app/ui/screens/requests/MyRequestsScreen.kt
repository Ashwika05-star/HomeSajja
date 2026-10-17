package com.homesajja.app.ui.screens.requests

import androidx.compose.foundation.BorderStroke
import com.homesajja.app.ui.components.PillTabs
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
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.ui.Alignment
import com.homesajja.app.ui.components.DateFilter
import com.homesajja.app.ui.components.DateFilterDropdown
import com.homesajja.app.ui.components.NoResultsState
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
import com.homesajja.app.ui.i18n.VendorStrings
import com.homesajja.app.ui.i18n.isAppHindi
import com.homesajja.app.ui.theme.PriceMedium
import com.homesajja.app.ui.util.formatPrice
import com.homesajja.app.data.model.PaymentMethod
import com.homesajja.app.payment.UpiPayment
import com.homesajja.app.payment.paymentOpen
import com.homesajja.app.data.model.CancelContext
import com.homesajja.app.data.model.CancelReason
import com.homesajja.app.payment.refundRequired
import com.homesajja.app.ui.components.CancellationCard
import com.homesajja.app.ui.components.PaymentPanel
import com.homesajja.app.ui.components.VendorCancelDialog
import com.homesajja.app.ui.components.ReviewPrompt
import com.homesajja.app.viewmodel.ReviewParams
import com.homesajja.app.data.model.EntityType
import com.homesajja.app.ui.components.PurchaseStatusTracker
import com.homesajja.app.viewmodel.MyRequestsUiState
import com.homesajja.app.viewmodel.MyRequestsViewModel
import com.homesajja.app.viewmodel.SellerAction
import com.homesajja.app.viewmodel.sellerActionsFor

import com.homesajja.app.viewmodel.ItemRequestsUiState
import com.homesajja.app.viewmodel.ItemRequestsViewModel

/** Buy/Sell requests and custom item requests. */
@Composable
fun MyRequestsScreen(
    snackbarHostState: SnackbarHostState,
    onOpenListing: (String) -> Unit,
    onOpenChat: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    receivedOnly: Boolean = false,
    includeItemRequests: Boolean = !receivedOnly,
    initialTab: Int = 0,
) {
    val isHindi = isAppHindi
    val container = LocalAppContainer.current
    val viewModel: MyRequestsViewModel = viewModel(factory = ViewModelFactory(container))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var chosenTab by rememberSaveable { mutableIntStateOf(initialTab) }
    val selectedTab = if (receivedOnly) 1 else chosenTab
    var dateFilter by remember { mutableStateOf<DateFilter>(DateFilter.All) }

    val itemRequestsVm: ItemRequestsViewModel? = if (includeItemRequests) {
        viewModel(factory = ViewModelFactory(container))
    } else null
    val itemState = itemRequestsVm?.uiState?.collectAsStateWithLifecycle()?.value
    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }
    if (itemRequestsVm != null) {
        LaunchedEffect(itemRequestsVm) {
            itemRequestsVm.messages.collect { snackbarHostState.showSnackbar(it) }
        }
        LaunchedEffect(itemRequestsVm) {
            itemRequestsVm.openChat.collect { onOpenChat(it) }
        }
    }

    // Coming back from another screen (detail, sell, exchange...): reload if the data is old.
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshIfStale()
        itemRequestsVm?.refresh()
        onPauseOrDispose {}
    }

    Column(modifier = modifier.fillMaxSize()) {
        val content = state as? MyRequestsUiState.Content
        if (!receivedOnly) {
            val userReqsCount = (itemState as? ItemRequestsUiState.Content)?.userRequests?.size
            val labels = if (includeItemRequests) {
                listOf(
                    "${VendorStrings.sent(isHindi)}${content?.let { " (${it.sent.size})" }.orEmpty()}",
                    "${VendorStrings.received(isHindi)}${content?.let { " (${it.received.size})" }.orEmpty()}",
                    "${VendorStrings.itemRequests(isHindi)}${userReqsCount?.let { " ($it)" }.orEmpty()}",
                )
            } else {
                listOf(
                    "${VendorStrings.sent(isHindi)}${content?.let { " (${it.sent.size})" }.orEmpty()}",
                    "${VendorStrings.received(isHindi)}${content?.let { " (${it.received.size})" }.orEmpty()}",
                )
            }
            PillTabs(
                labels = labels,
                selectedIndex = selectedTab,
                onSelect = { chosenTab = it },
            )
        }

        if (includeItemRequests && selectedTab == 2 && itemRequestsVm != null) {
            when (val itCurrent = itemState) {
                ItemRequestsUiState.Loading, null -> LoadingState()
                is ItemRequestsUiState.Error -> ErrorState(message = itCurrent.message, onRetry = itemRequestsVm::retry)
                is ItemRequestsUiState.Content -> {
                    UserItemRequestsList(
                        requests = itCurrent.userRequests,
                        busyRequestId = itemRequestsVm.busyRequestId,
                        onRequestNew = { showCreateDialog = true },
                        onCancelRequest = { req -> itemRequestsVm.cancelRequest(req) },
                        onFulfillRequest = { req -> itemRequestsVm.fulfillRequest(req) },
                        onChatWithVendor = { req -> itemRequestsVm.chatWithUser(req) },
                    )
                }
            }
        } else {
            when (val current = state) {
                MyRequestsUiState.Loading -> LoadingState()
                is MyRequestsUiState.Error -> ErrorState(message = current.message, onRetry = viewModel::retry)
                is MyRequestsUiState.Content -> {
                    val isSent = selectedTab == 0
                    val requests = if (isSent) current.sent else current.received
                    if (requests.isEmpty()) {
                        EmptyState(
                            icon = if (isSent) Icons.Filled.ShoppingBag else Icons.Filled.Inventory2,
                            title = if (isSent) VendorStrings.noSentPurchases(isHindi) else VendorStrings.noReceivedPurchases(isHindi),
                            subtitle = if (isSent) {
                                VendorStrings.noSentPurchasesSub(isHindi)
                            } else {
                                VendorStrings.noReceivedPurchasesSub(isHindi)
                            },
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            DateFilterDropdown(selected = dateFilter, onSelect = { dateFilter = it })
                        }
                        val visibleRequests = requests.filter { dateFilter.matches(it.createdAt) }
                        if (visibleRequests.isEmpty()) {
                            NoResultsState(
                                icon = Icons.Filled.FilterList,
                                title = if (isHindi) "इस अवधि में कोई अनुरोध नहीं मिला" else "No requests for this period",
                                subtitle = if (isHindi) "कृपया कोई अन्य तारीख चुनें" else "Try selecting another time range or clear the filter.",
                                actionLabel = if (isHindi) "सभी समय देखें" else "Show all time",
                                onActionClick = { dateFilter = DateFilter.All },
                            )
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                items(visibleRequests, key = { it.id }) { request ->
                                    RequestCard(
                                        request = request,
                                        isSent = isSent,
                                        busy = viewModel.busyRequestId == request.id,
                                        onClick = { onOpenListing(request.listingId) },
                                        onCancel = { viewModel.cancelRequest(request) },
                                        onSellerAction = { action, upiId -> viewModel.performSellerAction(request, action, upiId) },
                                        onCancelOrder = { reason, note, refundDone -> viewModel.cancelOrder(request, reason, note, refundDone) },
                                        myUpiId = viewModel.myUpiId,
                                        onMarkPaid = { method, ref -> viewModel.markPaid(request, method, ref) },
                                        onConfirmReceived = { viewModel.confirmPayment(request) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog && itemRequestsVm != null) {
        val initialCity = (itemState as? ItemRequestsUiState.Content)?.userCity.orEmpty()
        ItemRequestCreateDialog(
            initialCity = initialCity,
            onDismiss = { showCreateDialog = false },
            onSubmit = { title, category, budget, description, city ->
                showCreateDialog = false
                itemRequestsVm.createRequest(title, category, budget, description, city) {}
            },
        )
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
    onCancelOrder: (CancelReason, String, Boolean) -> Unit,
    myUpiId: String?,
    onMarkPaid: (PaymentMethod, String?) -> Unit,
    onConfirmReceived: () -> Unit,
) {
    var askUpiId by remember { mutableStateOf(false) }
    var askCancel by remember { mutableStateOf(false) }
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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
                    style = PriceMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            PurchaseStatusTracker(status = request.status)
            request.cancellation?.let {
                CancellationCard(
                    vendorName = request.sellerName.ifBlank { "the seller" },
                    cancellation = it,
                    agreedAmount = request.agreedAmount,
                    payment = request.payment,
                    refund = request.refund,
                    viewerIsPayer = isSent,
                )
            }
            val payment = request.payment
            if (payment != null && request.paymentOpen) {
                PaymentPanel(
                    amount = request.agreedAmount ?: request.offeredPrice,
                    payment = payment,
                    viewerId = if (isSent) request.buyerId else request.sellerId,
                    payerName = request.buyerName,
                    payeeName = request.sellerName.ifBlank { "the seller" },
                    paymentNote = "HomeSajja: ${request.listingTitle}",
                    cashLabel = "Cash on pickup",
                    busy = busy,
                    onMarkPaid = onMarkPaid,
                    onConfirmReceived = onConfirmReceived,
                )
            }

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
                            val onActionClick = {
                                when (action) {
                                    SellerAction.ACCEPT -> askUpiId = true
                                    SellerAction.CANCEL_ORDER -> askCancel = true
                                    else -> onSellerAction(action, null)
                                }
                            }
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

    if (askCancel) {
        VendorCancelDialog(
            context = CancelContext.PURCHASE,
            title = "Cancel this order?",
            otherName = request.buyerName,
            // The seller confirmed receiving the buyer's payment: they must refund it before the cancel can go through.
            refundAmount = if (refundRequired(request.payment, request.sellerId)) request.agreedAmount ?: request.offeredPrice else null,
            onConfirm = { reason, note, refundDone ->
                askCancel = false
                onCancelOrder(reason, note, refundDone)
            },
            onDismiss = { askCancel = false },
        )
    }

    if (askUpiId) {
        AcceptDialog(
            price = request.offeredPrice,
            initialUpiId = myUpiId,
            onConfirm = { upiId ->
                askUpiId = false
                onSellerAction(SellerAction.ACCEPT, upiId)
            },
            onDismiss = { askUpiId = false },
        )
    }
}

/**
 * The seller confirms acceptance, which fixes the price the buyer will pay, and can say where to be paid by UPI
 * (pre-filled from their profile; leave it empty to be paid in cash).
 */
@Composable
private fun AcceptDialog(price: Long, initialUpiId: String?, onConfirm: (String?) -> Unit, onDismiss: () -> Unit) {
    var upiId by remember(initialUpiId) { mutableStateOf(initialUpiId.orEmpty()) }
    val entered = upiId.trim()
    val invalid = entered.isNotEmpty() && !UpiPayment.isValidUpiId(entered)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Accept this request?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("The price is fixed at ${formatPrice(price)} once you accept. Add your UPI ID so the buyer can pay by UPI, or leave it empty to be paid in cash.")
                AppTextField(
                    value = upiId,
                    onValueChange = { upiId = it },
                    label = "Your UPI ID (optional)",
                    placeholder = "name@bank",
                    isError = invalid,
                    errorMessage = if (invalid) "That doesn't look like a UPI ID, e.g. name@okhdfcbank" else null,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(entered.takeIf { it.isNotEmpty() }) }, enabled = !invalid) { Text("Accept") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
