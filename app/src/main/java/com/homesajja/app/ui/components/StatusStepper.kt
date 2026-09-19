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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * A row of steps joined by a line: steps up to [currentIndex] are reached, earlier ones are
 * ticked, and the current one is ringed — unless [allDone], when the last step is ticked too.
 * Each system (purchase, exchange, ...) wraps this with its own labels and status mapping.
 */
@Composable
fun StatusStepper(
    labels: List<String>,
    currentIndex: Int,
    allDone: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        labels.forEachIndexed { index, label ->
            StepperStep(
                label = label,
                isFirst = index == 0,
                isLast = index == labels.lastIndex,
                reached = index <= currentIndex,
                isCurrent = index == currentIndex && !allDone,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StepperStep(
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

    // Read out as "Accepted, done" / "In progress, current step" / "Ready, not reached yet" instead of a bare label.
    val stateText = when {
        isCurrent -> "current step"
        reached -> "done"
        else -> "not reached yet"
    }
    Column(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = "$label, $stateText" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
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
