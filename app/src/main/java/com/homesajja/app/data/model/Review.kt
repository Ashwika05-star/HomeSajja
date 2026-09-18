package com.homesajja.app.data.model

/** Stored at `reviews/{reviewerId_contextType_contextId}` — the deterministic
 * id means one review per person per completed transaction. [rating] is 1..5. */
data class Review(
    val id: String = "",
    val reviewerId: String = "",
    val reviewerName: String = "",
    val targetUserId: String = "",
    val contextType: EntityType = EntityType.REPAIR_REQUEST,
    val contextId: String = "",
    val rating: Int = 5,
    val comment: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
