package com.homesajja.app.data.model

/** What happened. Each type has its own label and icon in the Notifications screen. */
enum class NotificationType(val label: String) {
    NEW_MESSAGE("New message"),
    NEW_OFFER("New offer"),
    EXCHANGE_REQUEST("Exchange request"),
    EXCHANGE_ACCEPTED("Exchange accepted"),
    EXCHANGE_DECLINED("Exchange declined"),
    REPAIR_UPDATE("Repair update"),
    RECYCLING_UPDATE("Recycling update"),
    PURCHASE_UPDATE("Purchase request update"),
    LISTING_PUBLISHED("Listing published"),
    MATERIAL_MATCH("Material request match"),
    VENDOR_RESPONSE("Vendor response"),
    SALE_COMPLETED("Sale completed"),
}

/** Stored at `notifications/{id}`; visible only to [recipientId]. [seen]
 * (not `isRead`) avoids Firestore's Kotlin/Java boolean-getter naming quirk.
 * [relatedType] and [relatedId] say which screen tapping it should open. */
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

/** Stored at `deviceTokens/{token}`: which signed-in person a phone's FCM token belongs to, so a server can push to them. */
data class DeviceToken(
    val token: String = "",
    val userId: String = "",
    val platform: String = "android",
    val updatedAt: Long = System.currentTimeMillis(),
)
