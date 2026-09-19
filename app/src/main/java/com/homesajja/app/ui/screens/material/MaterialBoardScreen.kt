package com.homesajja.app.ui.screens.material

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.homesajja.app.data.model.MaterialType
import com.homesajja.app.di.LocalAppContainer
import com.homesajja.app.di.ViewModelFactory
import com.homesajja.app.ui.components.CategoryChip
import com.homesajja.app.ui.components.EmptyState
import com.homesajja.app.ui.components.ErrorState
import com.homesajja.app.ui.components.LoadingState
import com.homesajja.app.ui.components.NoResultsState
import com.homesajja.app.viewmodel.MaterialBoardViewModel
import com.homesajja.app.viewmodel.MaterialListUiState
import com.homesajja.app.viewmodel.VendorMaterialsViewModel

/** What users see in Services > Materials: vendors in their city asking for materials, filterable by material. */
@Composable
fun MaterialBoardScreen(onOpenRequest: (String) -> Unit, modifier: Modifier = Modifier) {
    val viewModel: MaterialBoardViewModel = viewModel(factory = ViewModelFactory(LocalAppContainer.current))
    MaterialList(viewModel, isVendorView = false, onOpenRequest = onOpenRequest, onNewRequest = null, modifier = modifier)
}

/** The vendor's own material requests, with a way to post the first one. */
@Composable
fun VendorMaterialsScreen(onOpenRequest: (String) -> Unit, onNewRequest: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel: VendorMaterialsViewModel = viewModel(factory = ViewModelFactory(LocalAppContainer.current))
    MaterialList(viewModel, isVendorView = true, onOpenRequest = onOpenRequest, onNewRequest = onNewRequest, modifier = modifier)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MaterialList(
    viewModel: MaterialBoardViewModel,
    isVendorView: Boolean,
    onOpenRequest: (String) -> Unit,
    onNewRequest: (() -> Unit)?,
    modifier: Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleResumeEffect(viewModel) {
        viewModel.refreshIfStale()
        onPauseOrDispose {}
    }

    Column(modifier = modifier.fillMaxSize()) {
        when (val current = state) {
            MaterialListUiState.Loading -> LoadingState()
            is MaterialListUiState.Error -> ErrorState(message = current.message, onRetry = viewModel::retry)
            is MaterialListUiState.Content -> {
                if (current.requests.isEmpty()) {
                    EmptyState(
                        icon = Icons.Filled.Inventory2,
                        title = if (isVendorView) "No material requests yet" else "No requests in ${viewModel.city.ifBlank { "your city" }}",
                        subtitle = if (isVendorView) {
                            "Post what you're looking for and people can offer furniture to you."
                        } else {
                            "When vendors in your city look for materials, their requests show up here."
                        },
                        actionLabel = if (isVendorView) "Post a request" else null,
                        onActionClick = onNewRequest,
                    )
                } else {
                    if (!isVendorView) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            CategoryChip(label = "All", selected = viewModel.materialFilter == null, onClick = { viewModel.selectMaterial(null) })
                            MaterialType.entries.forEach { type ->
                                CategoryChip(
                                    label = type.displayName,
                                    selected = viewModel.materialFilter == type,
                                    onClick = { viewModel.selectMaterial(type) },
                                )
                            }
                        }
                    }
                    val visible = current.requests.filter { viewModel.materialFilter == null || it.materialType == viewModel.materialFilter }
                    if (visible.isEmpty()) {
                        NoResultsState(
                            icon = Icons.Filled.Inventory2,
                            title = "No ${viewModel.materialFilter?.displayName?.lowercase()} requests",
                            subtitle = "Try a different material.",
                            actionLabel = "Show all",
                            onActionClick = { viewModel.selectMaterial(null) },
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(visible, key = { it.id }) { request ->
                                MaterialRequestCard(request, showVendor = !isVendorView, onClick = { onOpenRequest(request.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}
