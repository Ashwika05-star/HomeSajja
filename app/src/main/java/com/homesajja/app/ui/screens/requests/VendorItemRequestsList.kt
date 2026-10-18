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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.homesajja.app.data.model.ItemRequest
import com.homesajja.app.data.model.ItemRequestStatus
import com.homesajja.app.ui.components.AppTextField
import com.homesajja.app.ui.components.DateFilter
import com.homesajja.app.ui.components.DateFilterDropdown
import com.homesajja.app.ui.components.EmptyState
import com.homesajja.app.ui.components.NoResultsState
import com.homesajja.app.ui.components.OutlinedButton
import com.homesajja.app.ui.components.PaymentPanel
import com.homesajja.app.ui.components.PillTabs
import com.homesajja.app.ui.components.PrimaryButton
import com.homesajja.app.ui.components.SecondaryButton
import com.homesajja.app.ui.components.StatusBadge
import com.homesajja.app.ui.i18n.VendorStrings
import com.homesajja.app.ui.i18n.isAppHindi
import com.homesajja.app.ui.theme.PriceLarge
import com.homesajja.app.ui.theme.PriceMedium
import com.homesajja.app.ui.util.formatPrice

@Composable
fun VendorItemRequestsList(
    openRequests: List<ItemRequest>,
    myAcceptedRequests: List<ItemRequest>,
    allRequests: List<ItemRequest>,
    currentVendorId: String?,
    vendorUpiId: String? = null,
    busyRequestId: String?,
    onSendQuote: (ItemRequest, amount: Long, estimatedDays: Int?, note: String) -> Unit = { _, _, _, _ -> },
    onUpdateStatus: (ItemRequest, ItemRequestStatus) -> Unit = { _, _ -> },
    onConfirmPayment: (ItemRequest) -> Unit = {},
    onAcceptRequest: (ItemRequest, note: String?) -> Unit = { _, _ -> },
    onFulfillRequest: (ItemRequest) -> Unit = {},
    onChatWithCustomer: (ItemRequest) -> Unit,
    modifier: Modifier = Modifier,
    vendorCity: String = "",
) {
    val isHindi = isAppHindi
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var dateFilter by remember { mutableStateOf<DateFilter>(DateFilter.All) }
    var pendingQuote by remember { mutableStateOf<ItemRequest?>(null) }

    val tabs = listOf(
        "${if (isHindi) "खुले अनुरोध" else "Open"} (${openRequests.size})",
        "${if (isHindi) "मेरा काम" else "My work"} (${myAcceptedRequests.size})",
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
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(visibleRequests, key = { it.id }) { req ->
                        VendorItemRequestCard(
                            request = req,
                            currentVendorId = currentVendorId,
                            isBusy = busyRequestId == req.id,
                            onQuote = { pendingQuote = req },
                            onUpdateStatus = { status -> onUpdateStatus(req, status) },
                            onConfirmPayment = { onConfirmPayment(req) },
                            onChat = { onChatWithCustomer(req) },
                        )
                    }
                }
            }
        }
    }

    pendingQuote?.let { req ->
        ItemRequestQuoteDialog(
            request = req,
            vendorUpiId = vendorUpiId,
            onDismiss = { pendingQuote = null },
            onSend = { amount, days, note ->
                pendingQuote = null
                onSendQuote(req, amount, days, note)
            },
        )
    }
}

@Composable
private fun VendorItemRequestCard(
    request: ItemRequest,
    currentVendorId: String?,
    isBusy: Boolean,
    onQuote: () -> Unit,
    onUpdateStatus: (ItemRequestStatus) -> Unit,
    onConfirmPayment: () -> Unit,
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
                    modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp),
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
                            text = VendorStrings.sendQuote(isHindi),
                            onClick = onQuote,
                            enabled = !isBusy,
                        )
                    }
                }

                ItemRequestStatus.QUOTED -> {
                    if (isMine) {
                        val quote = request.quote
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "✓ ${if (isHindi) "कोटेशन भेजा गया" else "Quote sent"}: ${formatPrice(quote?.amount ?: 0L)}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                                quote?.estimatedDays?.let { days ->
                                    Text(
                                        text = VendorStrings.estimatedDays(days, isHindi),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    )
                                }
                                Text(
                                    text = if (isHindi) "ग्राहक द्वारा स्वीकार या अस्वीकार किए जाने की प्रतीक्षा है..."
                                    else "Waiting for customer to accept or decline...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            OutlinedButton(
                                text = VendorStrings.chatWithBuyer(isHindi),
                                onClick = onChat,
                                enabled = !isBusy,
                            )
                        }
                    } else {
                        Text(
                            text = if (isHindi) "किसी अन्य विक्रेता द्वारा कोटेशन भेजा गया है।" else "Quoted by another vendor.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                ItemRequestStatus.AGREED, ItemRequestStatus.ACCEPTED,
                ItemRequestStatus.IN_PROGRESS, ItemRequestStatus.READY,
                ItemRequestStatus.COMPLETED, ItemRequestStatus.FULFILLED -> {
                    if (isMine) {
                        val finalAmount = request.agreedAmount ?: request.quote?.amount ?: request.targetBudget
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "✓ ${VendorStrings.agreedPrice(isHindi)}: ${formatPrice(finalAmount)}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp),
                                    )
                                    StatusBadge(status = request.status.localizedLabel(isHindi))
                                }
                                if (!request.vendorNote.isNullOrBlank()) {
                                    Text(
                                        text = "\"${request.vendorNote}\"",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    )
                                }
                            }
                        }

                        // Payment Panel
                        val payment = request.payment
                        if (payment != null && request.paymentOpen) {
                            PaymentPanel(
                                amount = finalAmount,
                                payment = payment,
                                viewerId = currentVendorId,
                                payerName = request.userName,
                                payeeName = request.acceptedVendorName ?: "You",
                                paymentNote = "HomeSajja custom item: ${request.title}",
                                cashLabel = "Cash on delivery",
                                busy = isBusy,
                                onMarkPaid = { _, _ -> },
                                onConfirmReceived = onConfirmPayment,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        // Workflow progress buttons
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            when (request.status) {
                                ItemRequestStatus.AGREED, ItemRequestStatus.ACCEPTED -> {
                                    PrimaryButton(
                                        text = VendorStrings.startWork(isHindi),
                                        onClick = { onUpdateStatus(ItemRequestStatus.IN_PROGRESS) },
                                        enabled = !isBusy,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                                ItemRequestStatus.IN_PROGRESS -> {
                                    PrimaryButton(
                                        text = VendorStrings.markReady(isHindi),
                                        onClick = { onUpdateStatus(ItemRequestStatus.READY) },
                                        enabled = !isBusy,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                                ItemRequestStatus.READY -> {
                                    PrimaryButton(
                                        text = VendorStrings.markCompleted(isHindi),
                                        onClick = { onUpdateStatus(ItemRequestStatus.COMPLETED) },
                                        enabled = !isBusy,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                                else -> Unit
                            }
                            OutlinedButton(
                                text = VendorStrings.chatWithBuyer(isHindi),
                                onClick = onChat,
                                enabled = !isBusy,
                                modifier = Modifier.fillMaxWidth(),
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

                ItemRequestStatus.CANCELLED -> {
                    Text(
                        text = "✕ ${VendorStrings.cancel(isHindi)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun ItemRequestQuoteDialog(
    request: ItemRequest,
    vendorUpiId: String?,
    onDismiss: () -> Unit,
    onSend: (amount: Long, estimatedDays: Int?, note: String) -> Unit,
) {
    val isHindi = isAppHindi
    var amountText by remember { mutableStateOf(if (request.targetBudget > 0) request.targetBudget.toString() else "") }
    var daysText by remember { mutableStateOf("3") }
    var noteText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(VendorStrings.quoteDialogTitle(isHindi)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = VendorStrings.quoteDialogDesc(isHindi),
                    style = MaterialTheme.typography.bodyMedium,
                )

                AppTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it.filter(Char::isDigit).take(8)
                        errorMessage = null
                    },
                    label = VendorStrings.quoteAmountLabel(isHindi),
                    placeholder = "e.g. 4500",
                    keyboardType = KeyboardType.Number,
                )

                AppTextField(
                    value = daysText,
                    onValueChange = {
                        daysText = it.filter(Char::isDigit).take(3)
                        errorMessage = null
                    },
                    label = VendorStrings.quoteDaysLabel(isHindi),
                    placeholder = "e.g. 5",
                    keyboardType = KeyboardType.Number,
                )

                AppTextField(
                    value = noteText,
                    onValueChange = {
                        noteText = it.take(300)
                        errorMessage = null
                    },
                    label = VendorStrings.quoteNoteLabel(isHindi),
                    placeholder = "e.g. Can craft in solid sheesham with dark teak polish",
                    singleLine = false,
                    minLines = 2,
                )

                if (!vendorUpiId.isNullOrBlank()) {
                    Text(
                        text = if (isHindi) "ग्राहक आपको $vendorUpiId (आपकी प्रोफ़ाइल से) पर UPI से भुगतान कर सकेंगे।"
                        else "Customers will be able to pay you at $vendorUpiId (from your profile).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = if (isHindi) "आपने अभी तक UPI ID नहीं जोड़ा है, इसलिए ग्राहक केवल नकद भुगतान देख सकेंगे।"
                        else "You haven't saved a UPI ID, so customers will only see cash on delivery.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                errorMessage?.let {
                    Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = amountText.toLongOrNull()
                    if (amount == null || amount <= 0L) {
                        errorMessage = if (isHindi) "कृपया एक मान्य राशि दर्ज करें" else "Please enter a valid amount"
                        return@TextButton
                    }
                    val days = daysText.toIntOrNull()
                    if (days == null || days <= 0) {
                        errorMessage = if (isHindi) "कृपया अनुमानित दिन दर्ज करें" else "Please enter valid estimated days"
                        return@TextButton
                    }
                    onSend(amount, days, noteText.trim())
                },
            ) {
                Text(VendorStrings.sendQuote(isHindi))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(VendorStrings.cancel(isHindi))
            }
        },
    )
}
