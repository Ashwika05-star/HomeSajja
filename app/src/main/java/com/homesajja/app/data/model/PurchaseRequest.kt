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
 * "Buy" is a request at the asking price; "Make offer" is one at a lower [offeredPrice]. */
data class PurchaseRequest(
    val id: String = "",
    val listingId: String = "",
    val listingTitle: String = "",
    val buyerId: String = "",
    val buyerName: String = "",
    val sellerId: String = "",
    val offeredPrice: Long = 0L,
    val message: String = "",
    val status: PurchaseStatus = PurchaseStatus.REQUESTED,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
