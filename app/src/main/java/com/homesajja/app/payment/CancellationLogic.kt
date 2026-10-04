package com.homesajja.app.payment

import com.homesajja.app.data.model.CancelContext
import com.homesajja.app.data.model.CancelReason
import com.homesajja.app.data.model.Cancellation
import com.homesajja.app.data.model.Payment
import com.homesajja.app.data.model.PaymentStatus
import com.homesajja.app.data.model.Refund
import com.homesajja.app.data.model.RefundStatus

/** The longest optional note on a cancellation. firestore.rules enforces the same cap. */
const val MAX_CANCEL_NOTE_LENGTH = 300

/** The first problem with a cancellation form, or null when it can be sent. A reason from the list is required; the note is optional. */
fun cancelError(reason: CancelReason?, note: String, context: CancelContext): String? = when {
    reason == null -> "Pick a reason"
    reason !in context.reasons -> "Pick a reason from the list"
    note.trim().length > MAX_CANCEL_NOTE_LENGTH -> "The note is too long"
    else -> null
}

fun buildCancellation(reason: CancelReason, note: String, vendorId: String, now: Long = System.currentTimeMillis()) =
    Cancellation(reason = reason, note = note.trim(), cancelledBy = vendorId, cancelledAt = now)

/**
 * Refund safeguard: true when the vendor had confirmed receiving the money, so they must mark a refund done before the cancel can complete.
 * (A payment the payer only marked, or none at all, needs no refund.)
 */
fun refundRequired(payment: Payment?, vendorId: String?): Boolean =
    vendorId != null && payment?.status == PaymentStatus.CONFIRMED && payment.payeeId == vendorId

/**
 * A vendor can cancel unless they are the one who already paid money out (a recycler paying a customer): there is nothing they could refund,
 * and cancelling would leave the customer with the money. firestore.rules enforces the same.
 */
fun vendorCanCancelWithPayment(payment: Payment?, vendorId: String?): Boolean =
    payment == null || payment.status == PaymentStatus.UNPAID || payment.payeeId == vendorId

fun buildRefund(amount: Long, now: Long = System.currentTimeMillis()) = Refund(amount = amount, status = "DONE", doneAt = now)

/** What the payer is told about their money after a vendor cancelled: refunded, nothing to refund, or "check with the vendor". */
fun refundStatusFor(payment: Payment?, refund: Refund?): RefundStatus = when {
    refund != null -> RefundStatus.DONE
    payment == null || payment.status == PaymentStatus.UNPAID -> RefundStatus.NOT_NEEDED
    else -> RefundStatus.UNCONFIRMED
}
