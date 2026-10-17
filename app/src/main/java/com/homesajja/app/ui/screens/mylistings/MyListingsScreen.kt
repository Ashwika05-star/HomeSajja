package com.homesajja.app.ui.screens.mylistings

import androidx.compose.foundation.BorderStroke
import com.homesajja.app.ui.components.PillTabs
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
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Sell
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
import com.homesajja.app.ui.util.ImageWidth
import com.homesajja.app.ui.util.optimizedImage
import com.homesajja.app.data.model.FurnitureListing
import com.homesajja.app.data.model.ListingStatus
import com.homesajja.app.di.LocalAppContainer
import com.homesajja.app.di.ViewModelFactory
import com.homesajja.app.ui.components.EmptyState
import com.homesajja.app.ui.components.ErrorState
import com.homesajja.app.ui.components.LoadingState
import com.homesajja.app.ui.components.StatusBadge
import com.homesajja.app.ui.i18n.VendorStrings
import com.homesajja.app.ui.i18n.isAppHindi
import com.homesajja.app.ui.theme.PriceMedium
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
    allowAvailabilityToggle: Boolean = false,
) {
    val isHindi = isAppHindi
    val viewModel: MyListingsViewModel = viewModel(factory = ViewModelFactory(LocalAppContainer.current))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var pendingDelete by remember { mutableStateOf<FurnitureListing?>(null) }
    var dateFilter by remember { mutableStateOf<DateFilter>(DateFilter.All) }

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
        PillTabs(
            labels = listOf(
                content?.let { VendorStrings.activeTab(it.active.size, isHindi) } ?: if (isHindi) "सक्रिय" else "Active",
                content?.let { VendorStrings.soldAndClosedTab(it.closed.size, isHindi) } ?: if (isHindi) "बिक चुके / बंद" else "Sold & closed",
            ),
            selectedIndex = selectedTab,
            onSelect = { selectedTab = it },
        )

        when (val current = state) {
            MyListingsUiState.Loading -> LoadingState()
            is MyListingsUiState.Error -> ErrorState(message = current.message, onRetry = viewModel::retry)
            is MyListingsUiState.Content -> {
                val listings = if (selectedTab == 0) current.active else current.closed
                if (listings.isEmpty()) {
                    if (selectedTab == 0) {
                        EmptyState(
                            icon = Icons.Filled.Inventory2,
                            title = VendorStrings.noActiveListings(isHindi),
                            subtitle = VendorStrings.itemsForSaleOrExchangeSub(isHindi),
                            actionLabel = VendorStrings.addListingFab(isHindi),
                            onActionClick = onSell,
                        )
                    } else {
                        EmptyState(
                            icon = Icons.Filled.Inventory2,
                            title = VendorStrings.nothingSoldYet(isHindi),
                            subtitle = VendorStrings.nothingSoldYetSub(isHindi),
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DateFilterDropdown(selected = dateFilter, onSelect = { dateFilter = it })
                    }
                    val visibleListings = listings.filter { dateFilter.matches(it.createdAt) }
                    if (visibleListings.isEmpty()) {
                        NoResultsState(
                            icon = Icons.Filled.FilterList,
                            title = if (isHindi) "इस अवधि में कोई लिस्टिंग नहीं मिली" else "No listings for this period",
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
                            items(visibleListings, key = { it.id }) { listing ->
                            MyListingCard(
                                listing = listing,
                                busy = viewModel.busyListingId == listing.id,
                                onClick = { onOpenListing(listing.id) },
                                onEdit = { onEditListing(listing.id) },
                                onMarkSold = { viewModel.markAsSold(listing) },
                                onToggleAvailability = if (allowAvailabilityToggle) {
                                    { viewModel.toggleAvailability(listing) }
                                } else {
                                    null
                                },
                                onDelete = { pendingDelete = listing },
                            )
                        }
                    }
                }
            }
        }
    }
}

    pendingDelete?.let { listing ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(VendorStrings.deleteListingTitle(isHindi)) },
            text = { Text(VendorStrings.deleteListingConfirm(listing.title, isHindi)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    viewModel.delete(listing)
                }) { Text(VendorStrings.delete(isHindi), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text(VendorStrings.cancel(isHindi)) } },
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
    onToggleAvailability: (() -> Unit)?,
    onDelete: () -> Unit,
) {
    val isHindi = isAppHindi
    val isOpen = listing.status in setOf(ListingStatus.ACTIVE, ListingStatus.RESERVED, ListingStatus.UNAVAILABLE)
    val canToggle = onToggleAvailability != null && listing.status in setOf(ListingStatus.ACTIVE, ListingStatus.UNAVAILABLE)

    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium),
                ) {
                    AsyncImage(
                        model = optimizedImage(listing.images.firstOrNull(), ImageWidth.THUMBNAIL),
                        contentDescription = listing.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(88.dp).clip(MaterialTheme.shapes.medium),
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        listing.category.displayName.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(listing.title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                    Text(
                        listing.priceLabel(),
                        style = PriceMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    StatusBadge(status = listing.status.displayName)
                }
            }
            if (isOpen) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onEdit, enabled = !busy) { Text(VendorStrings.edit(isHindi)) }
                    if (canToggle) {
                        TextButton(onClick = { onToggleAvailability?.invoke() }, enabled = !busy) {
                            Text(if (listing.status == ListingStatus.ACTIVE) VendorStrings.markUnavailable(isHindi) else VendorStrings.markAvailable(isHindi))
                        }
                    }
                    if (listing.status != ListingStatus.UNAVAILABLE) {
                        TextButton(onClick = onMarkSold, enabled = !busy) { Text(VendorStrings.markAsSold(isHindi)) }
                    }
                    TextButton(onClick = onDelete, enabled = !busy) {
                        Text(VendorStrings.delete(isHindi), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
