package com.homesajja.app.ui.screens.recycle

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
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import com.homesajja.app.viewmodel.MyRecyclingViewModel
import com.homesajja.app.viewmodel.RecycleFilter
import com.homesajja.app.viewmodel.RecycleListUiState
import com.homesajja.app.viewmodel.VendorRecyclingViewModel

/** The user's recycling requests with a status badge each, filterable to active or closed. */
@Composable
fun MyRecyclingScreen(
    onOpenRequest: (String) -> Unit,
    onRecycle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: MyRecyclingViewModel = viewModel(factory = ViewModelFactory(LocalAppContainer.current))
    RecycleList(viewModel, viewerIsRecycler = false, onOpenRequest = onOpenRequest, onRecycle = onRecycle, modifier = modifier)
}

/** The recycling requests addressed to this vendor. */
@Composable
fun VendorRecyclingList(onOpenRequest: (String) -> Unit, modifier: Modifier = Modifier) {
    val viewModel: VendorRecyclingViewModel = viewModel(factory = ViewModelFactory(LocalAppContainer.current))
    RecycleList(viewModel, viewerIsRecycler = true, onOpenRequest = onOpenRequest, onRecycle = null, modifier = modifier)
}

@Composable
private fun RecycleList(
    viewModel: MyRecyclingViewModel,
    viewerIsRecycler: Boolean,
    onOpenRequest: (String) -> Unit,
    onRecycle: (() -> Unit)?,
    modifier: Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Coming back from a request or the flow: reload quietly if the data is old.
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshIfStale()
        onPauseOrDispose {}
    }

    Column(modifier = modifier.fillMaxSize()) {
        when (val current = state) {
            RecycleListUiState.Loading -> LoadingState()
            is RecycleListUiState.Error -> ErrorState(message = current.message, onRetry = viewModel::retry)
            is RecycleListUiState.Content -> {
                if (current.requests.isEmpty()) {
                    EmptyState(
                        icon = Icons.Filled.Recycling,
                        title = "No recycling requests yet",
                        subtitle = if (viewerIsRecycler) {
                            "Recycling requests addressed to you will show up here."
                        } else {
                            "Furniture too far gone to sell or fix? Send it to a recycler instead of the bin."
                        },
                        actionLabel = if (onRecycle != null) "Recycle furniture" else null,
                        onActionClick = onRecycle,
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        RecycleFilter.entries.forEach { option ->
                            CategoryChip(
                                label = option.label,
                                selected = viewModel.filter == option,
                                onClick = { viewModel.selectFilter(option) },
                            )
                        }
                    }
                    val visible = current.requests.filter { viewModel.filter.matches(it.status) }
                    if (visible.isEmpty()) {
                        NoResultsState(
                            icon = Icons.Filled.Recycling,
                            title = "No ${viewModel.filter.label.lowercase()} requests",
                            subtitle = "Try a different filter.",
                            actionLabel = "Show all",
                            onActionClick = { viewModel.selectFilter(RecycleFilter.ALL) },
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(visible, key = { it.id }) { request ->
                                RecycleRequestCard(
                                    request = request,
                                    viewerIsRecycler = viewerIsRecycler,
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
