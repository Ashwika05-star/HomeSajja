package com.homesajja.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.homesajja.app.ui.util.formatCompactPrice
import com.homesajja.app.ui.util.formatPrice
import com.homesajja.app.viewmodel.MonthTotal

private val CHART_BAR_AREA = 110.dp

/**
 * A simple bar chart of the last months' earnings, oldest to newest: one bar per month, tall in proportion to the biggest month, with the
 * amount above and the month below. The newest month is drawn in full colour. No chart library: each bar is a plain box.
 */
@Composable
fun EarningsChart(months: List<MonthTotal>, modifier: Modifier = Modifier) {
    val highest = months.maxOfOrNull { it.amount }?.coerceAtLeast(1L) ?: 1L
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        months.forEachIndexed { index, month ->
            val fraction = month.amount.toFloat() / highest
            val isLatest = index == months.lastIndex
            Column(
                modifier = Modifier
                    .weight(1f)
                    .semantics(mergeDescendants = true) { contentDescription = "${month.label}: ${formatPrice(month.amount)} earned" },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    if (month.amount > 0) formatCompactPrice(month.amount) else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                Box(modifier = Modifier.height(CHART_BAR_AREA).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    // A month with nothing earned still shows a thin line, so the chart always reads as six months.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height((CHART_BAR_AREA * fraction).coerceAtLeast(3.dp))
                            .alpha(if (isLatest) 1f else 0.75f)
                            .background(
                                if (month.amount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
                            ),
                    )
                }
                Text(
                    month.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isLatest) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}
