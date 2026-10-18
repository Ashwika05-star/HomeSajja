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

import androidx.compose.runtime.LaunchedEffect
import com.homesajja.app.ui.screens.requests.VendorItemRequestsList
import com.homesajja.app.viewmodel.ItemRequestsUiState
import com.homesajja.app.viewmodel.ItemRequestsViewModel

private fun RequestSection.localizedLabel(isHindi: Boolean): String = when (this) {
    RequestSection.PURCHASES -> VendorStrings.purchases(isHindi)
    RequestSection.EXCHANGES -> VendorStrings.exchangesTab(isHindi)
    RequestSection.REPAIRS -> VendorStrings.repairsTab(isHindi)
    RequestSection.ITEM_REQUESTS -> VendorStrings.itemRequests(isHindi)
}

/**
 * Everything addressed to the vendor in one place: purchase, exchange, repair, and customer item requests,
 * each in its own section wired to that system's own screens and repository.
 */
@Composable
fun VendorRequestsScreen(
    snackbarHostState: SnackbarHostState,
    onOpenListing: (String) -> Unit,
    onOpenExchange: (String) -> Unit,
    onNewExchange: () -> Unit = {},
    onOpenRepair: (String) -> Unit,
    onOpenChat: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    initialSection: RequestSection = RequestSection.PURCHASES,
) {
    val isHindi = isAppHindi
    val container = LocalAppContainer.current
    val viewModel: VendorRequestsViewModel = viewModel(factory = ViewModelFactory(container))
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
                        onOpenChat = onOpenChat,
                        receivedOnly = false,
                        includeItemRequests = false,
                        initialTab = 1,
                    )
                    RequestSection.EXCHANGES -> ExchangeRequestsScreen(
                        onOpenRequest = onOpenExchange,
                        onNewExchange = onNewExchange,
                        incomingOnly = false,
                    )
                    RequestSection.REPAIRS -> VendorRepairsList(onOpenRequest = onOpenRepair)
                    RequestSection.ITEM_REQUESTS -> {
                        val itemRequestsVm: ItemRequestsViewModel = viewModel(factory = ViewModelFactory(container))
                        val itemState by itemRequestsVm.uiState.collectAsStateWithLifecycle()
                        val authRepo = container.authRepository

                        LaunchedEffect(itemRequestsVm) {
                            itemRequestsVm.messages.collect { snackbarHostState.showSnackbar(it) }
                        }
                        LaunchedEffect(itemRequestsVm) {
                            itemRequestsVm.openChat.collect { onOpenChat(it) }
                        }

                        when (val itCurrent = itemState) {
                            ItemRequestsUiState.Loading -> LoadingState()
                            is ItemRequestsUiState.Error -> ErrorState(message = itCurrent.message, onRetry = itemRequestsVm::retry)
                            is ItemRequestsUiState.Content -> {
                                VendorItemRequestsList(
                                    openRequests = itCurrent.openRequests,
                                    myAcceptedRequests = itCurrent.myAcceptedRequests,
                                    allRequests = itCurrent.allRequests,
                                    currentVendorId = authRepo.currentUserId,
                                    vendorUpiId = itCurrent.vendorUpiId,
                                    busyRequestId = itemRequestsVm.busyRequestId,
                                    onSendQuote = { req, amount, days, note -> itemRequestsVm.sendQuote(req, amount, days, note) },
                                    onUpdateStatus = { req, status -> itemRequestsVm.updateStatus(req, status) },
                                    onConfirmPayment = { req -> itemRequestsVm.confirmPayment(req) },
                                    onAcceptRequest = { req, note -> itemRequestsVm.acceptRequest(req, note) },
                                    onFulfillRequest = { req -> itemRequestsVm.fulfillRequest(req) },
                                    onChatWithCustomer = { req -> itemRequestsVm.chatWithUser(req) },
                                    vendorCity = itCurrent.userCity,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
