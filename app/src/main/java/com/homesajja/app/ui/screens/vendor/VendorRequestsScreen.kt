package com.homesajja.app.ui.screens.vendor

import com.homesajja.app.ui.components.PillTabs
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.homesajja.app.di.LocalAppContainer
import com.homesajja.app.di.ViewModelFactory
import com.homesajja.app.ui.components.ErrorState
import com.homesajja.app.ui.components.LoadingState
import com.homesajja.app.ui.i18n.VendorStrings
import com.homesajja.app.ui.i18n.isAppHindi
import com.homesajja.app.ui.screens.exchange.ExchangeRequestsScreen
import com.homesajja.app.ui.screens.repair.VendorRepairsList
import com.homesajja.app.ui.screens.requests.MyRequestsScreen
import com.homesajja.app.viewmodel.RequestSection
import com.homesajja.app.viewmodel.VendorRequestsUiState
import com.homesajja.app.viewmodel.VendorRequestsViewModel

private fun RequestSection.localizedLabel(isHindi: Boolean): String = when (this) {
    RequestSection.PURCHASES -> VendorStrings.purchases(isHindi)
    RequestSection.EXCHANGES -> VendorStrings.exchangesTab(isHindi)
    RequestSection.REPAIRS -> VendorStrings.repairsTab(isHindi)
}

/**
 * Everything addressed to the vendor in one place: purchase, exchange, and repair requests,
 * each in its own section wired to that system's own screens and repository.
 */
@Composable
fun VendorRequestsScreen(
    snackbarHostState: SnackbarHostState,
    onOpenListing: (String) -> Unit,
    onOpenExchange: (String) -> Unit,
    onNewExchange: () -> Unit = {},
    onOpenRepair: (String) -> Unit,
    modifier: Modifier = Modifier,
    initialSection: RequestSection = RequestSection.PURCHASES,
) {
    val isHindi = isAppHindi
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
                PillTabs(
                    labels = sections.map { it.localizedLabel(isHindi) },
                    selectedIndex = sections.indexOf(section),
                    onSelect = { selected = sections[it].ordinal },
                )
                when (section) {
                    RequestSection.PURCHASES -> MyRequestsScreen(
                        snackbarHostState = snackbarHostState,
                        onOpenListing = onOpenListing,
                        receivedOnly = false,
                        initialTab = 1,
                    )
                    RequestSection.EXCHANGES -> ExchangeRequestsScreen(
                        onOpenRequest = onOpenExchange,
                        onNewExchange = onNewExchange,
                        incomingOnly = false,
                    )
                    RequestSection.REPAIRS -> VendorRepairsList(onOpenRequest = onOpenRepair)
                }
            }
        }
    }
}
