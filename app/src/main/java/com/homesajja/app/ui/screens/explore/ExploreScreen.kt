package com.homesajja.app.ui.screens.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.homesajja.app.data.model.FurnitureListing
import com.homesajja.app.data.model.FurnitureCategory
import com.homesajja.app.di.LocalAppContainer
import com.homesajja.app.di.ViewModelFactory
import com.homesajja.app.ui.components.CategoryChip
import com.homesajja.app.ui.components.EmptyState
import com.homesajja.app.ui.components.ErrorState
import com.homesajja.app.ui.components.FurnitureCard
import com.homesajja.app.ui.components.LoadingState
import com.homesajja.app.ui.components.NoResultsState
import com.homesajja.app.ui.util.formatPrice
import com.homesajja.app.viewmodel.ExploreUiState
import com.homesajja.app.viewmodel.ExploreViewModel

/** Browse listings in the user's city: search, category chips, filters, paged grid. */
@Composable
fun ExploreScreen(
    onOpenListing: (String) -> Unit,
    onSell: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: ExploreViewModel = viewModel(factory = ViewModelFactory(LocalAppContainer.current))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showFilters by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        SearchBar(
            query = viewModel.query,
            onQueryChange = viewModel::onQueryChange,
            filtersActive = viewModel.filters.isActive,
            onFiltersClick = { showFilters = true },
        )
        CategoryRow(selected = viewModel.category, onSelect = viewModel::onCategoryChange)

        Box(modifier = Modifier.weight(1f)) {
            when (val current = state) {
                ExploreUiState.Loading -> LoadingState()
                is ExploreUiState.Error -> ErrorState(message = current.message, onRetry = viewModel::retry)
                is ExploreUiState.Content -> ExploreContent(
                    content = current,
                    city = viewModel.city,
                    hasActiveNarrowing = viewModel.hasActiveNarrowing,
                    isRefreshing = viewModel.isRefreshing,
                    onRefresh = viewModel::refresh,
                    onLoadMore = viewModel::loadMore,
                    onClearNarrowing = viewModel::clearNarrowing,
                    onOpenListing = onOpenListing,
                    onSell = onSell,
                )
            }
        }
    }

    if (showFilters) {
        FilterSheet(
            filters = viewModel.filters,
            onApply = {
                viewModel.onFiltersChange(it)
                showFilters = false
            },
            onDismiss = { showFilters = false },
        )
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    filtersActive: Boolean,
    onFiltersClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Search furniture") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
        )
        IconButton(onClick = onFiltersClick) {
            BadgedBox(badge = { if (filtersActive) Badge() }) {
                Icon(Icons.Filled.FilterList, contentDescription = "Filters")
            }
        }
    }
}

@Composable
private fun CategoryRow(selected: FurnitureCategory?, onSelect: (FurnitureCategory?) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 8.dp),
    ) {
        item { CategoryChip(label = "All", selected = selected == null, onClick = { onSelect(null) }) }
        items(FurnitureCategory.entries) { category ->
            CategoryChip(
                label = category.displayName,
                selected = selected == category,
                onClick = { onSelect(category) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExploreContent(
    content: ExploreUiState.Content,
    city: String,
    hasActiveNarrowing: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onClearNarrowing: () -> Unit,
    onOpenListing: (String) -> Unit,
    onSell: () -> Unit,
) {
    if (content.listings.isEmpty() && content.isLoadingMore) {
        // Search/filters hid everything fetched so far, and more is being fetched.
        LoadingState()
        return
    }

    if (content.listings.isEmpty()) {
        // Scrollable so the empty screens can still be pulled down to refresh
        // (e.g. after someone lists the first item in the city).
        PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize()) {
                        if (hasActiveNarrowing) {
                            NoResultsState(
                                icon = Icons.Filled.SearchOff,
                                title = "No matching listings",
                                subtitle = "Nothing in $city matches your search or filters.",
                                actionLabel = "Clear search & filters",
                                onActionClick = onClearNarrowing,
                            )
                        } else {
                            EmptyState(
                                icon = Icons.Filled.Inventory2,
                                title = "No listings in $city yet",
                                subtitle = "Be the first to list something for sale here.",
                                actionLabel = "Sell an item",
                                onActionClick = onSell,
                            )
                        }
                    }
                }
            }
        }
        return
    }

    val gridState = rememberLazyGridState()
    val nearEnd by remember {
        derivedStateOf {
            val info = gridState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= info.totalItemsCount - 4
        }
    }
    LaunchedEffect(nearEnd, content.listings.size) {
        if (nearEnd) onLoadMore()
    }

    PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(content.listings, key = { it.id }) { listing ->
                FurnitureCard(
                    title = listing.title,
                    price = formatPrice(listing.price),
                    imageUrl = listing.images.firstOrNull(),
                    subtitle = cardSubtitle(listing),
                    onClick = { onOpenListing(listing.id) },
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                GridFooter(content = content, city = city, onRetry = onLoadMore)
            }
        }
    }
}

private fun cardSubtitle(listing: FurnitureListing): String =
    "${listing.city} · ${if (listing.refurbished) "Refurbished" else listing.condition.displayName}"

@Composable
private fun GridFooter(content: ExploreUiState.Content, city: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        when {
            content.isLoadingMore -> CircularProgressIndicator(strokeWidth = 2.dp)
            content.loadMoreError != null -> TextButton(onClick = onRetry) {
                Text("${content.loadMoreError} Tap to retry")
            }
            content.endReached -> Text(
                "That's everything in $city",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
