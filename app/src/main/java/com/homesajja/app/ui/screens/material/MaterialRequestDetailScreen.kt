package com.homesajja.app.ui.screens.material

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.homesajja.app.data.model.MaterialOffer
import com.homesajja.app.data.model.MaterialRequest
import com.homesajja.app.data.model.MaterialRequestStatus
import com.homesajja.app.data.model.OfferStatus
import com.homesajja.app.di.LocalAppContainer
import com.homesajja.app.di.ViewModelFactory
import com.homesajja.app.ui.components.AppTopBar
import com.homesajja.app.ui.components.ErrorState
import com.homesajja.app.ui.components.ItemThumbnail
import com.homesajja.app.ui.components.LoadingState
import com.homesajja.app.ui.components.OutlinedButton
import com.homesajja.app.ui.components.PrimaryButton
import com.homesajja.app.ui.components.StatusBadge
import com.homesajja.app.ui.util.budgetLabel
import com.homesajja.app.viewmodel.MaterialDetailUiState
import com.homesajja.app.viewmodel.MaterialRequestDetailViewModel
import com.homesajja.app.viewmodel.OfferChooserState

/**
 * One material request. The vendor who posted it sees the offers (accept / decline) and can edit, close or
 * delete it; other people see it and can offer one of their own listings.
 */
@Composable
fun MaterialRequestDetailScreen(onBackClick: () -> Unit, onEdit: (String) -> Unit, onOpenVendor: (String) -> Unit) {
    val viewModel: MaterialRequestDetailViewModel = viewModel(factory = ViewModelFactory(LocalAppContainer.current))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val chooser by viewModel.chooser.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }
    LaunchedEffect(viewModel) {
        viewModel.deleted.collect { onBackClick() }
    }

    Scaffold(
        topBar = { AppTopBar(title = "Material request", onBackClick = onBackClick) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val current = state) {
                MaterialDetailUiState.Loading -> LoadingState()
                is MaterialDetailUiState.Error -> ErrorState(message = current.message, onRetry = viewModel::retry)
                is MaterialDetailUiState.Content -> DetailContent(current, viewModel, onEdit, onOpenVendor, onDelete = { confirmDelete = true })
            }
        }
    }

    OfferChooserDialog(chooser, onPick = viewModel::offer, onDismiss = viewModel::closeChooser)

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete request?") },
            text = { Text("The request and its offers will no longer be reachable.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.deleteRequest()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun DetailContent(
    content: MaterialDetailUiState.Content,
    viewModel: MaterialRequestDetailViewModel,
    onEdit: (String) -> Unit,
    onOpenVendor: (String) -> Unit,
    onDelete: () -> Unit,
) {
    val request = content.request
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        RequestSummary(request, onOpenVendor = if (content.isOwner) null else ({ onOpenVendor(request.vendorId) }))

        if (content.isOwner) {
            OwnerSection(content, viewModel, onEdit = { onEdit(request.id) }, onDelete = onDelete)
        } else {
            OffererSection(content, viewModel)
        }
    }
}

@Composable
private fun RequestSummary(request: MaterialRequest, onOpenVendor: (() -> Unit)?) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(request.title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                StatusBadge(status = request.status.displayName)
            }
            Text("${request.materialType.displayName} · ${request.quantity}", style = MaterialTheme.typography.bodyLarge)
            budgetLabel(request.budgetMin, request.budgetMax)?.let {
                Text("Budget $it", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
            }
            Text(request.description, style = MaterialTheme.typography.bodyMedium)
            Text(
                "${request.vendorName} · ${request.city}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (onOpenVendor != null) TextButton(onClick = onOpenVendor) { Text("View vendor profile") }
        }
    }
}

// ---------------- the vendor who posted it ----------------

@Composable
private fun OwnerSection(
    content: MaterialDetailUiState.Content,
    viewModel: MaterialRequestDetailViewModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val request = content.request
    Text("Offers (${content.offers.size})", style = MaterialTheme.typography.titleMedium)
    if (content.offers.isEmpty()) {
        Text(
            "No offers yet. People in ${request.city} can offer their furniture against this request.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    content.offers.forEach { offer ->
        OfferCard(
            offer = offer,
            busy = content.isBusy,
            showActions = true,
            onAccept = { viewModel.decideOffer(offer, OfferStatus.ACCEPTED) },
            onDecline = { viewModel.decideOffer(offer, OfferStatus.DECLINED) },
        )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(text = "Edit", onClick = onEdit, enabled = !content.isBusy, modifier = Modifier.weight(1f))
        OutlinedButton(text = "Delete", onClick = onDelete, enabled = !content.isBusy, modifier = Modifier.weight(1f))
    }
    when (request.status) {
        MaterialRequestStatus.OPEN -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            PrimaryButton(
                text = "Mark fulfilled",
                onClick = { viewModel.setRequestStatus(MaterialRequestStatus.FULFILLED) },
                enabled = !content.isBusy,
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(
                text = "Close",
                onClick = { viewModel.setRequestStatus(MaterialRequestStatus.CLOSED) },
                enabled = !content.isBusy,
                modifier = Modifier.weight(1f),
            )
        }
        MaterialRequestStatus.FULFILLED, MaterialRequestStatus.CLOSED -> OutlinedButton(
            text = "Reopen",
            onClick = { viewModel.setRequestStatus(MaterialRequestStatus.OPEN) },
            enabled = !content.isBusy,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun OfferCard(offer: MaterialOffer, busy: Boolean, showActions: Boolean, onAccept: () -> Unit, onDecline: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ItemThumbnail(
                    imageUrl = offer.listingImage,
                    description = offer.listingTitle,
                    modifier = Modifier.width(88.dp).clip(RoundedCornerShape(10.dp)),
                )
                Column(modifier = Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(offer.listingTitle, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("From ${offer.offererName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                StatusBadge(status = offer.status.displayName)
            }
            if (showActions && offer.status == OfferStatus.PENDING) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(text = "Decline", onClick = onDecline, enabled = !busy, modifier = Modifier.weight(1f))
                    PrimaryButton(text = "Accept", onClick = onAccept, enabled = !busy, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// ---------------- anyone else ----------------

@Composable
private fun OffererSection(content: MaterialDetailUiState.Content, viewModel: MaterialRequestDetailViewModel) {
    val offer = content.myOffer
    when {
        offer != null -> {
            Text("Your offer", style = MaterialTheme.typography.titleMedium)
            OfferCard(offer, busy = false, showActions = false, onAccept = {}, onDecline = {})
            if (offer.status == OfferStatus.PENDING) {
                OutlinedButton(text = "Withdraw offer", onClick = viewModel::withdrawOffer, enabled = !content.isBusy, modifier = Modifier.fillMaxWidth())
            }
        }
        content.request.status != MaterialRequestStatus.OPEN -> Text(
            "This request is no longer open.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        else -> PrimaryButton(text = "Offer furniture", onClick = viewModel::openChooser, enabled = !content.isBusy, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun OfferChooserDialog(state: OfferChooserState, onPick: (com.homesajja.app.data.model.FurnitureListing) -> Unit, onDismiss: () -> Unit) {
    if (state == OfferChooserState.Closed) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose an item to offer") },
        text = {
            when (state) {
                OfferChooserState.Loading -> CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                is OfferChooserState.Error -> Text(state.message, color = MaterialTheme.colorScheme.error)
                is OfferChooserState.Choosing -> {
                    if (state.listings.isEmpty()) {
                        Text("You have no active listings to offer. List an item first.")
                    } else {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            state.listings.forEach { listing ->
                                Card(
                                    onClick = { onPick(listing) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        ItemThumbnail(
                                            imageUrl = listing.images.firstOrNull(),
                                            description = listing.title,
                                            modifier = Modifier.width(72.dp).clip(RoundedCornerShape(8.dp)),
                                        )
                                        Text(listing.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 12.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                OfferChooserState.Closed -> Unit
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
