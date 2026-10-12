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
import com.homesajja.app.ui.theme.LocalDarkTheme
import com.homesajja.app.ui.theme.StatusAgreedDarkBg
import com.homesajja.app.ui.theme.StatusAgreedDarkText
import com.homesajja.app.ui.theme.StatusAgreedLightBg
import com.homesajja.app.ui.theme.StatusAgreedLightText
import com.homesajja.app.ui.theme.StatusInProgressDarkBg
import com.homesajja.app.ui.theme.StatusInProgressDarkText
import com.homesajja.app.ui.theme.StatusInProgressLightBg
import com.homesajja.app.ui.theme.StatusInProgressLightText
import com.homesajja.app.ui.theme.StatusQuotedDarkBg
import com.homesajja.app.ui.theme.StatusQuotedDarkText
import com.homesajja.app.ui.theme.StatusQuotedLightBg
import com.homesajja.app.ui.theme.StatusQuotedLightText
import com.homesajja.app.ui.theme.StatusReservedDarkBg
import com.homesajja.app.ui.theme.StatusReservedDarkText
import com.homesajja.app.ui.theme.StatusReservedLightBg
import com.homesajja.app.ui.theme.StatusReservedLightText

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
    val dark = LocalDarkTheme.current
    return when (status.trim().lowercase()) {
        "pending", "scheduled", "in progress", "in-progress" ->
            if (dark) StatusInProgressDarkBg to StatusInProgressDarkText else StatusInProgressLightBg to StatusInProgressLightText
        "accepted", "approved", "ready", "completed", "done", "agreed" ->
            if (dark) StatusAgreedDarkBg to StatusAgreedDarkText else StatusAgreedLightBg to StatusAgreedLightText
        "quoted" ->
            if (dark) StatusQuotedDarkBg to StatusQuotedDarkText else StatusQuotedLightBg to StatusQuotedLightText
        "reserved" ->
            if (dark) StatusReservedDarkBg to StatusReservedDarkText else StatusReservedLightBg to StatusReservedLightText
        "rejected", "cancelled", "canceled", "failed" -> scheme.errorContainer to scheme.onErrorContainer
        else -> scheme.surfaceContainerHigh to scheme.onSurfaceVariant
    }
}
