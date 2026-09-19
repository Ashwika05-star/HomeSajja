package com.homesajja.app.ui.screens.vendor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.homesajja.app.data.model.RecyclingRequest
import com.homesajja.app.di.LocalAppContainer
import com.homesajja.app.di.ViewModelFactory
import com.homesajja.app.ui.components.CategoryChip
import com.homesajja.app.ui.components.EmptyState
import com.homesajja.app.ui.components.ErrorState
import com.homesajja.app.ui.components.ItemThumbnail
import com.homesajja.app.ui.components.LoadingState
import com.homesajja.app.ui.components.PrimaryButton
import com.homesajja.app.ui.screens.exchange.ExchangeRequestsScreen
import com.homesajja.app.ui.screens.recycle.VendorRecyclingList
import com.homesajja.app.ui.screens.repair.VendorRepairsList
import com.homesajja.app.ui.screens.requests.MyRequestsScreen
import com.homesajja.app.viewmodel.OpenPickupsUiState
import com.homesajja.app.viewmodel.OpenPickupsViewModel
import com.homesajja.app.viewmodel.RequestSection
import com.homesajja.app.viewmodel.VendorRequestsUiState
import com.homesajja.app.viewmodel.VendorRequestsViewModel

/**
 * Everything addressed to the vendor in one place: purchase, exchange, repair and recycling requests,
 * each in its own section wired to that system's own screens and repository.
 */
@Composable
fun VendorRequestsScreen(
    snackbarHostState: SnackbarHostState,
    onOpenListing: (String) -> Unit,
    onOpenExchange: (String) -> Unit,
    onOpenRepair: (String) -> Unit,
    onOpenRecycling: (String) -> Unit,
    modifier: Modifier = Modifier,
    initialSection: RequestSection = RequestSection.PURCHASES,
) {
    val viewModel: VendorRequestsViewModel = viewModel(factory = ViewModelFactory(LocalAppContainer.current))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selected by rememberSaveable { mutableIntStateOf(initialSection.ordinal) }

    when (val current = state) {
        VendorRequestsUiState.Loading -> LoadingState(modifier)
        is VendorRequestsUiState.Error -> ErrorState(message = current.message, onRetry = viewModel::retry, modifier = modifier)
        is VendorRequestsUiState.Content -> {
            val sections = current.sections
            val section = RequestSection.entries[selected].takeIf { it in sections } ?: sections.first()
            Column(modifier = modifier.fillMaxSize()) {
                ScrollableTabRow(
                    selectedTabIndex = sections.indexOf(section),
                    edgePadding = 8.dp,
                    containerColor = MaterialTheme.colorScheme.background,
                ) {
                    sections.forEach { entry ->
                        Tab(selected = entry == section, onClick = { selected = entry.ordinal }, text = { Text(entry.label) })
                    }
                }
                when (section) {
                    RequestSection.PURCHASES -> MyRequestsScreen(snackbarHostState, onOpenListing, receivedOnly = true)
                    RequestSection.EXCHANGES -> ExchangeRequestsScreen(onOpenExchange, onNewExchange = {}, incomingOnly = true)
                    RequestSection.REPAIRS -> VendorRepairsList(onOpenRequest = onOpenRepair)
                    RequestSection.RECYCLING -> RecyclingSection(snackbarHostState, onOpenRecycling)
                }
            }
        }
    }
}

/** Recyclers switch between the requests addressed to them and the unclaimed pickups in their city. */
@Composable
private fun RecyclingSection(snackbarHostState: SnackbarHostState, onOpenRecycling: (String) -> Unit) {
    var showOpen by rememberSaveable { mutableIntStateOf(0) }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CategoryChip(label = "My requests", selected = showOpen == 0, onClick = { showOpen = 0 })
            CategoryChip(label = "Open pickups", selected = showOpen == 1, onClick = { showOpen = 1 })
        }
        if (showOpen == 0) {
            VendorRecyclingList(onOpenRequest = onOpenRecycling)
        } else {
            OpenPickups(snackbarHostState)
        }
    }
}

@Composable
private fun OpenPickups(snackbarHostState: SnackbarHostState) {
    val viewModel: OpenPickupsViewModel = viewModel(factory = ViewModelFactory(LocalAppContainer.current))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    when (val current = state) {
        OpenPickupsUiState.Loading -> LoadingState()
        is OpenPickupsUiState.Error -> ErrorState(message = current.message, onRetry = viewModel::retry)
        is OpenPickupsUiState.Content -> {
            if (current.pickups.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.Recycling,
                    title = "No open pickups in ${current.city}",
                    subtitle = "Pickup requests nobody has claimed yet will show up here.",
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(current.pickups, key = { it.id }) { request ->
                        PickupCard(request, busy = viewModel.busyId == request.id, onClaim = { viewModel.claim(request) })
                    }
                }
            }
        }
    }
}

@Composable
private fun PickupCard(request: RecyclingRequest, busy: Boolean, onClaim: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.weight(0.4f)) {
                    ItemThumbnail(
                        imageUrl = request.images.firstOrNull(),
                        description = "${request.material.displayName} furniture",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Column(modifier = Modifier.weight(0.6f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${request.material.displayName} furniture", style = MaterialTheme.typography.titleSmall)
                    Text(request.condition.displayName, style = MaterialTheme.typography.bodyMedium)
                    Text("From ${request.userName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            PrimaryButton(text = "Claim pickup", onClick = onClaim, enabled = !busy, modifier = Modifier.fillMaxWidth())
        }
    }
}
