package com.homesajja.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.homesajja.app.data.model.ExchangeStatus

private val TRACKED_STEPS = listOf(ExchangeStatus.PENDING, ExchangeStatus.ACCEPTED, ExchangeStatus.COMPLETED)

/** Pending -> Accepted -> Completed for an exchange. Declined or cancelled requests show a badge instead. */
@Composable
fun ExchangeStatusTracker(status: ExchangeStatus, modifier: Modifier = Modifier) {
    val currentIndex = TRACKED_STEPS.indexOf(status)
    if (currentIndex < 0) {
        StatusBadge(status = status.displayName, modifier = modifier)
        return
    }
    StatusStepper(
        labels = TRACKED_STEPS.map { it.displayName },
        currentIndex = currentIndex,
        allDone = status == ExchangeStatus.COMPLETED,
        modifier = modifier,
    )
}
