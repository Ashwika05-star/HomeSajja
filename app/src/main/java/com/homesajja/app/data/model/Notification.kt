package com.homesajja.app.data.model

enum class NotificationType {
    NEW_MESSAGE,
    REQUEST_RECEIVED,
    REQUEST_STATUS_CHANGED,
    REVIEW_RECEIVED,
}

/** Stored at `notifications/{id}`; visible only to [recipientId]. [seen]
 * (not `isRead`) avoids Firestore's Kotlin/Java boolean-getter naming quirk. */
data class Notification(
    val id: String = "",
    val recipientId: String = "",
    val senderId: String = "",
    val type: NotificationType = NotificationType.NEW_MESSAGE,
    val title: String = "",
    val body: String = "",
    val relatedType: EntityType = EntityType.CHAT,
    val relatedId: String = "",
    val seen: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)
