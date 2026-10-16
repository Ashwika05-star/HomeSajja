package com.homesajja.app.ui.screens.exchange

import com.homesajja.app.ui.components.PillTabs
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.filled.FilterList
import com.homesajja.app.ui.components.DateFilter
import com.homesajja.app.ui.components.DateFilterDropdown
import com.homesajja.app.ui.components.NoResultsState
import com.homesajja.app.ui.i18n.VendorStrings
import com.homesajja.app.ui.i18n.isAppHindi
import com.homesajja.app.viewmodel.ExchangeRequestsUiState
import com.homesajja.app.viewmodel.ExchangeRequestsViewModel
/** The Exchange tab: requests I received (Incoming) and requests I sent (Outgoing). */
@Composable
fun ExchangeRequestsScreen(
    onOpenRequest: (String) -> Unit,
    onNewExchange: () -> Unit,
    modifier: Modifier = Modifier,
    incomingOnly: Boolean = false,
) {
    val isHindi = isAppHindi
    val viewModel: ExchangeRequestsViewModel = viewModel(factory = ViewModelFactory(LocalAppContainer.current))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var chosenTab by rememberSaveable { mutableIntStateOf(0) }
    // Vendors answer exchange offers made on their items; they don't start exchanges from this screen.
    val selectedTab = if (incomingOnly) 0 else chosenTab
    var dateFilter by remember { mutableStateOf<DateFilter>(DateFilter.All) }

    // Coming back from a request or the proposal flow: pick up changes quietly.
    // Coming back from another screen (detail, sell, exchange...): reload if the data is old.
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshIfStale()
        onPauseOrDispose {}
    }

    Column(modifier = modifier.fillMaxSize()) {
        val content = state as? ExchangeRequestsUiState.Content
        if (!incomingOnly) {
            PillTabs(
                labels = listOf(
                    "${VendorStrings.incoming(isHindi)}${content?.let { " (${it.incoming.size})" }.orEmpty()}",
                    "${VendorStrings.outgoing(isHindi)}${content?.let { " (${it.outgoing.size})" }.orEmpty()}",
                ),
                selectedIndex = selectedTab,
                onSelect = { chosenTab = it },
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
                        title = if (isIncoming) VendorStrings.noIncomingRequests(isHindi) else VendorStrings.noOutgoingRequests(isHindi),
                        subtitle = if (isIncoming) {
                            VendorStrings.noIncomingRequestsSub(isHindi)
                        } else {
                            VendorStrings.noOutgoingRequestsSub(isHindi)
                        },
                        actionLabel = if (isIncoming) null else VendorStrings.newExchange(isHindi),
                        onActionClick = if (isIncoming) null else onNewExchange,
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DateFilterDropdown(selected = dateFilter, onSelect = { dateFilter = it })
                    }
                    val visibleRequests = requests.filter { dateFilter.matches(it.createdAt) }
                    if (visibleRequests.isEmpty()) {
                        NoResultsState(
                            icon = Icons.Filled.FilterList,
                            title = if (isHindi) "इस अवधि में कोई अनुरोध नहीं मिला" else "No requests for this period",
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
                            items(visibleRequests, key = { it.id }) { request ->
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
}
