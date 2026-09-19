package com.homesajja.app.data.model

/** REQUESTED -> ACCEPTED -> READY_FOR_PICKUP -> COMPLETED. The seller can also
 * REJECT a request, and either side can end it as CANCELLED. */
enum class PurchaseStatus(val displayName: String) {
    REQUESTED("Requested"),
    ACCEPTED("Accepted"),
    READY_FOR_PICKUP("Ready for pickup"),
    COMPLETED("Completed"),
    REJECTED("Rejected"),
    CANCELLED("Cancelled"),
}

/** Stored at `purchaseRequests/{id}`. Parties: [buyerId] and [sellerId].
 * "Buy" is a request at the asking price; "Make offer" is one at a lower [offeredPrice].
 * Payment happens outside HomeSajja (GPay/UPI or cash): the seller may attach their [upiId] when accepting, and either
 * side can mark the request [paid]. Completing the sale stays a separate, seller-side step. */
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
    val upiId: String? = null,
    val paid: Boolean = false,
    val paidAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
) {
    /** The buyer can pay with UPI while the seller has accepted, gave a UPI ID and it isn't marked paid yet. */
    val canPayWithUpi: Boolean
        get() = upiId != null && !paid && (status == PurchaseStatus.ACCEPTED || status == PurchaseStatus.READY_FOR_PICKUP)

    /** Either side can mark it paid while it is accepted (or ready for pickup). */
    val canMarkPaid: Boolean
        get() = !paid && (status == PurchaseStatus.ACCEPTED || status == PurchaseStatus.READY_FOR_PICKUP)
}
