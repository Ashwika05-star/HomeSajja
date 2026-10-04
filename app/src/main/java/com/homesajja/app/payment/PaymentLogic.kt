package com.homesajja.app.payment

import com.homesajja.app.data.model.PayDirection
import com.homesajja.app.data.model.Payment
import com.homesajja.app.data.model.PaymentStatus
import com.homesajja.app.data.model.PurchaseRequest
import com.homesajja.app.data.model.PurchaseStatus
import com.homesajja.app.data.model.Quote
import com.homesajja.app.data.model.RecyclingRequest
import com.homesajja.app.data.model.RecyclingStatus
import com.homesajja.app.data.model.RepairRequest
import com.homesajja.app.data.model.RepairStatus

/** Largest amount a quote can carry, in rupees. firestore.rules enforces the same cap. */
const val MAX_QUOTE_AMOUNT = 1_000_000L

/** The longest estimate a repair quote can give, in days. firestore.rules enforces the same cap. */
const val MAX_ESTIMATED_DAYS = 365

/** The longest transaction reference a payer can enter, and the longest quote note. */
const val MAX_UPI_REF_LENGTH = 64
const val MAX_QUOTE_NOTE_LENGTH = 500

/** What was typed into the quote form, checked before anything is sent. */
data class QuoteForm(
    val amount: String = "",
    val days: String = "",
    val note: String = "",
    val direction: PayDirection? = null,
)

/** The first problem with a quote form, or null when it can be sent. [needsDays] is true for repairs; [needsDirection] for recycling. */
fun quoteError(form: QuoteForm, needsDays: Boolean, needsDirection: Boolean): String? {
    val amount = form.amount.trim().toLongOrNull()
    if (amount == null || amount <= 0) return "Enter the amount in rupees"
    if (amount > MAX_QUOTE_AMOUNT) return "The amount can't be more than ₹10,00,000"
    if (needsDays || form.days.isNotBlank()) {
        val days = form.days.trim().toIntOrNull()
        if (days == null || days < 1) return "Enter the estimated days"
        if (days > MAX_ESTIMATED_DAYS) return "Estimated days can't be more than $MAX_ESTIMATED_DAYS"
    }
    if (needsDirection && form.direction == null) return "Choose who pays whom"
    if (form.note.trim().length > MAX_QUOTE_NOTE_LENGTH) return "The note is too long"
    return null
}

/** Builds the quote to store from a valid [form]. [revision] is 1 for a first quote and goes up with each revised one. */
fun buildQuote(form: QuoteForm, payeeUpiId: String?, revision: Int, now: Long = System.currentTimeMillis()) = Quote(
    amount = form.amount.trim().toLong(),
    estimatedDays = form.days.trim().toIntOrNull(),
    note = form.note.trim(),
    payeeUpiId = payeeUpiId?.trim()?.takeIf { it.isNotEmpty() },
    direction = form.direction,
    revision = revision,
    sentAt = now,
)

/** The revision number for the next quote on a request whose current quote is [current]. */
fun nextRevision(current: Quote?): Int = (current?.revision ?: 0) + 1

/** A fresh, unpaid payment record, opened when an amount is agreed. */
fun newPayment(payerId: String, payeeId: String, payeeUpiId: String?) = Payment(
    payerId = payerId,
    payeeId = payeeId,
    payeeUpiId = payeeUpiId?.trim()?.takeIf { it.isNotEmpty() },
)

/** Who is to pay and who is to receive on a recycling quote. */
fun recyclingParties(request: RecyclingRequest, direction: PayDirection): Pair<String, String> {
    val vendorId = request.vendorId.orEmpty()
    return if (direction == PayDirection.USER_PAYS_VENDOR) request.userId to vendorId else vendorId to request.userId
}

/** What the person looking at a payment should do next. */
enum class PaymentTurn {
    /** The viewer is the payer and hasn't paid yet. */
    PAY,

    /** The viewer is the payee and is waiting for the payer. */
    WAIT_FOR_PAYER,

    /** The viewer is the payee and the payer says they paid: check it arrived, then confirm. */
    CONFIRM,

    /** The viewer is the payer and is waiting for the payee to confirm. */
    WAIT_FOR_PAYEE,

    /** The payee has confirmed the money arrived. */
    DONE,

    /** The viewer isn't a party to this payment. */
    NONE,
}

fun Payment.turnFor(viewerId: String?): PaymentTurn {
    val isPayer = viewerId != null && viewerId == payerId
    val isPayee = viewerId != null && viewerId == payeeId
    return when {
        !isPayer && !isPayee -> PaymentTurn.NONE
        status == PaymentStatus.CONFIRMED -> PaymentTurn.DONE
        status == PaymentStatus.MARKED_PAID -> if (isPayee) PaymentTurn.CONFIRM else PaymentTurn.WAIT_FOR_PAYEE
        else -> if (isPayer) PaymentTurn.PAY else PaymentTurn.WAIT_FOR_PAYER
    }
}

/** True while nobody has said money moved, so the job can still be cancelled. firestore.rules blocks cancelling after that. */
val Payment?.isUntouched: Boolean get() = this == null || status == PaymentStatus.UNPAID

/** The payment steps are open while the job is live: from the agreement until it ends (a finished job can still be paid for). */
val PurchaseRequest.paymentOpen: Boolean
    get() = payment != null && status in setOf(PurchaseStatus.ACCEPTED, PurchaseStatus.READY_FOR_PICKUP, PurchaseStatus.COMPLETED)

val RepairRequest.paymentOpen: Boolean
    get() = payment != null && status in setOf(RepairStatus.AGREED, RepairStatus.IN_PROGRESS, RepairStatus.READY, RepairStatus.COMPLETED)

val RecyclingRequest.paymentOpen: Boolean
    get() = payment != null && status in setOf(RecyclingStatus.ACCEPTED, RecyclingStatus.SCHEDULED, RecyclingStatus.COMPLETED)

/** One agreed amount and its payment record, whichever kind of request it came from. [refunded] is true once the vendor cancelled and refunded it. */
data class Receivable(val amount: Long?, val payment: Payment?, val refunded: Boolean = false)

/**
 * Money [personId] has actually earned: the agreed amount of every request whose payment they were due to receive and
 * whose payment is CONFIRMED. A payer's "I've paid" does not count until the payee confirms it, and money the person
 * paid out themselves (a recycler paying a customer) is never earnings. A payment that was refunded when the vendor cancelled stops counting.
 */
fun confirmedEarnings(personId: String, items: List<Receivable>): Long = items
    .filter { !it.refunded && it.payment?.status == PaymentStatus.CONFIRMED && it.payment.payeeId == personId }
    .sumOf { it.amount ?: 0L }

fun PurchaseRequest.receivable() = Receivable(agreedAmount, payment, refunded = refund != null)
fun RepairRequest.receivable() = Receivable(agreedAmount, payment, refunded = refund != null)
fun RecyclingRequest.receivable() = Receivable(agreedAmount, payment, refunded = refund != null)
