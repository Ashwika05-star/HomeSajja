package com.homesajja.app.ui.screens.recycle

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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
import com.homesajja.app.data.model.RatingSummary
import com.homesajja.app.data.model.VendorProfile
import com.homesajja.app.ui.components.RatingLine
import com.homesajja.app.ui.components.VerifiedBadge

/** A recycler as a selectable card for drop-off: name, city and, once the vendor has set it, the shop address. */
@Composable
fun RecyclerCard(
    recycler: VendorProfile,
    rating: RatingSummary?,
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
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(recycler.businessName.ifBlank { recycler.name }, style = MaterialTheme.typography.titleMedium)
                Text(
                    recycler.shopAddress.ifBlank { "Shop address not added yet" } + " · ${recycler.city}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                RatingLine(rating)
                if (recycler.verified) VerifiedBadge()
                TextButton(onClick = onViewProfile) { Text("View profile") }
            }
            if (selected) Icon(Icons.Filled.CheckCircle, contentDescription = "Selected", tint = primary)
        }
    }
}
