package com.homesajja.app.viewmodel

import com.homesajja.app.data.model.RepairRequest
import com.homesajja.app.data.model.RepairStatus

/** A step a person can take on a repair request. [target] is the status it moves the request to. */
enum class RepairAction(val label: String, val target: RepairStatus) {
    ACCEPT("Accept job", RepairStatus.ACCEPTED),
    REJECT("Reject", RepairStatus.REJECTED),
    START("Start work", RepairStatus.IN_PROGRESS),
    MARK_READY("Mark as ready", RepairStatus.READY),
    COMPLETE("Mark as completed", RepairStatus.COMPLETED),
    CANCEL("Cancel request", RepairStatus.CANCELLED),
}

/**
 * What [userId] can do to [request] right now. Mirrors the transitions firestore.rules enforces:
 * the vendor moves the job REQUESTED -> ACCEPTED -> IN_PROGRESS -> READY -> COMPLETED (or rejects
 * it at the start); the user can only cancel, and only before work has started.
 */
fun repairActionsFor(request: RepairRequest, userId: String?): List<RepairAction> {
    val isVendor = userId != null && userId == request.vendorId
    val isUser = userId != null && userId == request.userId
    return when (request.status) {
        RepairStatus.REQUESTED -> when {
            isVendor -> listOf(RepairAction.ACCEPT, RepairAction.REJECT)
            isUser -> listOf(RepairAction.CANCEL)
            else -> emptyList()
        }
        RepairStatus.ACCEPTED -> when {
            isVendor -> listOf(RepairAction.START)
            isUser -> listOf(RepairAction.CANCEL)
            else -> emptyList()
        }
        RepairStatus.IN_PROGRESS -> if (isVendor) listOf(RepairAction.MARK_READY) else emptyList()
        RepairStatus.READY -> if (isVendor) listOf(RepairAction.COMPLETE) else emptyList()
        RepairStatus.COMPLETED, RepairStatus.REJECTED, RepairStatus.CANCELLED -> emptyList()
    }
}
