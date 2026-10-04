package com.homesajja.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.homesajja.app.data.model.RecyclingStatus

private val FREE_STEPS = listOf(
    RecyclingStatus.REQUESTED,
    RecyclingStatus.ACCEPTED,
    RecyclingStatus.SCHEDULED,
    RecyclingStatus.COMPLETED,
)

private val QUOTED_STEPS = listOf(
    RecyclingStatus.REQUESTED,
    RecyclingStatus.QUOTED,
    RecyclingStatus.ACCEPTED,
    RecyclingStatus.SCHEDULED,
    RecyclingStatus.COMPLETED,
)

/**
 * Requested -> Accepted -> Scheduled -> Completed for a recycling request. A request that has a quote ([hasQuote]) shows the extra
 * Quoted step. A declined quote, a rejected or a cancelled request shows a badge instead.
 */
@Composable
fun RecycleStatusTracker(status: RecyclingStatus, modifier: Modifier = Modifier, hasQuote: Boolean = false) {
    val steps = if (hasQuote || status == RecyclingStatus.QUOTED) QUOTED_STEPS else FREE_STEPS
    val currentIndex = steps.indexOf(status)
    if (currentIndex < 0) {
        StatusBadge(status = status.displayName, modifier = modifier)
        return
    }
    StatusStepper(
        labels = steps.map { it.displayName },
        currentIndex = currentIndex,
        allDone = status == RecyclingStatus.COMPLETED,
        modifier = modifier,
    )
}
