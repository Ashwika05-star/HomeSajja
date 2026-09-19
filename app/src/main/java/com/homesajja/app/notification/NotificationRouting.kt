package com.homesajja.app.notification

import android.content.Intent
import com.homesajja.app.data.model.EntityType
import com.homesajja.app.navigation.Routes

/** Where a notification, from the Notifications screen or the system tray, should take the person. */
object NotificationRouting {

    const val EXTRA_RELATED_TYPE = "relatedType"
    const val EXTRA_RELATED_ID = "relatedId"

    /** The screen for a record of [type] with id [id], or null when nothing opens (e.g. a review, which has no screen yet). */
    fun routeFor(type: EntityType, id: String): String? = when (type) {
        EntityType.LISTING -> Routes.ListingDetail.createRoute(id)
        EntityType.EXCHANGE_REQUEST -> Routes.ExchangeDetail.createRoute(id)
        EntityType.REPAIR_REQUEST -> Routes.RepairDetail.createRoute(id)
        EntityType.RECYCLING_REQUEST -> Routes.RecycleDetail.createRoute(id)
        EntityType.MATERIAL_REQUEST -> Routes.MaterialDetail.createRoute(id)
        EntityType.CHAT -> Routes.ChatThread.createRoute(id)
        EntityType.PURCHASE_REQUEST, EntityType.REVIEW -> null
    }.takeIf { id.isNotBlank() }

    /** Reads the destination a tapped system notification carries, or null if the intent has none. */
    fun routeFrom(intent: Intent?): String? {
        val type = intent?.getStringExtra(EXTRA_RELATED_TYPE)?.let { name -> EntityType.entries.firstOrNull { it.name == name } }
        val id = intent?.getStringExtra(EXTRA_RELATED_ID)
        return if (type != null && id != null) routeFor(type, id) else null
    }
}
