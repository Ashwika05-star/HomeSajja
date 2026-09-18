package com.homesajja.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.homesajja.app.data.model.PurchaseStatus

private val TRACKED_STEPS = listOf(
    PurchaseStatus.REQUESTED,
    PurchaseStatus.ACCEPTED,
    PurchaseStatus.READY_FOR_PICKUP,
    PurchaseStatus.COMPLETED,
)

/**
 * Shows where a Buy/Sell request is in Requested -> Accepted -> Ready for pickup
 * -> Completed. A rejected or cancelled request has left that path, so it shows
 * a badge instead. Specific to purchase requests — other systems get their own trackers.
 */
@Composable
fun PurchaseStatusTracker(status: PurchaseStatus, modifier: Modifier = Modifier) {
    val currentIndex = TRACKED_STEPS.indexOf(status)
    if (currentIndex < 0) {
        StatusBadge(status = status.displayName, modifier = modifier)
        return
    }

    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        TRACKED_STEPS.forEachIndexed { index, step ->
            TrackerStep(
                label = step.displayName,
                isFirst = index == 0,
                isLast = index == TRACKED_STEPS.lastIndex,
                reached = index <= currentIndex,
                // A completed request has nothing left to do, so its last step is ticked, not "current".
                isCurrent = index == currentIndex && status != PurchaseStatus.COMPLETED,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TrackerStep(
    label: String,
    isFirst: Boolean,
    isLast: Boolean,
    reached: Boolean,
    isCurrent: Boolean,
    modifier: Modifier = Modifier,
) {
    val active = MaterialTheme.colorScheme.primary
    val idle = MaterialTheme.colorScheme.outlineVariant
    // The line to the left of a step is coloured once that step is reached.
    val lineColor = if (reached) active else idle

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Line(color = if (isFirst) Color.Transparent else lineColor, modifier = Modifier.weight(1f))
            Surface(
                shape = CircleShape,
                color = if (reached && !isCurrent) active else MaterialTheme.colorScheme.background,
                border = BorderStroke(2.dp, if (reached) active else idle),
                modifier = Modifier.size(24.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    when {
                        reached && !isCurrent -> Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp),
                        )
                        isCurrent -> Box(Modifier.size(10.dp).background(active, CircleShape))
                    }
                }
            }
            Line(color = if (isLast) Color.Transparent else idle, modifier = Modifier.weight(1f))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (reached) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Line(color: Color, modifier: Modifier = Modifier) {
    Box(modifier = modifier.height(2.dp).background(color))
}
