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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
) {
    val viewModel: MyRequestsViewModel = viewModel(factory = ViewModelFactory(LocalAppContainer.current))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

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
        TabRow(selectedTabIndex = selectedTab, containerColor = MaterialTheme.colorScheme.background) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Sent${content?.let { " (${it.sent.size})" }.orEmpty()}") },
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Received${content?.let { " (${it.received.size})" }.orEmpty()}") },
            )
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
                                onSellerAction = { viewModel.performSellerAction(request, it) },
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
    onSellerAction: (SellerAction) -> Unit,
) {
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

            if (isSent) {
                if (request.status == PurchaseStatus.REQUESTED) {
                    OutlinedButton(
                        text = "Cancel request",
                        onClick = onCancel,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                val actions = sellerActionsFor(request.status)
                if (actions.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        actions.forEachIndexed { index, action ->
                            if (index == 0) {
                                PrimaryButton(
                                    text = action.label,
                                    onClick = { onSellerAction(action) },
                                    enabled = !busy,
                                    modifier = Modifier.weight(1f),
                                )
                            } else {
                                OutlinedButton(
                                    text = action.label,
                                    onClick = { onSellerAction(action) },
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
}
