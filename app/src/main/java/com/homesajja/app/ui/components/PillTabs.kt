package com.homesajja.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.homesajja.app.ui.theme.LocalDarkTheme

/**
 * The page-level tabs (Incoming / Outgoing, Active / Sold & closed, Repair / Recycle / Materials ...): a pill track with the
 * chosen tab raised on it. Every tab has the same width and keeps a 48dp touch target.
 */
@Composable
fun PillTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dark = LocalDarkTheme.current
    val raised = if (dark) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLowest
    val textStyle = when {
        labels.size >= 4 -> MaterialTheme.typography.labelSmall
        labels.size == 3 -> MaterialTheme.typography.labelMedium
        else -> MaterialTheme.typography.labelLarge
    }
    val innerPad = if (labels.size >= 3) 4.dp else 8.dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
            .padding(3.dp),
    ) {
        labels.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .minimumInteractiveComponentSize()
                    .height(36.dp)
                    .then(
                        if (selected) {
                            Modifier.shadow(1.dp, RoundedCornerShape(50)).background(raised, RoundedCornerShape(50))
                        } else {
                            Modifier
                        },
                    )
                    .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(index) })
                    .padding(horizontal = innerPad),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = textStyle,
                    fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Medium,
                    color = when {
                        !selected -> MaterialTheme.colorScheme.onSurfaceVariant
                        dark -> MaterialTheme.colorScheme.onSurface
                        else -> MaterialTheme.colorScheme.primary
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
