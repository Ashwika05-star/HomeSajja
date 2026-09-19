package com.homesajja.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.homesajja.app.data.model.RecyclingStatus

private val TRACKED_STEPS = listOf(
    RecyclingStatus.REQUESTED,
    RecyclingStatus.ACCEPTED,
    RecyclingStatus.SCHEDULED,
    RecyclingStatus.COMPLETED,
)

/** Requested -> Accepted -> Scheduled -> Completed for a recycling request. Rejected or cancelled requests show a badge instead. */
@Composable
fun RecycleStatusTracker(status: RecyclingStatus, modifier: Modifier = Modifier) {
    val currentIndex = TRACKED_STEPS.indexOf(status)
    if (currentIndex < 0) {
        StatusBadge(status = status.displayName, modifier = modifier)
        return
    }
    StatusStepper(
        labels = TRACKED_STEPS.map { it.displayName },
        currentIndex = currentIndex,
        allDone = status == RecyclingStatus.COMPLETED,
        modifier = modifier,
    )
}
