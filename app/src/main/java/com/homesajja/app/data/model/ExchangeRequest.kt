package com.homesajja.app.data.model

/** PENDING -> ACCEPTED -> COMPLETED, or REJECTED (owner) / CANCELLED (requester). */
enum class ExchangeStatus(val displayName: String) {
    PENDING("Pending"),
    ACCEPTED("Accepted"),
    REJECTED("Rejected"),
    CANCELLED("Cancelled"),
    COMPLETED("Completed"),
}

/** Stored at `exchangeRequests/{id}`. The requester proposes their own item
 * (the `offeredItem*` fields) for a listing marked EXCHANGE.
 * Parties: [requesterId] and [ownerId]. */
data class ExchangeRequest(
    val id: String = "",
    val targetListingId: String = "",
    val targetListingTitle: String = "",
    val requesterId: String = "",
    val requesterName: String = "",
    val ownerId: String = "",
    val offeredItemTitle: String = "",
    val offeredItemDescription: String = "",
    val offeredItemImages: List<String> = emptyList(),
    val offeredItemCondition: FurnitureCondition = FurnitureCondition.GOOD,
    val message: String = "",
    val status: ExchangeStatus = ExchangeStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
