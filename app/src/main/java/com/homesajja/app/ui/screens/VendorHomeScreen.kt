package com.homesajja.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.homesajja.app.di.LocalAppContainer
import com.homesajja.app.di.ViewModelFactory
import com.homesajja.app.ui.components.AppTopBar
import com.homesajja.app.ui.screens.recycle.VendorRecyclingList
import com.homesajja.app.ui.screens.repair.VendorRepairsList
import com.homesajja.app.viewmodel.HomeViewModel

/**
 * TEMPORARY vendor home: only the repair and recycling requests addressed to this vendor, so those
 * pipelines can be exercised end to end. Replaced by the full Vendor Dashboard in Phase 8.
 */
@Composable
fun VendorHomeScreen(onOpenRepair: (String) -> Unit, onOpenRecycling: (String) -> Unit, onLoggedOut: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: HomeViewModel = viewModel(factory = ViewModelFactory(container))
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Incoming requests",
                actions = {
                    IconButton(onClick = { viewModel.logout(onLoggedOut) }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Log out")
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Text(
                text = "${viewModel.displayName} · Vendor. Temporary screen until the Vendor Dashboard (Phase 8).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            TabRow(selectedTabIndex = selectedTab, containerColor = MaterialTheme.colorScheme.background) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Repairs") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Recycling") })
            }
            if (selectedTab == 0) VendorRepairsList(onOpenRequest = onOpenRepair) else VendorRecyclingList(onOpenRequest = onOpenRecycling)
        }
    }
}
