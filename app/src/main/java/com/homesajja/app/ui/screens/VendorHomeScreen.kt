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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.homesajja.app.di.LocalAppContainer
import com.homesajja.app.di.ViewModelFactory
import com.homesajja.app.ui.components.AppTopBar
import com.homesajja.app.ui.screens.repair.VendorRepairsList
import com.homesajja.app.viewmodel.HomeViewModel

/**
 * TEMPORARY vendor home: only the repair requests addressed to this vendor, so the repair
 * pipeline can be exercised end to end. Replaced by the full Vendor Dashboard in Phase 8.
 */
@Composable
fun VendorHomeScreen(onOpenRepair: (String) -> Unit, onLoggedOut: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: HomeViewModel = viewModel(factory = ViewModelFactory(container))

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Incoming repairs",
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
            VendorRepairsList(onOpenRequest = onOpenRepair)
        }
    }
}
