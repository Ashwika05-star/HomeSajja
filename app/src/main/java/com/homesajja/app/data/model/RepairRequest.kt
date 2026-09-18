package com.homesajja.app.data.model

/** REQUESTED -> QUOTED -> ACCEPTED -> IN_PROGRESS -> COMPLETED.
 * The vendor moves it forward; the user can ACCEPT the quote or CANCEL. */
enum class RepairStatus(val displayName: String) {
    REQUESTED("Requested"),
    QUOTED("Quoted"),
    ACCEPTED("Accepted"),
    IN_PROGRESS("In progress"),
    COMPLETED("Completed"),
    REJECTED("Rejected"),
    CANCELLED("Cancelled"),
}

/** Stored at `repairRequests/{id}`, addressed to one chosen vendor.
 * Parties: [userId] and [vendorId]. [quotedPrice] is set by the vendor. */
data class RepairRequest(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val vendorId: String = "",
    val vendorName: String = "",
    val furnitureCategory: FurnitureCategory = FurnitureCategory.OTHER,
    val issueDescription: String = "",
    val images: List<String> = emptyList(),
    val city: String = "",
    val quotedPrice: Long? = null,
    val status: RepairStatus = RepairStatus.REQUESTED,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
