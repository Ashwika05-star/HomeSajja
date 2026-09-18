package com.homesajja.app.viewmodel

import com.homesajja.app.data.model.ExchangeRequest
import com.homesajja.app.data.model.ExchangeStatus

/** A step a person can take on an exchange request. */
enum class ExchangeAction(val label: String) {
    ACCEPT("Accept"),
    DECLINE("Decline"),
    CANCEL("Cancel request"),
    COMPLETE("Mark as completed"),
}

/**
 * What [userId] can do to [request] right now. Mirrors the transitions firestore.rules enforces:
 * the receiver answers a pending request, the sender can withdraw it while it is pending, and
 * either party can complete it once accepted.
 */
fun exchangeActionsFor(request: ExchangeRequest, userId: String?): List<ExchangeAction> {
    val isSender = userId != null && userId == request.senderId
    val isReceiver = userId != null && userId == request.receiverId
    return when (request.status) {
        ExchangeStatus.PENDING -> when {
            isReceiver -> listOf(ExchangeAction.ACCEPT, ExchangeAction.DECLINE)
            isSender -> listOf(ExchangeAction.CANCEL)
            else -> emptyList()
        }
        ExchangeStatus.ACCEPTED ->
            if (isSender || isReceiver) listOf(ExchangeAction.COMPLETE) else emptyList()
        ExchangeStatus.DECLINED, ExchangeStatus.CANCELLED, ExchangeStatus.COMPLETED -> emptyList()
    }
}
