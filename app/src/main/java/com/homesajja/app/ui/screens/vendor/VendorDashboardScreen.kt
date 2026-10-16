package com.homesajja.app.ui.screens.vendor

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Explore
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
import com.homesajja.app.ui.i18n.LocalAppLanguage
import com.homesajja.app.ui.i18n.VendorStrings
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
    val isHindi = LocalAppLanguage.current.isHindi
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Header(data, isHindi)

        if (data.isNew) {
            WelcomeCard(hasListings = data.activeListings > 0, onAddListing = onAddListing, isHindi = isHindi)
        } else {
            EarningsCard(summary, isHindi)
            ChartCard(summary, isHindi)
            RatingCard(summary, isHindi)
            Text(VendorStrings.yourWork(isHindi), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                SummaryTile(VendorStrings.completed(isHindi), summary.tasks.completed, Modifier.weight(1f))
                SummaryTile(VendorStrings.pending(isHindi), summary.tasks.pending, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                TypeTile(VendorStrings.sales(isHindi), summary.tasks.sales, isHindi, Modifier.weight(1f))
                TypeTile(VendorStrings.repairs(isHindi), summary.tasks.repairs, isHindi, Modifier.weight(1f))
            }
            TypeTile(VendorStrings.exchanges(isHindi), summary.tasks.exchanges, isHindi, Modifier.fillMaxWidth())
        }

        Text(VendorStrings.goTo(isHindi), style = MaterialTheme.typography.titleMedium)
        Shortcut(Icons.Filled.Sell, VendorStrings.myListings(isHindi), VendorStrings.onSaleNow(data.activeListings, isHindi)) { onOpenTab(VendorTab.LISTINGS) }
        Shortcut(Icons.AutoMirrored.Filled.ReceiptLong, VendorStrings.incomingRequests(isHindi), VendorStrings.pendingText(summary.tasks.pending, isHindi)) { onOpenTab(VendorTab.REQUESTS) }
        Shortcut(Icons.Filled.Explore, VendorStrings.exploreMarketplace(isHindi), VendorStrings.browseAllListings(isHindi)) { onOpenTab(VendorTab.EXPLORE) }
        Shortcut(Icons.Filled.Assessment, VendorStrings.monthlyReport(isHindi), VendorStrings.exportReportDesc(isHindi), onOpenReports)

        if (data.profileIncomplete) {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(VendorStrings.finishProfileTitle(isHindi), style = MaterialTheme.typography.titleSmall)
                    Text(VendorStrings.finishProfileDesc(isHindi), style = MaterialTheme.typography.bodyMedium)
                    OutlinedButton(text = VendorStrings.editProfile(isHindi), onClick = onEditProfile)
                }
            }
        }
    }
}

private fun pendingText(pending: Int) = if (pending == 0) "Nothing waiting" else "$pending waiting for you"

@Composable
private fun Header(data: DashboardData, isHindi: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(data.vendor.businessName.ifBlank { data.vendor.name }, style = MaterialTheme.typography.headlineMedium)
        Text(
            "${VendorStrings.businessType(data.vendor.businessType, isHindi)} · ${data.vendor.city}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (data.vendor.verified) {
            VerifiedBadge()
        } else {
            Text(VendorStrings.notVerifiedYet(isHindi), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** What a vendor with nothing yet sees instead of a wall of zeros. */
@Composable
private fun WelcomeCard(hasListings: Boolean, onAddListing: () -> Unit, isHindi: Boolean) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(VendorStrings.welcomeTitle(isHindi), style = MaterialTheme.typography.titleMedium)
            Text(
                VendorStrings.welcomeDesc(hasListings, isHindi),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PrimaryButton(text = VendorStrings.addFirstListing(hasListings, isHindi), onClick = onAddListing, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun EarningsCard(summary: DashboardSummary, isHindi: Boolean) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(VendorStrings.totalEarned(isHindi), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(
                formatPrice(summary.totalEarned) + if (summary.earnedIsPartial) "+" else "",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(VendorStrings.thisMonth(isHindi), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(formatPrice(summary.earnedThisMonth), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Text(
                VendorStrings.earnedNote(summary.earnedIsPartial, isHindi),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun ChartCard(summary: DashboardSummary, isHindi: Boolean) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(VendorStrings.earnedEachMonth(isHindi), style = MaterialTheme.typography.titleSmall)
            if (summary.months.all { it.amount == 0L }) {
                Text(
                    VendorStrings.noPaymentsYet(isHindi),
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
private fun RatingCard(summary: DashboardSummary, isHindi: Boolean) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Filled.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            if (summary.rating.hasRatings) {
                Text(String.format(java.util.Locale.US, "%.1f", summary.rating.average), style = MaterialTheme.typography.titleLarge)
                Text(
                    if (isHindi) "औसत रेटिंग · ${summary.rating.count} समीक्षाएं" else "average rating · ${summary.rating.count} review${if (summary.rating.count == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(VendorStrings.noReviewsYet(isHindi), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SummaryTile(label: String, value: Int, modifier: Modifier = Modifier) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("$value", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TypeTile(label: String, count: TaskCount, isHindi: Boolean, modifier: Modifier = Modifier) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(if (isHindi) "${count.completed} पूर्ण" else "${count.completed} done", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text(if (isHindi) "${count.pending} लंबित" else "${count.pending} pending", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Shortcut(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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
