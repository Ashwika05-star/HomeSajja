package com.homesajja.app.ui.screens.vendor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.homesajja.app.data.model.VendorBusinessType
import com.homesajja.app.di.LocalAppContainer
import com.homesajja.app.di.ViewModelFactory
import com.homesajja.app.ui.components.ErrorState
import com.homesajja.app.ui.components.LoadingState
import com.homesajja.app.ui.components.OutlinedButton
import com.homesajja.app.ui.components.StatusBadge
import com.homesajja.app.ui.components.VerifiedBadge
import com.homesajja.app.ui.util.formatTimeAgo
import com.homesajja.app.viewmodel.ActivityItem
import com.homesajja.app.viewmodel.DashboardData
import com.homesajja.app.viewmodel.VendorDashboardUiState
import com.homesajja.app.viewmodel.VendorDashboardViewModel

/** The vendor's home tab. Tapping an activity item opens that request; purchase requests open the Requests tab. */
@Composable
fun VendorDashboardScreen(
    onOpenActivity: (ActivityItem) -> Unit,
    onEditProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: VendorDashboardViewModel = viewModel(factory = ViewModelFactory(LocalAppContainer.current))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleResumeEffect(viewModel) {
        viewModel.refreshIfStale()
        onPauseOrDispose {}
    }

    when (val current = state) {
        VendorDashboardUiState.Loading -> LoadingState(modifier)
        is VendorDashboardUiState.Error -> ErrorState(message = current.message, onRetry = viewModel::retry, modifier = modifier)
        is VendorDashboardUiState.Content -> DashboardContent(current.data, onOpenActivity, onEditProfile, modifier)
    }
}

@Composable
private fun DashboardContent(
    data: DashboardData,
    onOpenActivity: (ActivityItem) -> Unit,
    onEditProfile: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(data.vendor.businessName.ifBlank { data.vendor.name }, style = MaterialTheme.typography.headlineMedium)
            Text(
                "${VendorBusinessType.fromNameOrNull(data.vendor.businessType)?.displayName ?: "Vendor"} · ${data.vendor.city}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (data.vendor.verified) {
                VerifiedBadge()
            } else {
                Text("Not verified yet", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard("Active listings", data.stats.activeListings, Modifier.weight(1f))
            StatCard("Pending requests", data.stats.pendingRequests, Modifier.weight(1f))
            StatCard("Completed sales", data.stats.completedSales, Modifier.weight(1f))
        }

        if (data.profileIncomplete) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Finish your shop profile", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Add a description and your shop location so customers can find you.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    OutlinedButton(text = "Edit profile", onClick = onEditProfile)
                }
            }
        }

        Text("Recent activity", style = MaterialTheme.typography.titleMedium)
        if (data.activity.isEmpty()) {
            Text(
                "Nothing yet. Requests on your listings and services will show up here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            data.activity.forEach { item -> ActivityRow(item, onClick = { onOpenActivity(item) }) }
        }
    }
}

@Composable
private fun StatCard(label: String, value: Int, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("$value", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ActivityRow(item: ActivityItem, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${item.kind.label} · ${formatTimeAgo(item.timestamp)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusBadge(status = item.statusLabel)
        }
    }
}
