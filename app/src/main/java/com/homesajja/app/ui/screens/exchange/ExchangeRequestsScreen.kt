package com.homesajja.app.ui.screens.exchange

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.homesajja.app.di.LocalAppContainer
import com.homesajja.app.di.ViewModelFactory
import com.homesajja.app.ui.components.EmptyState
import com.homesajja.app.ui.components.ErrorState
import com.homesajja.app.ui.components.LoadingState
import com.homesajja.app.viewmodel.ExchangeRequestsUiState
import com.homesajja.app.viewmodel.ExchangeRequestsViewModel

/** The Exchange tab: requests I received (Incoming) and requests I sent (Outgoing). */
@Composable
fun ExchangeRequestsScreen(
    onOpenRequest: (String) -> Unit,
    onNewExchange: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: ExchangeRequestsViewModel = viewModel(factory = ViewModelFactory(LocalAppContainer.current))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    // Coming back from a request or the proposal flow: pick up changes quietly.
    // Coming back from another screen (detail, sell, exchange...): reload if the data is old.
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshIfStale()
        onPauseOrDispose {}
    }

    Column(modifier = modifier.fillMaxSize()) {
        val content = state as? ExchangeRequestsUiState.Content
        TabRow(selectedTabIndex = selectedTab, containerColor = MaterialTheme.colorScheme.background) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Incoming${content?.let { " (${it.incoming.size})" }.orEmpty()}") },
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Outgoing${content?.let { " (${it.outgoing.size})" }.orEmpty()}") },
            )
        }

        when (val current = state) {
            ExchangeRequestsUiState.Loading -> LoadingState()
            is ExchangeRequestsUiState.Error -> ErrorState(message = current.message, onRetry = viewModel::retry)
            is ExchangeRequestsUiState.Content -> {
                val isIncoming = selectedTab == 0
                val requests = if (isIncoming) current.incoming else current.outgoing
                if (requests.isEmpty()) {
                    EmptyState(
                        icon = Icons.Filled.SwapHoriz,
                        title = if (isIncoming) "No incoming requests" else "No requests sent",
                        subtitle = if (isIncoming) {
                            "When someone offers a swap for one of your exchange items, it shows up here."
                        } else {
                            "Propose an exchange and you can follow it here."
                        },
                        actionLabel = if (isIncoming) null else "New exchange",
                        onActionClick = if (isIncoming) null else onNewExchange,
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(requests, key = { it.id }) { request ->
                            ExchangeRequestCard(
                                request = request,
                                isIncoming = isIncoming,
                                onClick = { onOpenRequest(request.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}
