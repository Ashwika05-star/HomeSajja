package com.homesajja.app.ui.screens.repair

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
import androidx.compose.material.icons.filled.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.homesajja.app.ui.components.DateFilter
import com.homesajja.app.ui.components.DateFilterDropdown
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.homesajja.app.di.LocalAppContainer
import com.homesajja.app.di.ViewModelFactory
import com.homesajja.app.ui.components.CategoryChip
import com.homesajja.app.ui.components.EmptyState
import com.homesajja.app.ui.components.ErrorState
import com.homesajja.app.ui.components.LoadingState
import com.homesajja.app.ui.components.NoResultsState
import com.homesajja.app.ui.i18n.VendorStrings
import com.homesajja.app.ui.i18n.isAppHindi
import com.homesajja.app.viewmodel.MyRepairsViewModel
import com.homesajja.app.viewmodel.RepairFilter
import com.homesajja.app.viewmodel.RepairListUiState
import com.homesajja.app.viewmodel.VendorRepairsViewModel

/** The Repair tab: the user's repair requests with a status badge each, filterable to active or closed. */
@Composable
fun MyRepairsScreen(
    onOpenRequest: (String) -> Unit,
    onRequestRepair: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: MyRepairsViewModel = viewModel(factory = ViewModelFactory(LocalAppContainer.current))
    RepairList(viewModel, viewerIsVendor = false, onOpenRequest = onOpenRequest, onRequestRepair = onRequestRepair, modifier = modifier)
}

/** The repair requests addressed to this vendor. */
@Composable
fun VendorRepairsList(onOpenRequest: (String) -> Unit, modifier: Modifier = Modifier) {
    val viewModel: VendorRepairsViewModel = viewModel(factory = ViewModelFactory(LocalAppContainer.current))
    RepairList(viewModel, viewerIsVendor = true, onOpenRequest = onOpenRequest, onRequestRepair = null, modifier = modifier)
}

@Composable
private fun RepairList(
    viewModel: MyRepairsViewModel,
    viewerIsVendor: Boolean,
    onOpenRequest: (String) -> Unit,
    onRequestRepair: (() -> Unit)?,
    modifier: Modifier,
) {
    val isHindi = isAppHindi
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var dateFilter by remember { mutableStateOf<DateFilter>(DateFilter.All) }

    // Coming back from a request or the flow: reload quietly if the data is old.
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshIfStale()
        onPauseOrDispose {}
    }

    Column(modifier = modifier.fillMaxSize()) {
        when (val current = state) {
            RepairListUiState.Loading -> LoadingState()
            is RepairListUiState.Error -> ErrorState(message = current.message, onRetry = viewModel::retry)
            is RepairListUiState.Content -> {
                if (current.requests.isEmpty()) {
                    EmptyState(
                        icon = Icons.Filled.Build,
                        title = VendorStrings.noRepairRequestsYet(isHindi),
                        subtitle = if (viewerIsVendor) {
                            VendorStrings.noRepairRequestsVendorSub(isHindi)
                        } else {
                            "Got a damaged chair or a wobbly table? Ask a repair professional in your city to fix it."
                        },
                        actionLabel = if (onRequestRepair != null) "Request a repair" else null,
                        onActionClick = onRequestRepair,
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            RepairFilter.entries.forEach { option ->
                                val label = when (option) {
                                    RepairFilter.ALL -> VendorStrings.filterAll(isHindi)
                                    RepairFilter.ACTIVE -> VendorStrings.filterActive(isHindi)
                                    RepairFilter.CLOSED -> VendorStrings.filterClosed(isHindi)
                                }
                                CategoryChip(
                                    label = label,
                                    selected = viewModel.filter == option,
                                    onClick = { viewModel.selectFilter(option) },
                                )
                            }
                        }
                        DateFilterDropdown(selected = dateFilter, onSelect = { dateFilter = it })
                    }
                    val visible = current.requests.filter { viewModel.filter.matches(it.status) && dateFilter.matches(it.createdAt) }
                    if (visible.isEmpty()) {
                        NoResultsState(
                            icon = Icons.Filled.Build,
                            title = if (isHindi) "कोई अनुरोध नहीं मिला" else "No matching requests",
                            subtitle = VendorStrings.tryDifferentFilter(isHindi),
                            actionLabel = VendorStrings.showAll(isHindi),
                            onActionClick = {
                                viewModel.selectFilter(RepairFilter.ALL)
                                dateFilter = DateFilter.All
                            },
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(visible, key = { it.id }) { request ->
                                RepairRequestCard(
                                    request = request,
                                    viewerIsVendor = viewerIsVendor,
                                    onClick = { onOpenRequest(request.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
