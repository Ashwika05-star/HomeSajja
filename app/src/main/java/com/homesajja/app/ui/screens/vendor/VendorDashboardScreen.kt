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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.homesajja.app.data.model.VendorBusinessType
import com.homesajja.app.di.LocalAppContainer
import com.homesajja.app.di.ViewModelFactory
import com.homesajja.app.navigation.VendorTab
import com.homesajja.app.ui.components.EarningsChart
import com.homesajja.app.ui.components.ErrorState
import com.homesajja.app.ui.components.LoadingState
import com.homesajja.app.ui.components.OutlinedButton
import com.homesajja.app.ui.components.PrimaryButton
import com.homesajja.app.ui.components.VerifiedBadge
import com.homesajja.app.ui.util.formatPrice
import com.homesajja.app.viewmodel.DashboardData
import com.homesajja.app.viewmodel.DashboardSummary
import com.homesajja.app.viewmodel.TaskCount
import com.homesajja.app.viewmodel.VendorDashboardUiState
import com.homesajja.app.viewmodel.VendorDashboardViewModel

/**
 * The vendor's home tab: a few clear numbers (what they earned, what they've done and have waiting, their rating) and shortcuts to
 * their listings, incoming requests, material requests and the monthly report.
 */
@Composable
fun VendorDashboardScreen(
    onOpenTab: (VendorTab) -> Unit,
    onOpenReports: () -> Unit,
    onAddListing: () -> Unit,
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
        is VendorDashboardUiState.Content -> DashboardContent(current.data, onOpenTab, onOpenReports, onAddListing, onEditProfile, modifier)
    }
}

@Composable
private fun DashboardContent(
    data: DashboardData,
    onOpenTab: (VendorTab) -> Unit,
    onOpenReports: () -> Unit,
    onAddListing: () -> Unit,
    onEditProfile: () -> Unit,
    modifier: Modifier,
) {
    val summary = data.summary
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Header(data)

        if (data.isNew) {
            WelcomeCard(hasListings = data.activeListings > 0, onAddListing = onAddListing)
        } else {
            EarningsCard(summary)
            ChartCard(summary)
            RatingCard(summary)
            Text("Your work", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                SummaryTile("Completed", summary.tasks.completed, Modifier.weight(1f))
                SummaryTile("Pending", summary.tasks.pending, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                TypeTile("Sales", summary.tasks.sales, Modifier.weight(1f))
                TypeTile("Repairs", summary.tasks.repairs, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                TypeTile("Exchanges", summary.tasks.exchanges, Modifier.weight(1f))
                TypeTile("Recycling", summary.tasks.recycling, Modifier.weight(1f))
            }
        }

        Text("Go to", style = MaterialTheme.typography.titleMedium)
        Shortcut(Icons.Filled.Sell, "My listings", "${data.activeListings} on sale now") { onOpenTab(VendorTab.LISTINGS) }
        Shortcut(Icons.AutoMirrored.Filled.ReceiptLong, "Incoming requests", pendingText(summary.tasks.pending)) { onOpenTab(VendorTab.REQUESTS) }
        Shortcut(Icons.Filled.Inventory2, "Material requests", "Ask people for wood, fabric and more") { onOpenTab(VendorTab.MATERIALS) }
        Shortcut(Icons.Filled.Assessment, "Monthly report", "Export a month as PDF or CSV", onOpenReports)

        if (data.profileIncomplete) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Finish your shop profile", style = MaterialTheme.typography.titleSmall)
                    Text("Add a description and your shop location so customers can find you.", style = MaterialTheme.typography.bodyMedium)
                    OutlinedButton(text = "Edit profile", onClick = onEditProfile)
                }
            }
        }
    }
}

private fun pendingText(pending: Int) = if (pending == 0) "Nothing waiting" else "$pending waiting for you"

@Composable
private fun Header(data: DashboardData) {
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
}

/** What a vendor with nothing yet sees instead of a wall of zeros. */
@Composable
private fun WelcomeCard(hasListings: Boolean, onAddListing: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Welcome to HomeSajja", style = MaterialTheme.typography.titleMedium)
            Text(
                if (hasListings) "Your listings are live. Once customers send requests and pay you, your earnings, finished work and rating show up on this page."
                else "Nothing here yet. Once you list something, answer requests and customers pay you, your earnings, finished work and rating show up on this page.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PrimaryButton(text = if (hasListings) "Add another listing" else "Add your first listing", onClick = onAddListing, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun EarningsCard(summary: DashboardSummary) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Total earned", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(
                formatPrice(summary.totalEarned) + if (summary.earnedIsPartial) "+" else "",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("This month", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(formatPrice(summary.earnedThisMonth), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Text(
                if (summary.earnedIsPartial) "Counts your most recent confirmed payments." else "Counts only payments you confirmed as received.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun ChartCard(summary: DashboardSummary) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Earned each month", style = MaterialTheme.typography.titleSmall)
            if (summary.months.all { it.amount == 0L }) {
                Text(
                    "No confirmed payments in the last 6 months yet. When someone pays you and you tap \"Payment received\", it appears here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                EarningsChart(summary.months)
            }
        }
    }
}

@Composable
private fun RatingCard(summary: DashboardSummary) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Filled.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            if (summary.rating.hasRatings) {
                Text(String.format(java.util.Locale.US, "%.1f", summary.rating.average), style = MaterialTheme.typography.titleLarge)
                Text(
                    "average rating · ${summary.rating.count} review${if (summary.rating.count == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text("No reviews yet", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SummaryTile(label: String, value: Int, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("$value", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TypeTile(label: String, count: TaskCount, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${count.completed} done", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text("${count.pending} pending", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Shortcut(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
