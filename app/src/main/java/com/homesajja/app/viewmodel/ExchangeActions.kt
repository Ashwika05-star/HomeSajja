package com.homesajja.app.viewmodel

import com.homesajja.app.data.model.ExchangeRequest
import com.homesajja.app.data.model.ExchangeStatus

/** A step a person can take on an exchange request. */
enum class ExchangeAction(val label: String) {
    ACCEPT("Accept"),
    DECLINE("Decline"),
    CANCEL("Cancel request"),
    COMPLETE("Mark as completed"),
    /** A vendor party backs out of an accepted exchange: opens the reason form first. */
    CANCEL_BY_VENDOR("Cancel exchange"),
}

/**
 * What [userId] can do to [request] right now. Mirrors the transitions firestore.rules enforces:
 * the receiver answers a pending request, the sender can withdraw it while it is pending, and
 * either party can complete it once accepted, and whichever party is a vendor ([viewerIsVendor]) can instead cancel it, with a reason.
 */
fun exchangeActionsFor(request: ExchangeRequest, userId: String?, viewerIsVendor: Boolean = false): List<ExchangeAction> {
    val isSender = userId != null && userId == request.senderId
    val isReceiver = userId != null && userId == request.receiverId
    return when (request.status) {
        ExchangeStatus.PENDING -> when {
            isReceiver -> listOf(ExchangeAction.ACCEPT, ExchangeAction.DECLINE)
            isSender -> listOf(ExchangeAction.CANCEL)
            else -> emptyList()
        }
        ExchangeStatus.ACCEPTED ->
            if (isSender || isReceiver) listOfNotNull(ExchangeAction.COMPLETE, ExchangeAction.CANCEL_BY_VENDOR.takeIf { viewerIsVendor }) else emptyList()
        ExchangeStatus.DECLINED, ExchangeStatus.CANCELLED, ExchangeStatus.COMPLETED, ExchangeStatus.CANCELLED_BY_VENDOR -> emptyList()
    }
}
