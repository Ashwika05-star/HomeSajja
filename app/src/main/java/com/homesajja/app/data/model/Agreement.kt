package com.homesajja.app.data.model

/** How a payment was made. Chosen by the payer when they say they've paid. */
enum class PaymentMethod(val displayName: String) {
    UPI("UPI"),
    CASH("Cash"),
}

/** UNPAID -> MARKED_PAID (the payer says they paid) -> CONFIRMED (the payee says the money arrived). Only CONFIRMED counts as money earned. */
enum class PaymentStatus(val displayName: String) {
    UNPAID("Unpaid"),
    MARKED_PAID("Marked paid by payer"),
    CONFIRMED("Confirmed received"),
}

/**
 * The payment record on a request that has an agreed amount, stored inside the request document (`payment`).
 * [payerId] and [payeeId] are the two parties; [payeeUpiId] is the UPI id the payee wants to be paid at (null = no UPI id, cash only).
 * HomeSajja never moves money: the payer pays in a UPI app or in cash, and the two sides record it here.
 */
data class Payment(
    val payerId: String = "",
    val payeeId: String = "",
    val payeeUpiId: String? = null,
    val method: PaymentMethod? = null,
    val status: PaymentStatus = PaymentStatus.UNPAID,
    /** The UPI transaction reference the payer typed in, if any. Optional and unchecked. */
    val upiRef: String? = null,
    val markedPaidAt: Long? = null,
    val confirmedAt: Long? = null,
)

/** Who pays whom on a recycling job that has an amount. Repairs and purchases always have the customer pay. */
enum class PayDirection(val displayName: String) {
    USER_PAYS_VENDOR("Customer pays the recycler"),
    VENDOR_PAYS_USER("Recycler pays the customer"),
}

/**
 * A vendor's price for a job, stored inside the request (`quote`). Sending a revised quote replaces it and raises [revision].
 * [estimatedDays] is used by repairs; [direction] by recycling. [payeeUpiId] is the quoting vendor's UPI id when they are the one paid.
 */
data class Quote(
    val amount: Long = 0L,
    val estimatedDays: Int? = null,
    val note: String = "",
    val payeeUpiId: String? = null,
    val direction: PayDirection? = null,
    val revision: Int = 1,
    val sentAt: Long = System.currentTimeMillis(),
)
