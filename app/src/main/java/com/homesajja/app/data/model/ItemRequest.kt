package com.homesajja.app.data.model

/**
 * Status of a custom item request from a user.
 * OPEN -> QUOTED by a vendor -> AGREED (accepted by customer) -> IN_PROGRESS -> READY -> COMPLETED.
 * (ACCEPTED and FULFILLED are retained for backwards-compatibility).
 */
enum class ItemRequestStatus(val displayName: String) {
    OPEN("Open"),
    QUOTED("Quoted"),
    AGREED("Agreed"),
    ACCEPTED("Accepted"),
    IN_PROGRESS("In progress"),
    READY("Ready"),
    COMPLETED("Completed"),
    FULFILLED("Fulfilled"),
    CANCELLED("Cancelled");

    fun localizedLabel(isHindi: Boolean): String = if (isHindi) when (this) {
        OPEN -> "खुला"
        QUOTED -> "कोटेशन प्राप्त"
        AGREED -> "स्वीकृत"
        ACCEPTED -> "स्वीकृत"
        IN_PROGRESS -> "प्रगति पर"
        READY -> "तैयार"
        COMPLETED -> "पूर्ण हुआ"
        FULFILLED -> "पूर्ण हुआ"
        CANCELLED -> "रद्द"
    } else displayName
}

/**
 * Stored at `itemRequests/{id}`.
 * A user posts a request for a specific furniture item they are looking for.
 * Local vendors can browse open requests in their city, send quotes, agree on price,
 * update production status, and receive payments.
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
    val quote: Quote? = null,
    val agreedAmount: Long? = null,
    val agreedAt: Long? = null,
    val payment: Payment? = null,
    val acceptedVendorId: String? = null,
    val acceptedVendorName: String? = null,
    val vendorPhone: String? = null,
    val vendorNote: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
) {
    val isAgreed: Boolean
        get() = status == ItemRequestStatus.AGREED || status == ItemRequestStatus.ACCEPTED ||
            status == ItemRequestStatus.IN_PROGRESS || status == ItemRequestStatus.READY ||
            status == ItemRequestStatus.COMPLETED || status == ItemRequestStatus.FULFILLED

    val paymentOpen: Boolean
        get() = isAgreed && status != ItemRequestStatus.CANCELLED
}
