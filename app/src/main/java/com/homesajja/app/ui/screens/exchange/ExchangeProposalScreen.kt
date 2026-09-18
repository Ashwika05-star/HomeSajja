package com.homesajja.app.ui.screens.exchange

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.homesajja.app.data.model.FurnitureListing
import com.homesajja.app.di.LocalAppContainer
import com.homesajja.app.di.ViewModelFactory
import com.homesajja.app.ui.components.AppTextField
import com.homesajja.app.ui.components.AppTopBar
import com.homesajja.app.ui.components.EmptyState
import com.homesajja.app.ui.components.ErrorState
import com.homesajja.app.ui.components.InlineErrorBanner
import com.homesajja.app.ui.components.ItemComparison
import com.homesajja.app.ui.components.ItemThumbnail
import com.homesajja.app.ui.components.LoadingState
import com.homesajja.app.ui.components.OutlinedButton
import com.homesajja.app.ui.components.PrimaryButton
import com.homesajja.app.ui.screens.explore.ListingBrowser
import com.homesajja.app.ui.util.priceLabel
import com.homesajja.app.viewmodel.ExchangeBrowseViewModel
import com.homesajja.app.viewmodel.ExchangeProposalViewModel
import com.homesajja.app.viewmodel.ExchangeStep
import com.homesajja.app.viewmodel.MAX_EXCHANGE_MESSAGE_LENGTH
import com.homesajja.app.viewmodel.MyFurnitureState
import com.homesajja.app.viewmodel.SendState
import com.homesajja.app.viewmodel.WantedItemState

/** Propose an exchange in three steps: your furniture, the furniture you want, review & send. */
@Composable
fun ExchangeProposalScreen(
    onExit: () -> Unit,
    onSent: (requestId: String) -> Unit,
    onSell: () -> Unit,
) {
    val viewModel: ExchangeProposalViewModel = viewModel(factory = ViewModelFactory(LocalAppContainer.current))
    val sendState = viewModel.sendState

    BackHandler(enabled = sendState !is SendState.Sending) { if (!viewModel.back()) onExit() }
    LaunchedEffect(sendState) {
        if (sendState is SendState.Sent) onSent(sendState.requestId)
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Propose an exchange",
                onBackClick = { if (!viewModel.back()) onExit() },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            StepHeader(viewModel.step)
            Box(modifier = Modifier.weight(1f)) {
                when (viewModel.step) {
                    ExchangeStep.OFFER -> MyFurnitureStep(viewModel, onSell)
                    ExchangeStep.WANT -> WantedStep(viewModel)
                    ExchangeStep.REVIEW -> ReviewStep(viewModel)
                }
            }
        }
    }

    if (sendState is SendState.Sending) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Sending request") },
            text = {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    Text("Just a moment…")
                }
            },
            confirmButton = {},
        )
    }
    if (sendState is SendState.Failed) {
        AlertDialog(
            onDismissRequest = viewModel::dismissSendError,
            title = { Text("Couldn't send") },
            text = { Text(sendState.message) },
            confirmButton = { TextButton(onClick = viewModel::send) { Text("Try again") } },
            dismissButton = { TextButton(onClick = viewModel::dismissSendError) { Text("Close") } },
        )
    }
}

@Composable
private fun StepHeader(step: ExchangeStep) {
    val steps = ExchangeStep.entries
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text(
            "Step ${step.ordinal + 1} of ${steps.size} · ${step.title}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LinearProgressIndicator(
            progress = { (step.ordinal + 1f) / steps.size },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

/** Step 1 — "My Furniture": choose which of my active listings to offer. */
@Composable
private fun MyFurnitureStep(viewModel: ExchangeProposalViewModel, onSell: () -> Unit) {
    // Coming back from listing something new: reload quietly so it appears.
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshMyFurnitureIfStale()
        onPauseOrDispose {}
    }

    // If the flow started from a listing, that listing has to load before we can go on.
    when (val wanted = viewModel.wantedItem) {
        WantedItemState.Loading -> return LoadingState()
        is WantedItemState.Error -> return ErrorState(message = wanted.message, onRetry = { viewModel.loadPreselected() })
        WantedItemState.NotPreselected, WantedItemState.Ready -> Unit
    }

    when (val state = viewModel.myFurniture) {
        MyFurnitureState.Loading -> LoadingState()
        is MyFurnitureState.Error -> ErrorState(message = state.message, onRetry = viewModel::loadMyFurniture)
        is MyFurnitureState.Content -> {
            if (state.items.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.Inventory2,
                    title = "You have nothing to offer yet",
                    subtitle = "List a piece of furniture first, then come back to propose a swap.",
                    actionLabel = "List an item",
                    onActionClick = onSell,
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item {
                        Text(
                            "Choose the item you'd like to offer.",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                    items(state.items, key = { it.id }) { listing ->
                        FurnitureRow(listing = listing, onClick = { viewModel.selectOffered(listing) })
                    }
                }
            }
        }
    }
}

/** Step 2 — browse exchange listings in my city and pick the one I want. */
@Composable
private fun WantedStep(viewModel: ExchangeProposalViewModel) {
    val browseViewModel: ExchangeBrowseViewModel = viewModel(factory = ViewModelFactory(LocalAppContainer.current))
    ListingBrowser(
        viewModel = browseViewModel,
        onListingClick = viewModel::selectRequested,
        emptyTitle = { city -> "No exchange items in $city yet" },
        emptySubtitle = "Nobody has listed furniture for exchange in your city yet. Check back soon.",
        emptyActionLabel = null,
        onEmptyAction = null,
    )
}

/** Step 3 — compare both items, add a message and send. */
@Composable
private fun ReviewStep(viewModel: ExchangeProposalViewModel) {
    val offered = viewModel.offered
    val requested = viewModel.requested
    if (offered == null || requested == null) {
        ErrorState(message = "Something is missing. Please go back and choose both items.", onRetry = { viewModel.back() })
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ItemComparison(
                left = offered.toComparisonItem(),
                right = requested.toComparisonItem(),
                leftLabel = "You offer",
                rightLabel = "You want",
            )
            AppTextField(
                value = viewModel.message,
                onValueChange = viewModel::onMessageChange,
                label = "Message (optional)",
                placeholder = "Say hello, or suggest when to meet",
                singleLine = false,
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "${viewModel.message.length}/$MAX_EXCHANGE_MESSAGE_LENGTH",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (viewModel.sendState is SendState.Failed) {
                InlineErrorBanner(message = (viewModel.sendState as SendState.Failed).message)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(text = "Back", onClick = { viewModel.back() }, modifier = Modifier.weight(1f))
            PrimaryButton(text = "Send Exchange Request", onClick = viewModel::send, modifier = Modifier.weight(2f))
        }
    }
}

/** A compact selectable row for one of my listings. */
@Composable
private fun FurnitureRow(listing: FurnitureListing, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ItemThumbnail(
                imageUrl = listing.images.firstOrNull(),
                description = listing.title,
                modifier = Modifier.size(width = 96.dp, height = 72.dp).clip(RoundedCornerShape(12.dp)),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(listing.title, style = MaterialTheme.typography.titleSmall, maxLines = 2)
                Text(
                    listing.priceLabel(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "${listing.category.displayName} · ${listing.condition.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
