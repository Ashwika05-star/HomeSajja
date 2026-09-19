package com.homesajja.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.homesajja.app.data.model.RepairStatus

private val TRACKED_STEPS = listOf(
    RepairStatus.REQUESTED,
    RepairStatus.ACCEPTED,
    RepairStatus.IN_PROGRESS,
    RepairStatus.READY,
    RepairStatus.COMPLETED,
)

/** Requested -> Accepted -> In progress -> Ready -> Completed for a repair. Rejected or cancelled requests show a badge instead. */
@Composable
fun RepairStatusTracker(status: RepairStatus, modifier: Modifier = Modifier) {
    val currentIndex = TRACKED_STEPS.indexOf(status)
    if (currentIndex < 0) {
        StatusBadge(status = status.displayName, modifier = modifier)
        return
    }
    StatusStepper(
        labels = TRACKED_STEPS.map { it.displayName },
        currentIndex = currentIndex,
        allDone = status == RepairStatus.COMPLETED,
        modifier = modifier,
    )
}
