package com.homesajja.app.ui.screens.material

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.homesajja.app.data.model.MaterialRequest
import com.homesajja.app.ui.components.StatusBadge
import com.homesajja.app.ui.util.budgetLabel

/** One material request in a list: what is wanted, how much, the budget and who is asking. */
@Composable
fun MaterialRequestCard(request: MaterialRequest, showVendor: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    request.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                StatusBadge(status = request.status.displayName)
            }
            Text(
                "${request.materialType.displayName} · ${request.quantity}",
                style = MaterialTheme.typography.bodyMedium,
            )
            budgetLabel(request.budgetMin, request.budgetMax)?.let {
                Text("Budget $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
            if (showVendor) {
                Text(
                    "${request.vendorName} · ${request.city}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
