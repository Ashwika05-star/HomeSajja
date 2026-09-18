package com.homesajja.app.data.model

/** PENDING -> ACCEPTED -> COMPLETED, or REJECTED (seller) / CANCELLED (buyer). */
enum class PurchaseStatus(val displayName: String) {
    PENDING("Pending"),
    ACCEPTED("Accepted"),
    REJECTED("Rejected"),
    CANCELLED("Cancelled"),
    COMPLETED("Completed"),
}

/** Stored at `purchaseRequests/{id}`. Parties: [buyerId] and [sellerId]. */
data class PurchaseRequest(
    val id: String = "",
    val listingId: String = "",
    val listingTitle: String = "",
    val buyerId: String = "",
    val buyerName: String = "",
    val sellerId: String = "",
    val offeredPrice: Long = 0L,
    val message: String = "",
    val status: PurchaseStatus = PurchaseStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
