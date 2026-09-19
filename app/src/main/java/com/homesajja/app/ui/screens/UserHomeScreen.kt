package com.homesajja.app.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.viewmodel.compose.viewModel
import com.homesajja.app.di.LocalAppContainer
import com.homesajja.app.di.ViewModelFactory
import com.homesajja.app.ui.components.AppTopBar
import com.homesajja.app.ui.screens.exchange.ExchangeRequestsScreen
import com.homesajja.app.ui.screens.explore.ExploreScreen
import com.homesajja.app.ui.screens.mylistings.MyListingsScreen
import com.homesajja.app.ui.screens.requests.MyRequestsScreen
import com.homesajja.app.viewmodel.HomeViewModel

private enum class HomeTab(val label: String, val icon: ImageVector) {
    EXPLORE("Explore", Icons.Filled.Explore),
    EXCHANGE("Exchange", Icons.Filled.SwapHoriz),
    MY_LISTINGS("My listings", Icons.Filled.Sell),
    MY_REQUESTS("Requests", Icons.AutoMirrored.Filled.ReceiptLong),
    SERVICES("Services", Icons.Filled.Build),
}

/** The user's space: Explore, My listings and Requests behind a bottom bar. Detail, sell and edit are separate full-screen routes. */
@Composable
fun UserHomeScreen(
    onOpenListing: (String) -> Unit,
    onSell: () -> Unit,
    onEditListing: (String) -> Unit,
    onNewExchange: () -> Unit,
    onOpenExchange: (String) -> Unit,
    onRequestRepair: () -> Unit,
    onOpenRepair: (String) -> Unit,
    onRecycle: () -> Unit,
    onOpenRecycling: (String) -> Unit,
    onOpenMaterial: (String) -> Unit,
    onLoggedOut: () -> Unit,
) {
    val homeViewModel: HomeViewModel = viewModel(factory = ViewModelFactory(LocalAppContainer.current))
    var selectedTab by rememberSaveable { mutableStateOf(HomeTab.EXPLORE) }
    var servicesSection by rememberSaveable { mutableStateOf(ServicesSection.REPAIR) }
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            AppTopBar(
                title = selectedTab.label,
                actions = {
                    IconButton(onClick = { homeViewModel.logout(onLoggedOut) }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Log out")
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                HomeTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
        floatingActionButton = {
            when (selectedTab) {
                HomeTab.EXPLORE, HomeTab.MY_LISTINGS -> ExtendedFloatingActionButton(
                    onClick = onSell,
                    modifier = Modifier.semantics { contentDescription = "Sell an item" },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Sell") },
                )
                HomeTab.EXCHANGE -> ExtendedFloatingActionButton(
                    onClick = onNewExchange,
                    modifier = Modifier.semantics { contentDescription = "Propose an exchange" },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("New exchange") },
                )
                HomeTab.MY_REQUESTS -> Unit
                HomeTab.SERVICES -> if (servicesSection != ServicesSection.MATERIALS) {
                    val isRepair = servicesSection == ServicesSection.REPAIR
                    ExtendedFloatingActionButton(
                        onClick = if (isRepair) onRequestRepair else onRecycle,
                        modifier = Modifier.semantics { contentDescription = if (isRepair) "Request a repair" else "Recycle furniture" },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        text = { Text(if (isRepair) "Request repair" else "Recycle") },
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        val contentModifier = Modifier.padding(padding)
        when (selectedTab) {
            HomeTab.EXPLORE -> ExploreScreen(onOpenListing = onOpenListing, onSell = onSell, modifier = contentModifier)
            HomeTab.EXCHANGE -> ExchangeRequestsScreen(
                onOpenRequest = onOpenExchange,
                onNewExchange = onNewExchange,
                modifier = contentModifier,
            )
            HomeTab.MY_LISTINGS -> MyListingsScreen(
                snackbarHostState = snackbarHostState,
                onOpenListing = onOpenListing,
                onEditListing = onEditListing,
                onSell = onSell,
                modifier = contentModifier,
            )
            HomeTab.MY_REQUESTS -> MyRequestsScreen(
                snackbarHostState = snackbarHostState,
                onOpenListing = onOpenListing,
                modifier = contentModifier,
            )
            HomeTab.SERVICES -> ServicesScreen(
                section = servicesSection,
                onSectionChange = { servicesSection = it },
                onOpenRepair = onOpenRepair,
                onRequestRepair = onRequestRepair,
                onOpenRecycling = onOpenRecycling,
                onRecycle = onRecycle,
                onOpenMaterial = onOpenMaterial,
                modifier = contentModifier,
            )
        }
    }
}
