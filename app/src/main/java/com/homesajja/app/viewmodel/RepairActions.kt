package com.homesajja.app.viewmodel

import com.homesajja.app.data.model.RepairRequest
import com.homesajja.app.data.model.RepairStatus
import com.homesajja.app.payment.isUntouched

/**
 * A step a person can take on a repair request. [target] is the status it moves the request to.
 * [needsQuote] actions open the quote form instead of just asking for confirmation.
 */
enum class RepairAction(val label: String, val target: RepairStatus, val needsQuote: Boolean = false) {
    SEND_QUOTE("Send quote", RepairStatus.QUOTED, needsQuote = true),
    REVISE_QUOTE("Send revised quote", RepairStatus.QUOTED, needsQuote = true),
    ACCEPT_QUOTE("Accept quote", RepairStatus.AGREED),
    DECLINE_QUOTE("Decline quote", RepairStatus.DECLINED),
    REJECT("Reject", RepairStatus.REJECTED),
    CLOSE("Close request", RepairStatus.REJECTED),
    START("Start work", RepairStatus.IN_PROGRESS),
    MARK_READY("Mark as ready", RepairStatus.READY),
    COMPLETE("Mark as completed", RepairStatus.COMPLETED),
    CANCEL("Cancel request", RepairStatus.CANCELLED),
}

/**
 * What [userId] can do to [request] right now. Mirrors the transitions firestore.rules enforces:
 * the vendor quotes (REQUESTED -> QUOTED), the user accepts (-> AGREED) or declines (-> DECLINED), and a declined quote gets a revised
 * quote or the vendor closes the request. Only after AGREED can the vendor start work: IN_PROGRESS -> READY -> COMPLETED.
 * The user can cancel until work starts, as long as nobody has marked a payment.
 * (ACCEPTED is the old "accepted without a price" step; those requests carry on as an agreed job.)
 */
fun repairActionsFor(request: RepairRequest, userId: String?): List<RepairAction> {
    val isVendor = userId != null && userId == request.vendorId
    val isUser = userId != null && userId == request.userId
    val canCancel = request.payment.isUntouched
    return when (request.status) {
        RepairStatus.REQUESTED -> when {
            isVendor -> listOf(RepairAction.SEND_QUOTE, RepairAction.REJECT)
            isUser -> listOf(RepairAction.CANCEL)
            else -> emptyList()
        }
        RepairStatus.QUOTED -> if (isUser) listOf(RepairAction.ACCEPT_QUOTE, RepairAction.DECLINE_QUOTE) else emptyList()
        RepairStatus.DECLINED -> when {
            isVendor -> listOf(RepairAction.REVISE_QUOTE, RepairAction.CLOSE)
            isUser -> listOf(RepairAction.CANCEL)
            else -> emptyList()
        }
        RepairStatus.AGREED, RepairStatus.ACCEPTED -> when {
            isVendor -> listOf(RepairAction.START)
            isUser && canCancel -> listOf(RepairAction.CANCEL)
            else -> emptyList()
        }
        RepairStatus.IN_PROGRESS -> if (isVendor) listOf(RepairAction.MARK_READY) else emptyList()
        RepairStatus.READY -> if (isVendor) listOf(RepairAction.COMPLETE) else emptyList()
        RepairStatus.COMPLETED, RepairStatus.REJECTED, RepairStatus.CANCELLED -> emptyList()
    }
}
