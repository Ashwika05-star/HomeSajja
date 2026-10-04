package com.homesajja.app.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.runtime.collectAsState
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
import com.homesajja.app.navigation.UserTab
import com.homesajja.app.ui.components.AppTopBar
import com.homesajja.app.ui.components.BottomTabBar
import com.homesajja.app.ui.components.InboxActions
import com.homesajja.app.ui.components.RequestNotificationPermission
import com.homesajja.app.ui.screens.exchange.ExchangeRequestsScreen
import com.homesajja.app.ui.screens.explore.ExploreScreen
import com.homesajja.app.ui.screens.mylistings.MyListingsScreen
import com.homesajja.app.ui.screens.requests.MyRequestsScreen
import com.homesajja.app.viewmodel.HomeViewModel

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
    onOpenChats: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSajja: () -> Unit,
    onOpenProfile: () -> Unit,
    onLoggedOut: () -> Unit,
) {
    val homeViewModel: HomeViewModel = viewModel(factory = ViewModelFactory(LocalAppContainer.current))
    val container = LocalAppContainer.current
    val selectedTab by container.homeTabs.user.collectAsState()
    var servicesSection by rememberSaveable { mutableStateOf(ServicesSection.REPAIR) }
    val snackbarHostState = remember { SnackbarHostState() }
    RequestNotificationPermission()

    Scaffold(
        topBar = {
            AppTopBar(
                title = selectedTab.label,
                actions = {
                    IconButton(onClick = onOpenSajja) { Icon(Icons.Filled.AutoAwesome, contentDescription = "Ask Sajja AI") }
                    InboxActions(onOpenChats, onOpenNotifications)
                    IconButton(onClick = onOpenProfile) { Icon(Icons.Filled.Person, contentDescription = "Profile") }
                },
            )
        },
        bottomBar = {
            BottomTabBar(
                tabs = UserTab.entries,
                selected = selectedTab,
                label = { it.label },
                icon = { it.icon },
                onSelect = { container.homeTabs.user.value = it },
            )
        },
        floatingActionButton = {
            when (selectedTab) {
                UserTab.EXPLORE, UserTab.MY_LISTINGS -> ExtendedFloatingActionButton(
                    onClick = onSell,
                    modifier = Modifier.semantics { contentDescription = "Sell an item" },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Sell") },
                )
                UserTab.EXCHANGE -> ExtendedFloatingActionButton(
                    onClick = onNewExchange,
                    modifier = Modifier.semantics { contentDescription = "Propose an exchange" },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("New exchange") },
                )
                UserTab.MY_REQUESTS -> Unit
                UserTab.SERVICES -> if (servicesSection != ServicesSection.MATERIALS) {
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
            UserTab.EXPLORE -> ExploreScreen(onOpenListing = onOpenListing, onSell = onSell, modifier = contentModifier)
            UserTab.EXCHANGE -> ExchangeRequestsScreen(
                onOpenRequest = onOpenExchange,
                onNewExchange = onNewExchange,
                modifier = contentModifier,
            )
            UserTab.MY_LISTINGS -> MyListingsScreen(
                snackbarHostState = snackbarHostState,
                onOpenListing = onOpenListing,
                onEditListing = onEditListing,
                onSell = onSell,
                modifier = contentModifier,
            )
            UserTab.MY_REQUESTS -> MyRequestsScreen(
                snackbarHostState = snackbarHostState,
                onOpenListing = onOpenListing,
                modifier = contentModifier,
            )
            UserTab.SERVICES -> ServicesScreen(
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
