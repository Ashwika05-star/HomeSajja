package com.homesajja.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
 * a badge instead. Specific to purchase requests — Exchange has its own tracker.
 */
@Composable
fun PurchaseStatusTracker(status: PurchaseStatus, modifier: Modifier = Modifier) {
    val currentIndex = TRACKED_STEPS.indexOf(status)
    if (currentIndex < 0) {
        StatusBadge(status = status.displayName, modifier = modifier)
        return
    }
    StatusStepper(
        labels = TRACKED_STEPS.map { it.displayName },
        currentIndex = currentIndex,
        allDone = status == PurchaseStatus.COMPLETED,
        modifier = modifier,
    )
}
