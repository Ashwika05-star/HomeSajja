package com.homesajja.app.ui.screens.mylistings

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Sell
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.homesajja.app.data.model.FurnitureListing
import com.homesajja.app.data.model.ListingStatus
import com.homesajja.app.di.LocalAppContainer
import com.homesajja.app.di.ViewModelFactory
import com.homesajja.app.ui.components.EmptyState
import com.homesajja.app.ui.components.ErrorState
import com.homesajja.app.ui.components.LoadingState
import com.homesajja.app.ui.components.StatusBadge
import com.homesajja.app.ui.util.priceLabel
import com.homesajja.app.viewmodel.MyListingsUiState
import com.homesajja.app.viewmodel.MyListingsViewModel

/** The seller's own listings: Active (for sale or reserved) and Sold/closed tabs. */
@Composable
fun MyListingsScreen(
    snackbarHostState: SnackbarHostState,
    onOpenListing: (String) -> Unit,
    onEditListing: (String) -> Unit,
    onSell: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: MyListingsViewModel = viewModel(factory = ViewModelFactory(LocalAppContainer.current))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var pendingDelete by remember { mutableStateOf<FurnitureListing?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    // Coming back from another screen (detail, sell, exchange...): reload if the data is old.
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshIfStale()
        onPauseOrDispose {}
    }

    Column(modifier = modifier.fillMaxSize()) {
        val content = state as? MyListingsUiState.Content
        TabRow(selectedTabIndex = selectedTab, containerColor = MaterialTheme.colorScheme.background) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Active${content?.let { " (${it.active.size})" }.orEmpty()}") },
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Sold & closed${content?.let { " (${it.closed.size})" }.orEmpty()}") },
            )
        }

        when (val current = state) {
            MyListingsUiState.Loading -> LoadingState()
            is MyListingsUiState.Error -> ErrorState(message = current.message, onRetry = viewModel::retry)
            is MyListingsUiState.Content -> {
                val listings = if (selectedTab == 0) current.active else current.closed
                if (listings.isEmpty()) {
                    if (selectedTab == 0) {
                        EmptyState(
                            icon = Icons.Filled.Sell,
                            title = "No active listings",
                            subtitle = "Items you put up for sale appear here.",
                            actionLabel = "Sell an item",
                            onActionClick = onSell,
                        )
                    } else {
                        EmptyState(
                            icon = Icons.Filled.Inventory2,
                            title = "Nothing sold yet",
                            subtitle = "Sold and closed listings will show up here.",
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(listings, key = { it.id }) { listing ->
                            MyListingCard(
                                listing = listing,
                                busy = viewModel.busyListingId == listing.id,
                                onClick = { onOpenListing(listing.id) },
                                onEdit = { onEditListing(listing.id) },
                                onMarkSold = { viewModel.markAsSold(listing) },
                                onDelete = { pendingDelete = listing },
                            )
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { listing ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete listing?") },
            text = { Text("\"${listing.title}\" and its photos will be removed permanently.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    viewModel.delete(listing)
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun MyListingCard(
    listing: FurnitureListing,
    busy: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onMarkSold: () -> Unit,
    onDelete: () -> Unit,
) {
    val isOpen = listing.status == ListingStatus.ACTIVE || listing.status == ListingStatus.RESERVED

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
                ) {
                    AsyncImage(
                        model = listing.images.firstOrNull(),
                        contentDescription = listing.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(88.dp).clip(RoundedCornerShape(12.dp)),
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(listing.title, style = MaterialTheme.typography.titleSmall, maxLines = 2)
                    Text(
                        listing.priceLabel(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    StatusBadge(status = listing.status.displayName)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isOpen) {
                    TextButton(onClick = onEdit, enabled = !busy) { Text("Edit") }
                    TextButton(onClick = onMarkSold, enabled = !busy) { Text("Mark as sold") }
                }
                TextButton(onClick = onDelete, enabled = !busy) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
