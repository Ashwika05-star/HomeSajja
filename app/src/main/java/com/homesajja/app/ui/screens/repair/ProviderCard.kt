package com.homesajja.app.ui.screens.repair

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.homesajja.app.data.model.RepairProblemType
import com.homesajja.app.data.model.VendorBusinessType
import com.homesajja.app.data.model.VendorProfile
import com.homesajja.app.ui.util.formatPrice

/** A repair provider as a selectable card: name, rating placeholder, services and estimated cost range when given. */
@Composable
fun ProviderCard(
    provider: VendorProfile,
    selected: Boolean,
    onClick: () -> Unit,
    onViewProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (selected) BorderStroke(2.dp, primary) else null,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(provider.businessName.ifBlank { provider.name }, style = MaterialTheme.typography.titleMedium)
                    Text(
                        VendorBusinessType.fromNameOrNull(provider.businessType)?.displayName ?: "Vendor",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (selected) Icon(Icons.Filled.CheckCircle, contentDescription = "Selected", tint = primary)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Filled.StarBorder, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                // Placeholder until reviews are wired up in a later phase.
                Text("No ratings yet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = servicesLine(provider),
                style = MaterialTheme.typography.bodyMedium,
            )
            costRange(provider)?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = primary)
            }
            TextButton(onClick = onViewProfile) { Text("View profile") }
        }
    }
}

private fun servicesLine(provider: VendorProfile): String {
    val services = provider.repairServices.mapNotNull { name -> RepairProblemType.entries.firstOrNull { it.name == name }?.displayName }
    return if (services.isEmpty()) "Services not listed yet" else services.joinToString(" · ")
}

private fun costRange(provider: VendorProfile): String? {
    val min = provider.repairCostMin
    val max = provider.repairCostMax
    return when {
        min != null && max != null -> "Estimated ${formatPrice(min)} – ${formatPrice(max)}"
        min != null -> "From ${formatPrice(min)}"
        max != null -> "Up to ${formatPrice(max)}"
        else -> null
    }
}
