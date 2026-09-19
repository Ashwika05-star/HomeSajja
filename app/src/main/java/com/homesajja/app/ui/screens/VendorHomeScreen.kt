package com.homesajja.app.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sell
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
import com.homesajja.app.ui.components.InboxActions
import com.homesajja.app.ui.components.RequestNotificationPermission
import com.homesajja.app.ui.screens.material.VendorMaterialsScreen
import com.homesajja.app.ui.screens.mylistings.MyListingsScreen
import com.homesajja.app.ui.screens.vendor.VendorDashboardScreen
import com.homesajja.app.ui.screens.vendor.VendorPublicProfileScreen
import com.homesajja.app.ui.screens.vendor.VendorRequestsScreen
import com.homesajja.app.viewmodel.ActivityKind
import com.homesajja.app.viewmodel.HomeViewModel

private enum class VendorTab(val label: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Filled.Dashboard),
    LISTINGS("Listings", Icons.Filled.Sell),
    REQUESTS("Requests", Icons.AutoMirrored.Filled.ReceiptLong),
    MATERIALS("Materials", Icons.Filled.Inventory2),
    PROFILE("Profile", Icons.Filled.Person),
}

/** The vendor's space: dashboard, listing management, incoming requests, material requests and shop profile. */
@Composable
fun VendorHomeScreen(
    onOpenListing: (String) -> Unit,
    onAddListing: () -> Unit,
    onEditListing: (String) -> Unit,
    onOpenExchange: (String) -> Unit,
    onOpenRepair: (String) -> Unit,
    onOpenRecycling: (String) -> Unit,
    onNewMaterialRequest: () -> Unit,
    onOpenMaterialRequest: (String) -> Unit,
    onEditProfile: () -> Unit,
    onOpenChats: () -> Unit,
    onOpenNotifications: () -> Unit,
    onLoggedOut: () -> Unit,
) {
    val homeViewModel: HomeViewModel = viewModel(factory = ViewModelFactory(LocalAppContainer.current))
    var selectedTab by rememberSaveable { mutableStateOf(VendorTab.DASHBOARD) }
    val snackbarHostState = remember { SnackbarHostState() }
    RequestNotificationPermission()

    Scaffold(
        topBar = {
            AppTopBar(
                title = selectedTab.label,
                actions = {
                    InboxActions(onOpenChats, onOpenNotifications)
                    IconButton(onClick = { homeViewModel.logout(onLoggedOut) }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Log out")
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                VendorTab.entries.forEach { tab ->
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
                VendorTab.LISTINGS -> AddFab("Add listing", "Add a listing", onAddListing)
                VendorTab.MATERIALS -> AddFab("New request", "Post a material request", onNewMaterialRequest)
                else -> Unit
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        val contentModifier = Modifier.padding(padding)
        when (selectedTab) {
            VendorTab.DASHBOARD -> VendorDashboardScreen(
                onOpenActivity = { item ->
                    when (item.kind) {
                        ActivityKind.PURCHASE -> selectedTab = VendorTab.REQUESTS
                        ActivityKind.EXCHANGE -> onOpenExchange(item.requestId)
                        ActivityKind.REPAIR -> onOpenRepair(item.requestId)
                        ActivityKind.RECYCLING -> onOpenRecycling(item.requestId)
                    }
                },
                onEditProfile = onEditProfile,
                modifier = contentModifier,
            )
            VendorTab.LISTINGS -> MyListingsScreen(
                snackbarHostState = snackbarHostState,
                onOpenListing = onOpenListing,
                onEditListing = onEditListing,
                onSell = onAddListing,
                modifier = contentModifier,
                allowAvailabilityToggle = true,
            )
            VendorTab.REQUESTS -> VendorRequestsScreen(
                snackbarHostState = snackbarHostState,
                onOpenListing = onOpenListing,
                onOpenExchange = onOpenExchange,
                onOpenRepair = onOpenRepair,
                onOpenRecycling = onOpenRecycling,
                modifier = contentModifier,
            )
            VendorTab.MATERIALS -> VendorMaterialsScreen(
                onOpenRequest = onOpenMaterialRequest,
                onNewRequest = onNewMaterialRequest,
                modifier = contentModifier,
            )
            VendorTab.PROFILE -> VendorPublicProfileScreen(
                onOpenListing = onOpenListing,
                onEditProfile = onEditProfile,
                modifier = contentModifier,
            )
        }
    }
}

@Composable
private fun AddFab(label: String, description: String, onClick: () -> Unit) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        modifier = Modifier.semantics { contentDescription = description },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
        text = { Text(label) },
    )
}
