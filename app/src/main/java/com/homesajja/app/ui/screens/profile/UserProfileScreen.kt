package com.homesajja.app.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Card
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.homesajja.app.di.LocalAppContainer
import com.homesajja.app.di.ViewModelFactory
import com.homesajja.app.ui.components.AppTextField
import com.homesajja.app.ui.components.CopyableValue
import com.homesajja.app.ui.components.DeleteAccountSection
import com.homesajja.app.ui.components.ErrorState
import com.homesajja.app.ui.components.LoadingState
import com.homesajja.app.ui.components.OutlinedButton
import com.homesajja.app.ui.components.PrimaryButton
import com.homesajja.app.ui.components.ReviewsSection
import com.homesajja.app.ui.util.formatPrice
import com.homesajja.app.viewmodel.ProfileSummary
import com.homesajja.app.viewmodel.SummaryState
import com.homesajja.app.viewmodel.UserProfileUiState
import com.homesajja.app.viewmodel.UserProfileViewModel

/**
 * A person's profile with the reviews written about them. On your own profile it also links to your saved
 * furniture and blocked people, and has Log out. On someone else's, that's just their name and reviews
 * (their account details are private).
 */
@Composable
fun UserProfileScreen(
    modifier: Modifier = Modifier,
    onOpenSaved: () -> Unit = {},
    onOpenBlocked: () -> Unit = {},
    onLogout: () -> Unit = {},
    onAccountDeleted: () -> Unit = {},
) {
    val viewModel: UserProfileViewModel = viewModel(factory = ViewModelFactory(LocalAppContainer.current))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleResumeEffect(viewModel) {
        viewModel.refreshIfStale()
        onPauseOrDispose {}
    }

    when (val current = state) {
        UserProfileUiState.Loading -> LoadingState(modifier)
        is UserProfileUiState.Error -> ErrorState(message = current.message, onRetry = viewModel::retry, modifier = modifier)
        is UserProfileUiState.Content -> Column(
            modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(current.name, style = MaterialTheme.typography.headlineMedium)
                current.profile?.let {
                    Text(it.city, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(it.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (current.isOwn) {
                SummaryCards(viewModel.summary, onRetry = viewModel::retrySummary)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Payments", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Add your UPI ID so people can pay you, for example when you sell something. Only the people you do a deal with see it.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    viewModel.savedUpiId?.let { CopyableValue(label = "Your UPI ID", value = it) }
                    AppTextField(
                        value = viewModel.upiDraft,
                        onValueChange = viewModel::onUpiDraftChange,
                        label = "UPI ID",
                        placeholder = "name@bank",
                        isError = viewModel.upiDraftInvalid,
                        errorMessage = if (viewModel.upiDraftInvalid) "That doesn't look like a UPI ID, e.g. name@okhdfcbank" else null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    viewModel.upiMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
                    PrimaryButton(
                        text = if (viewModel.upiSaving) "Saving…" else "Save UPI ID",
                        onClick = viewModel::saveUpiId,
                        enabled = !viewModel.upiSaving && !viewModel.upiDraftInvalid,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                OutlinedButton(text = "Saved furniture", onClick = onOpenSaved, modifier = Modifier.fillMaxWidth())
                OutlinedButton(text = "Blocked people", onClick = onOpenBlocked, modifier = Modifier.fillMaxWidth())
            }
            ReviewsSection(current.reviews)
            if (current.isOwn) {
                OutlinedButton(text = "Log out", onClick = onLogout, modifier = Modifier.fillMaxWidth())
                DeleteAccountSection(onDeleted = onAccountDeleted)
            }
        }
    }
}

/** Your earnings from selling and your sustainability numbers: a card each, with a loading, an error and an empty state. */
@Composable
private fun SummaryCards(state: SummaryState, onRetry: () -> Unit) {
    when (state) {
        SummaryState.Loading -> Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(strokeWidth = 2.dp)
            }
        }
        is SummaryState.Error -> Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(state.message, style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(text = "Try again", onClick = onRetry)
            }
        }
        is SummaryState.Loaded -> {
            EarningsCard(state.summary)
            SustainabilityCard(state.summary)
        }
    }
}

@Composable
private fun EarningsCard(summary: ProfileSummary) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Total earned", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(
                        formatPrice(summary.totalEarned) + if (summary.earnedIsPartial) "+" else "",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Items sold", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("${summary.itemsSold}", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Text(
                if (summary.itemsSold == 0 && summary.totalEarned == 0L) "Sell something and confirm the payment, and it shows up here."
                else "From your completed sales. Only payments you confirmed as received count.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun SustainabilityCard(summary: ProfileSummary) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Your sustainability", style = MaterialTheme.typography.titleSmall)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SustainabilityStat("Reused", summary.itemsReused, Modifier.weight(1f))
                SustainabilityStat("Repaired", summary.itemsRepaired, Modifier.weight(1f))
                SustainabilityStat("Recycled", summary.itemsRecycled, Modifier.weight(1f))
            }
            Text(
                if (summary.isEmpty) "Buy, sell, swap, repair or recycle furniture and your count starts here."
                else "Pieces of furniture you kept in use instead of throwing away: bought, sold or swapped, repaired and recycled.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SustainabilityStat(label: String, value: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$value", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
