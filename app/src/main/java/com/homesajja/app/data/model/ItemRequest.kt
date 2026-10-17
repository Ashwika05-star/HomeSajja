package com.homesajja.app.data.model

/**
 * Status of a custom item request from a user.
 * OPEN -> ACCEPTED by a vendor -> FULFILLED (or CANCELLED by user).
 */
enum class ItemRequestStatus(val displayName: String) {
    OPEN("Open"),
    ACCEPTED("Accepted"),
    FULFILLED("Fulfilled"),
    CANCELLED("Cancelled");

    fun localizedLabel(isHindi: Boolean): String = if (isHindi) when (this) {
        OPEN -> "खुला"
        ACCEPTED -> "स्वीकृत"
        FULFILLED -> "पूर्ण हुआ"
        CANCELLED -> "रद्द"
    } else displayName
}

/**
 * Stored at `itemRequests/{id}`.
 * A user posts a request for a specific furniture item they are looking for.
 * Local vendors can browse open requests and accept them to fulfill the customer's need.
 */
data class ItemRequest(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhone: String = "",
    val title: String = "",
    val category: FurnitureCategory = FurnitureCategory.OTHER,
    val description: String = "",
    val targetBudget: Long = 0L,
    val city: String = "",
    val status: ItemRequestStatus = ItemRequestStatus.OPEN,
    val acceptedVendorId: String? = null,
    val acceptedVendorName: String? = null,
    val vendorNote: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
