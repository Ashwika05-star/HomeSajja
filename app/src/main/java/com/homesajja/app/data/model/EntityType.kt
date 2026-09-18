package com.homesajja.app.data.model

/** Points at "which record is this about" from a Chat, Notification or Review.
 * It is only a reference label — each system keeps its own model, collection
 * and status pipeline. */
enum class EntityType {
    LISTING,
    PURCHASE_REQUEST,
    EXCHANGE_REQUEST,
    REPAIR_REQUEST,
    RECYCLING_REQUEST,
    MATERIAL_REQUEST,
    CHAT,
    REVIEW,
}
