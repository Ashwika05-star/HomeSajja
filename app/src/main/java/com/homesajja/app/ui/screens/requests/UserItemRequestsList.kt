package com.homesajja.app.ui.screens.requests

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.homesajja.app.data.model.ItemRequest
import com.homesajja.app.data.model.ItemRequestStatus
import com.homesajja.app.ui.components.DateFilter
import com.homesajja.app.ui.components.DateFilterDropdown
import com.homesajja.app.ui.components.EmptyState
import com.homesajja.app.ui.components.NoResultsState
import com.homesajja.app.ui.components.OutlinedButton
import com.homesajja.app.ui.components.PrimaryButton
import com.homesajja.app.ui.components.SecondaryButton
import com.homesajja.app.ui.components.StatusBadge
import com.homesajja.app.ui.i18n.VendorStrings
import com.homesajja.app.ui.i18n.isAppHindi
import com.homesajja.app.ui.theme.PriceMedium
import com.homesajja.app.ui.util.formatPrice

@Composable
fun UserItemRequestsList(
    requests: List<ItemRequest>,
    busyRequestId: String?,
    onRequestNew: () -> Unit,
    onCancelRequest: (ItemRequest) -> Unit,
    onFulfillRequest: (ItemRequest) -> Unit,
    onChatWithVendor: (ItemRequest) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isHindi = isAppHindi
    var dateFilter by remember { mutableStateOf<DateFilter>(DateFilter.All) }
    var pendingCancel by remember { mutableStateOf<ItemRequest?>(null) }
    var pendingFulfill by remember { mutableStateOf<ItemRequest?>(null) }

    if (requests.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.Inventory2,
            title = VendorStrings.noItemRequestsYet(isHindi),
            subtitle = VendorStrings.noItemRequestsUserSub(isHindi),
            actionLabel = VendorStrings.requestItem(isHindi),
            onActionClick = onRequestNew,
            modifier = modifier,
        )
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (isHindi) "मेरे अनुरोध" else "My Item Requests",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp),
                )
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
                    contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(visibleRequests, key = { it.id }) { req ->
                        UserItemRequestCard(
                            request = req,
                            isBusy = busyRequestId == req.id,
                            onCancel = { pendingCancel = req },
                            onFulfill = { pendingFulfill = req },
                            onChat = { onChatWithVendor(req) },
                        )
                    }
                }
            }
        }

        // Request Item FAB - strictly inside Item Requested tab
        androidx.compose.material3.ExtendedFloatingActionButton(
            onClick = onRequestNew,
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text(VendorStrings.requestItem(isHindi)) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        )
    }

    pendingCancel?.let { req ->
        AlertDialog(
            onDismissRequest = { pendingCancel = null },
            title = { Text(VendorStrings.cancelRequest(isHindi)) },
            text = { Text(VendorStrings.cancelRequestConfirm(req.title, isHindi)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingCancel = null
                        onCancelRequest(req)
                    },
                ) {
                    Text(VendorStrings.cancelRequest(isHindi), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingCancel = null }) {
                    Text(VendorStrings.cancel(isHindi))
                }
            },
        )
    }

    pendingFulfill?.let { req ->
        AlertDialog(
            onDismissRequest = { pendingFulfill = null },
            title = { Text(VendorStrings.markFulfilled(isHindi)) },
            text = {
                Text(
                    if (isHindi) "क्या आपको यह सामान प्राप्त हो गया है और अनुरोध पूरा हो गया है?"
                    else "Have you received this item and completed the request?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingFulfill = null
                        onFulfillRequest(req)
                    },
                ) {
                    Text(VendorStrings.markFulfilled(isHindi))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingFulfill = null }) {
                    Text(VendorStrings.cancel(isHindi))
                }
            },
        )
    }
}

@Composable
private fun UserItemRequestCard(
    request: ItemRequest,
    isBusy: Boolean,
    onCancel: () -> Unit,
    onFulfill: () -> Unit,
    onChat: () -> Unit,
) {
    val isHindi = isAppHindi
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = request.category.displayName.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                StatusBadge(status = request.status.localizedLabel(isHindi))
            }

            Text(
                text = request.title,
                style = MaterialTheme.typography.titleMedium,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = VendorStrings.yourBudget(formatPrice(request.targetBudget), isHindi),
                    style = PriceMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = request.city,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (request.description.isNotBlank()) {
                Text(
                    text = request.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            when (request.status) {
                ItemRequestStatus.OPEN -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = VendorStrings.waitingForVendor(isHindi),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = onCancel, enabled = !isBusy) {
                            Text(VendorStrings.cancelRequest(isHindi), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                ItemRequestStatus.ACCEPTED -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "✓ ${VendorStrings.acceptedByVendor(request.acceptedVendorName.orEmpty(), isHindi)}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            if (!request.vendorNote.isNullOrBlank()) {
                                Text(
                                    text = "\"${request.vendorNote}\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PrimaryButton(
                            text = VendorStrings.chatWithVendor(isHindi),
                            onClick = onChat,
                            enabled = !isBusy,
                            modifier = Modifier.weight(1f),
                        )
                        SecondaryButton(
                            text = VendorStrings.markFulfilled(isHindi),
                            onClick = onFulfill,
                            enabled = !isBusy,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                ItemRequestStatus.FULFILLED -> {
                    Text(
                        text = "✓ ${VendorStrings.completed(isHindi)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                ItemRequestStatus.CANCELLED -> {
                    Text(
                        text = VendorStrings.cancel(isHindi),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
