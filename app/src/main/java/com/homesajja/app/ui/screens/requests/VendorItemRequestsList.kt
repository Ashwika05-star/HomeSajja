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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.homesajja.app.ui.components.PillTabs
import com.homesajja.app.ui.components.PrimaryButton
import com.homesajja.app.ui.components.SecondaryButton
import com.homesajja.app.ui.components.StatusBadge
import com.homesajja.app.ui.i18n.VendorStrings
import com.homesajja.app.ui.i18n.isAppHindi
import com.homesajja.app.ui.theme.PriceMedium
import com.homesajja.app.ui.util.formatPrice

@Composable
fun VendorItemRequestsList(
    openRequests: List<ItemRequest>,
    myAcceptedRequests: List<ItemRequest>,
    allRequests: List<ItemRequest>,
    currentVendorId: String?,
    busyRequestId: String?,
    onAcceptRequest: (ItemRequest, note: String?) -> Unit,
    onFulfillRequest: (ItemRequest) -> Unit,
    onChatWithCustomer: (ItemRequest) -> Unit,
    modifier: Modifier = Modifier,
    vendorCity: String = "",
) {
    val isHindi = isAppHindi
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var dateFilter by remember { mutableStateOf<DateFilter>(DateFilter.All) }
    var pendingAccept by remember { mutableStateOf<ItemRequest?>(null) }
    var pendingFulfill by remember { mutableStateOf<ItemRequest?>(null) }
    var acceptNote by remember { mutableStateOf("") }

    val tabs = listOf(
        "${if (isHindi) "खुले अनुरोध" else "Open"} (${openRequests.size})",
        "${if (isHindi) "स्वीकृत" else "Accepted by me"} (${myAcceptedRequests.size})",
        if (isHindi) "सभी" else "All",
    )

    val currentList = when (selectedTab) {
        0 -> openRequests
        1 -> myAcceptedRequests
        else -> allRequests
    }

    Column(modifier = modifier.fillMaxSize()) {
        PillTabs(
            labels = tabs,
            selectedIndex = selectedTab,
            onSelect = { selectedTab = it },
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (vendorCity.isNotBlank() && selectedTab == 0) {
                Text(
                    text = if (isHindi) "📍 $vendorCity में खुले अनुरोध" else "📍 Open in $vendorCity",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                Text(
                    text = "",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            DateFilterDropdown(selected = dateFilter, onSelect = { dateFilter = it })
        }

        if (currentList.isEmpty()) {
            val emptySub = if (selectedTab == 0 && vendorCity.isNotBlank()) {
                if (isHindi) "$vendorCity में अभी कोई नए अनुरोध नहीं हैं।" else "No open requests in $vendorCity right now."
            } else {
                VendorStrings.noItemRequestsVendorSub(isHindi)
            }
            EmptyState(
                icon = Icons.Filled.Inventory2,
                title = VendorStrings.noItemRequestsYet(isHindi),
                subtitle = emptySub,
            )
        } else {
            val visibleRequests = currentList.filter { dateFilter.matches(it.createdAt) }
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
                    items(visibleRequests, key = { it.id }) { req ->
                        VendorItemRequestCard(
                            request = req,
                            currentVendorId = currentVendorId,
                            isBusy = busyRequestId == req.id,
                            onAccept = {
                                acceptNote = ""
                                pendingAccept = req
                            },
                            onFulfill = { pendingFulfill = req },
                            onChat = { onChatWithCustomer(req) },
                        )
                    }
                }
            }
        }
    }

    pendingAccept?.let { req ->
        AlertDialog(
            onDismissRequest = { pendingAccept = null },
            title = { Text(VendorStrings.acceptRequest(isHindi)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(VendorStrings.acceptRequestConfirm(req.title, req.userName, isHindi))
                    OutlinedTextField(
                        value = acceptNote,
                        onValueChange = { acceptNote = it },
                        label = { Text(VendorStrings.vendorNoteLabel(isHindi)) },
                        placeholder = { Text(VendorStrings.vendorNoteHint(isHindi)) },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val note = acceptNote.trim().ifBlank { null }
                        pendingAccept = null
                        onAcceptRequest(req, note)
                    },
                ) {
                    Text(VendorStrings.acceptRequest(isHindi))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingAccept = null }) {
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
                    if (isHindi) "क्या आपने यह सामान ग्राहक को सफलतापूर्वक उपलब्ध करा दिया है?"
                    else "Have you delivered or provided this item to the customer?"
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
private fun VendorItemRequestCard(
    request: ItemRequest,
    currentVendorId: String?,
    isBusy: Boolean,
    onAccept: () -> Unit,
    onFulfill: () -> Unit,
    onChat: () -> Unit,
) {
    val isHindi = isAppHindi
    val isMine = currentVendorId != null && request.acceptedVendorId == currentVendorId

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
                    text = "${request.category.displayName.uppercase()} · ${request.city}",
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
                    text = VendorStrings.customerBudget(formatPrice(request.targetBudget), isHindi),
                    style = PriceMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = request.userName,
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        PrimaryButton(
                            text = VendorStrings.acceptRequest(isHindi),
                            onClick = onAccept,
                            enabled = !isBusy,
                        )
                    }
                }
                ItemRequestStatus.ACCEPTED -> {
                    if (isMine) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "✓ ${VendorStrings.acceptedByYou(isHindi)}",
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
                                text = VendorStrings.chatWithBuyer(isHindi),
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
                    } else {
                        Text(
                            text = VendorStrings.acceptedByOther(request.acceptedVendorName.orEmpty(), isHindi),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
