package com.homesajja.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.homesajja.app.ui.theme.OnPoignantPinkSecondaryContainer
import com.homesajja.app.ui.theme.OnSuccessContainer
import com.homesajja.app.ui.theme.PoignantPinkSecondaryContainer
import com.homesajja.app.ui.theme.SuccessContainer

/**
 * Generic status pill shared across Marketplace, Exchange, Repair and Recycle.
 * Each system passes its own status string; unrecognized values fall back to a
 * neutral color instead of crashing, since new statuses will be added per system.
 */
@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier,
) {
    val (container, content) = statusColors(status)
    Text(
        text = status,
        style = MaterialTheme.typography.labelMedium,
        color = content,
        modifier = modifier
            .background(container, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 4.dp),
    )
}

@Composable
private fun statusColors(status: String): Pair<Color, Color> {
    val scheme = MaterialTheme.colorScheme
    return when (status.trim().lowercase()) {
        "pending", "scheduled", "in progress", "in-progress" -> PoignantPinkSecondaryContainer to OnPoignantPinkSecondaryContainer
        "accepted", "approved", "ready", "completed", "done" -> SuccessContainer to OnSuccessContainer
        "rejected", "cancelled", "canceled", "failed" -> scheme.errorContainer to scheme.onErrorContainer
        else -> scheme.surfaceVariant to scheme.onSurfaceVariant
    }
}
