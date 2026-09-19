package com.homesajja.app.viewmodel

import com.homesajja.app.data.model.RecyclingRequest
import com.homesajja.app.data.model.RecyclingStatus

/** A step a person can take on a recycling request. [target] is the status it moves the request to. */
enum class RecycleAction(val label: String, val target: RecyclingStatus) {
    ACCEPT("Accept request", RecyclingStatus.ACCEPTED),
    REJECT("Reject", RecyclingStatus.REJECTED),
    SCHEDULE("Mark as scheduled", RecyclingStatus.SCHEDULED),
    COMPLETE("Mark as completed", RecyclingStatus.COMPLETED),
    CANCEL("Cancel request", RecyclingStatus.CANCELLED),
}

/**
 * What [userId] can do to [request] right now. Mirrors the transitions firestore.rules enforces:
 * the recycler moves it REQUESTED -> ACCEPTED -> SCHEDULED -> COMPLETED (or rejects it at the start);
 * the user can only cancel, and only before it is scheduled. An unassigned pickup has no recycler yet.
 */
fun recycleActionsFor(request: RecyclingRequest, userId: String?): List<RecycleAction> {
    val isRecycler = userId != null && userId == request.vendorId
    val isUser = userId != null && userId == request.userId
    return when (request.status) {
        RecyclingStatus.REQUESTED -> when {
            isRecycler -> listOf(RecycleAction.ACCEPT, RecycleAction.REJECT)
            isUser -> listOf(RecycleAction.CANCEL)
            else -> emptyList()
        }
        RecyclingStatus.ACCEPTED -> when {
            isRecycler -> listOf(RecycleAction.SCHEDULE)
            isUser -> listOf(RecycleAction.CANCEL)
            else -> emptyList()
        }
        RecyclingStatus.SCHEDULED -> if (isRecycler) listOf(RecycleAction.COMPLETE) else emptyList()
        RecyclingStatus.COMPLETED, RecyclingStatus.REJECTED, RecyclingStatus.CANCELLED -> emptyList()
    }
}
