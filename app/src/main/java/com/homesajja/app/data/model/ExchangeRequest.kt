package com.homesajja.app.data.model

/** PENDING -> ACCEPTED (by receiver) -> COMPLETED (by either party).
 * A PENDING request can instead be DECLINED (receiver) or CANCELLED (sender). */
enum class ExchangeStatus(val displayName: String) {
    PENDING("Pending"),
    ACCEPTED("Accepted"),
    DECLINED("Declined"),
    CANCELLED("Cancelled"),
    COMPLETED("Completed"),
}

/**
 * Stored at `exchangeRequests/{id}`. The sender offers one of their own listings
 * ([offeredListingId]) for the receiver's exchange listing ([requestedListingId]).
 * Parties: [senderId] and [receiverId].
 *
 * The titles and image URLs are snapshots taken when the request is made, so the
 * request cards keep working even if a listing is later edited or removed.
 */
data class ExchangeRequest(
    val id: String = "",
    val offeredListingId: String = "",
    val offeredTitle: String = "",
    val offeredImageUrl: String? = null,
    val requestedListingId: String = "",
    val requestedTitle: String = "",
    val requestedImageUrl: String? = null,
    val senderId: String = "",
    val senderName: String = "",
    val receiverId: String = "",
    val receiverName: String = "",
    val message: String = "",
    val status: ExchangeStatus = ExchangeStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
