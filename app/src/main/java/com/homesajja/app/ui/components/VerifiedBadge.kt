package com.homesajja.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** "Verified" tick next to a vendor's name. Placeholder: it just reflects the profile's `verified` flag. */
@Composable
fun VerifiedBadge(modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Text("  Verified", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
    }
}
