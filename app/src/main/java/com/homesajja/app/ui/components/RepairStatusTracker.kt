package com.homesajja.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.homesajja.app.data.model.RepairStatus

private val TRACKED_STEPS = listOf(
    RepairStatus.REQUESTED,
    RepairStatus.QUOTED,
    RepairStatus.AGREED,
    RepairStatus.IN_PROGRESS,
    RepairStatus.READY,
    RepairStatus.COMPLETED,
)

/**
 * Requested -> Quoted -> Agreed -> In progress -> Ready -> Completed for a repair. A declined quote, a rejected or a cancelled request
 * shows a badge instead; the old "Accepted" step counts as Agreed.
 */
@Composable
fun RepairStatusTracker(status: RepairStatus, modifier: Modifier = Modifier) {
    val currentIndex = TRACKED_STEPS.indexOf(if (status == RepairStatus.ACCEPTED) RepairStatus.AGREED else status)
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
