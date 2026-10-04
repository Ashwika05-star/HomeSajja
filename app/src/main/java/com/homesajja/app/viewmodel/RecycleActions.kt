package com.homesajja.app.viewmodel

import com.homesajja.app.data.model.RecyclingRequest
import com.homesajja.app.data.model.RecyclingStatus
import com.homesajja.app.payment.isUntouched
import com.homesajja.app.payment.vendorCanCancelWithPayment

/**
 * A step a person can take on a recycling request. [target] is the status it moves the request to.
 * [needsQuote] actions open the quote form instead of just asking for confirmation.
 */
enum class RecycleAction(
    val label: String,
    val target: RecyclingStatus,
    val needsQuote: Boolean = false,
    /** True for the recycler's cancel: it opens the reason form (and the refund step) instead of a plain confirmation. */
    val needsCancellation: Boolean = false,
) {
    ACCEPT("Accept (free)", RecyclingStatus.ACCEPTED),
    SEND_QUOTE("Send quote", RecyclingStatus.QUOTED, needsQuote = true),
    REVISE_QUOTE("Send revised quote", RecyclingStatus.QUOTED, needsQuote = true),
    ACCEPT_QUOTE("Accept quote", RecyclingStatus.ACCEPTED),
    DECLINE_QUOTE("Decline quote", RecyclingStatus.DECLINED),
    REJECT("Reject", RecyclingStatus.REJECTED),
    CLOSE("Close request", RecyclingStatus.REJECTED),
    SCHEDULE("Mark as scheduled", RecyclingStatus.SCHEDULED),
    COMPLETE("Mark as completed", RecyclingStatus.COMPLETED),
    CANCEL("Cancel request", RecyclingStatus.CANCELLED),
    CANCEL_BY_VENDOR("Cancel job", RecyclingStatus.CANCELLED_BY_VENDOR, needsCancellation = true),
}

/**
 * What [userId] can do to [request] right now. Mirrors the transitions firestore.rules enforces:
 * the recycler accepts for free (REQUESTED -> ACCEPTED) or sends a quote with an amount (QUOTED), which the customer accepts
 * (-> ACCEPTED, amount agreed) or declines (-> DECLINED, then a revised quote or the recycler closes it). Then ACCEPTED -> SCHEDULED -> COMPLETED
 * by the recycler. The user can cancel until it is scheduled, as long as nobody has marked a payment.
 * After accepting, the recycler can still cancel (with a reason, and a refund first if they had confirmed payment) until it is completed,
 * unless they already paid money out to the customer. An unassigned pickup has no recycler yet.
 */
fun recycleActionsFor(request: RecyclingRequest, userId: String?): List<RecycleAction> {
    val isRecycler = userId != null && userId == request.vendorId
    val isUser = userId != null && userId == request.userId
    return when (request.status) {
        RecyclingStatus.REQUESTED -> when {
            isRecycler -> listOf(RecycleAction.ACCEPT, RecycleAction.SEND_QUOTE, RecycleAction.REJECT)
            isUser -> listOf(RecycleAction.CANCEL)
            else -> emptyList()
        }
        RecyclingStatus.QUOTED -> if (isUser) listOf(RecycleAction.ACCEPT_QUOTE, RecycleAction.DECLINE_QUOTE) else emptyList()
        RecyclingStatus.DECLINED -> when {
            isRecycler -> listOf(RecycleAction.REVISE_QUOTE, RecycleAction.CLOSE)
            isUser -> listOf(RecycleAction.CANCEL)
            else -> emptyList()
        }
        RecyclingStatus.ACCEPTED -> when {
            isRecycler -> listOfNotNull(RecycleAction.SCHEDULE, RecycleAction.CANCEL_BY_VENDOR.takeIf { vendorCanCancelWithPayment(request.payment, userId) })
            isUser && request.payment.isUntouched -> listOf(RecycleAction.CANCEL)
            else -> emptyList()
        }
        RecyclingStatus.SCHEDULED ->
            if (isRecycler) listOfNotNull(RecycleAction.COMPLETE, RecycleAction.CANCEL_BY_VENDOR.takeIf { vendorCanCancelWithPayment(request.payment, userId) }) else emptyList()
        RecyclingStatus.COMPLETED, RecyclingStatus.REJECTED, RecyclingStatus.CANCELLED, RecyclingStatus.CANCELLED_BY_VENDOR -> emptyList()
    }
}
