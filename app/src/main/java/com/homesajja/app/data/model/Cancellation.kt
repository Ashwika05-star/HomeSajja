package com.homesajja.app.data.model

/** Why a vendor cancelled a request they had already accepted. The enum name is stored; [displayName] is what people read. */
enum class CancelReason(val displayName: String) {
    ITEM_UNAVAILABLE("The item is no longer available"),
    PRICE_OR_DETAILS_CHANGED("The price or details changed"),
    CANNOT_COMPLETE_JOB("I can't complete this job"),
    PARTS_UNAVAILABLE("Parts or materials aren't available"),
    SCHEDULE_CONFLICT("I have a scheduling conflict"),
    CUSTOMER_UNREACHABLE("I couldn't reach the other person"),
    OTHER("Another reason"),
}

/** The four request systems a vendor can cancel in; each offers its own short list of reasons. */
enum class CancelContext(val reasons: List<CancelReason>) {
    PURCHASE(listOf(CancelReason.ITEM_UNAVAILABLE, CancelReason.PRICE_OR_DETAILS_CHANGED, CancelReason.CUSTOMER_UNREACHABLE, CancelReason.OTHER)),
    EXCHANGE(listOf(CancelReason.ITEM_UNAVAILABLE, CancelReason.PRICE_OR_DETAILS_CHANGED, CancelReason.CUSTOMER_UNREACHABLE, CancelReason.OTHER)),
    REPAIR(listOf(CancelReason.CANNOT_COMPLETE_JOB, CancelReason.PARTS_UNAVAILABLE, CancelReason.SCHEDULE_CONFLICT, CancelReason.CUSTOMER_UNREACHABLE, CancelReason.OTHER)),
    RECYCLING(listOf(CancelReason.CANNOT_COMPLETE_JOB, CancelReason.SCHEDULE_CONFLICT, CancelReason.CUSTOMER_UNREACHABLE, CancelReason.OTHER)),
}

/**
 * Stored inside a request cancelled by a vendor (`cancellation`): the required [reason], an optional [note], and who and when.
 * The request's status is CANCELLED_BY_VENDOR.
 */
data class Cancellation(
    val reason: CancelReason = CancelReason.OTHER,
    val note: String = "",
    val cancelledBy: String = "",
    val cancelledAt: Long = System.currentTimeMillis(),
)

/** The refund status shown to the person who may have paid. Worked out by [refundStatusFor]; only DONE is stored (as a [Refund]). */
enum class RefundStatus(val displayName: String) {
    /** Nothing was paid, so there is nothing to refund. */
    NOT_NEEDED("No payment to refund"),

    /** The vendor confirmed they received the money and marked it refunded before cancelling. */
    DONE("Refunded"),

    /** The payer said they paid but the vendor never confirmed it, then cancelled: the payer should check with the vendor. */
    UNCONFIRMED("Payment was not confirmed"),
}

/**
 * Stored inside a request cancelled after the vendor had confirmed receiving payment (`refund`). The vendor must mark it done
 * as part of cancelling: the cancel can't complete without it. [amount] is the agreed amount that was paid.
 */
data class Refund(
    val amount: Long = 0L,
    val status: String = "DONE",
    val doneAt: Long = System.currentTimeMillis(),
)
