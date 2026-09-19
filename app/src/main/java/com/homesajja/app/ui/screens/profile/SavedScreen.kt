package com.homesajja.app.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.homesajja.app.di.LocalAppContainer
import com.homesajja.app.di.ViewModelFactory
import com.homesajja.app.ui.components.AppTopBar
import com.homesajja.app.ui.components.EmptyState
import com.homesajja.app.ui.components.ErrorState
import com.homesajja.app.ui.components.FurnitureCard
import com.homesajja.app.ui.components.LoadingState
import com.homesajja.app.ui.util.priceLabel
import com.homesajja.app.viewmodel.SavedItem
import com.homesajja.app.viewmodel.SavedListingsViewModel
import com.homesajja.app.viewmodel.SavedUiState

/** Everything the person has saved, as a grid. Listings that were deleted or sold since are shown as "No longer available". */
@Composable
fun SavedScreen(onBackClick: () -> Unit, onOpenListing: (String) -> Unit) {
    val viewModel: SavedListingsViewModel = viewModel(factory = ViewModelFactory(LocalAppContainer.current))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleResumeEffect(viewModel) {
        viewModel.refreshIfStale()
        onPauseOrDispose {}
    }

    Scaffold(
        topBar = { AppTopBar(title = "Saved furniture", onBackClick = onBackClick) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val current = state) {
                SavedUiState.Loading -> LoadingState()
                is SavedUiState.Error -> ErrorState(message = current.message, onRetry = viewModel::retry)
                is SavedUiState.Content -> {
                    if (current.items.isEmpty()) {
                        EmptyState(
                            icon = Icons.Filled.FavoriteBorder,
                            title = "Nothing saved yet",
                            subtitle = "Tap the heart on a listing to keep it here for later.",
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(current.items, key = { it.listingId }) { item ->
                                val listing = item.listing
                                if (item.isAvailable && listing != null) {
                                    FurnitureCard(
                                        title = listing.title,
                                        price = listing.priceLabel(),
                                        imageUrl = listing.images.firstOrNull(),
                                        subtitle = listing.city,
                                        onClick = { onOpenListing(listing.id) },
                                        isSaved = true,
                                        onToggleSaved = { viewModel.remove(item.listingId) },
                                    )
                                } else {
                                    UnavailableCard(item, onRemove = { viewModel.remove(item.listingId) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UnavailableCard(item: SavedItem, onRemove: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("No longer available", style = MaterialTheme.typography.titleSmall)
            item.listing?.title?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text(
                if (item.listing == null) "This listing was removed." else "It was sold or taken down.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onRemove) { Text("Remove") }
        }
    }
}
