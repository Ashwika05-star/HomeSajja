package com.homesajja.app.data.model

/** REQUESTED -> ACCEPTED -> READY_FOR_PICKUP -> COMPLETED. The seller can also REJECT a request; the buyer can withdraw it (CANCELLED)
 * until it is accepted; after that the seller can still cancel it, with a reason (CANCELLED_BY_VENDOR). */
enum class PurchaseStatus(val displayName: String) {
    REQUESTED("Requested"),
    ACCEPTED("Accepted"),
    READY_FOR_PICKUP("Ready for pickup"),
    COMPLETED("Completed"),
    REJECTED("Rejected"),
    CANCELLED("Cancelled"),
    CANCELLED_BY_VENDOR("Cancelled by seller"),
}

/** Stored at `purchaseRequests/{id}`. Parties: [buyerId] and [sellerId].
 * "Buy" is a request at the asking price; "Make offer" is one at a lower [offeredPrice].
 * When the seller accepts, the price (the asking price, or the accepted offer) becomes the [agreedAmount] and a [payment] record opens.
 * Payment happens outside HomeSajja (UPI app or cash): the buyer says they paid and the seller confirms they received it.
 * Completing the sale stays a separate, seller-side step. */
data class PurchaseRequest(
    val id: String = "",
    val listingId: String = "",
    val listingTitle: String = "",
    val buyerId: String = "",
    val buyerName: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val offeredPrice: Long = 0L,
    val message: String = "",
    val status: PurchaseStatus = PurchaseStatus.REQUESTED,
    /** Set once, when the seller accepts: equal to [offeredPrice], and never changed afterwards. */
    val agreedAmount: Long? = null,
    val agreedAt: Long? = null,
    val payment: Payment? = null,
    /** Set when the seller cancelled after accepting: the reason, an optional note, who and when. */
    val cancellation: Cancellation? = null,
    /** Set only when the seller cancelled after confirming they had received payment: the refund they marked done. */
    val refund: Refund? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
