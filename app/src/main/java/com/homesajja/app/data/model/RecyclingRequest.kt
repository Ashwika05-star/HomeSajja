package com.homesajja.app.data.model

/** REQUESTED -> ACCEPTED -> PICKUP_SCHEDULED -> COLLECTED -> COMPLETED. */
enum class RecyclingStatus(val displayName: String) {
    REQUESTED("Requested"),
    ACCEPTED("Accepted"),
    PICKUP_SCHEDULED("Pickup scheduled"),
    COLLECTED("Collected"),
    COMPLETED("Completed"),
    REJECTED("Rejected"),
    CANCELLED("Cancelled"),
}

/** Stored at `recyclingRequests/{id}`, addressed to one chosen recycler.
 * Parties: [userId] and [vendorId]. [pickupDate] is epoch millis, set by the vendor. */
data class RecyclingRequest(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val vendorId: String = "",
    val vendorName: String = "",
    val furnitureCategory: FurnitureCategory = FurnitureCategory.OTHER,
    val condition: FurnitureCondition = FurnitureCondition.FAIR,
    val description: String = "",
    val images: List<String> = emptyList(),
    val city: String = "",
    val pickupDate: Long? = null,
    val status: RecyclingStatus = RecyclingStatus.REQUESTED,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
