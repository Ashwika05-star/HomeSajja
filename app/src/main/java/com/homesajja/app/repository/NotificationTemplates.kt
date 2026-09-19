package com.homesajja.app.repository

import com.homesajja.app.data.model.Chat
import com.homesajja.app.data.model.EntityType
import com.homesajja.app.data.model.ExchangeRequest
import com.homesajja.app.data.model.FurnitureListing
import com.homesajja.app.data.model.MaterialOffer
import com.homesajja.app.data.model.MaterialRequest
import com.homesajja.app.data.model.Notification
import com.homesajja.app.data.model.NotificationType
import com.homesajja.app.data.model.PurchaseRequest
import com.homesajja.app.data.model.PurchaseStatus
import com.homesajja.app.data.model.RecyclingRequest
import com.homesajja.app.data.model.RecyclingStatus
import com.homesajja.app.data.model.RepairRequest
import com.homesajja.app.data.model.RepairStatus
import com.homesajja.app.ui.util.formatPrice

private const val PREVIEW_LENGTH = 80

/**
 * What each event tells the other person. Pure functions (no Firebase), so the wording and the choice of
 * recipient can be unit-tested. A null result means there is nobody to tell yet (e.g. an unclaimed pickup).
 * Every notification records who caused it ([Notification.senderId]) and which screen it should open.
 */
object NotificationTemplates {

    fun newMessage(chat: Chat, senderId: String, text: String): Notification? {
        val recipient = chat.otherParticipantId(senderId) ?: return null
        val senderName = chat.participantNames[senderId].orEmpty().ifBlank { "Someone" }
        return Notification(
            recipientId = recipient,
            senderId = senderId,
            type = NotificationType.NEW_MESSAGE,
            title = "New message from $senderName",
            body = text.ifBlank { "Sent a photo" }.take(PREVIEW_LENGTH),
            relatedType = EntityType.CHAT,
            relatedId = chat.id,
        )
    }

    // ---- marketplace ----

    fun purchaseRequested(request: PurchaseRequest) = Notification(
        recipientId = request.sellerId,
        senderId = request.buyerId,
        type = NotificationType.NEW_OFFER,
        title = "New request on ${request.listingTitle}",
        body = "${request.buyerName} offered ${formatPrice(request.offeredPrice)}",
        relatedType = EntityType.LISTING,
        relatedId = request.listingId,
    )

    /** The other party is told when [actorId] moves the request to [status]. Completing it is a "sale completed". */
    fun purchaseStatus(request: PurchaseRequest, status: PurchaseStatus, actorId: String): Notification {
        val toBuyer = actorId == request.sellerId
        val completed = status == PurchaseStatus.COMPLETED
        return Notification(
            recipientId = if (toBuyer) request.buyerId else request.sellerId,
            senderId = actorId,
            type = if (completed) NotificationType.SALE_COMPLETED else NotificationType.PURCHASE_UPDATE,
            title = if (completed) "Sale completed: ${request.listingTitle}" else "Request ${status.displayName.lowercase()}",
            body = if (toBuyer) "Your request for ${request.listingTitle} is now ${status.displayName.lowercase()}."
            else "${request.buyerName} ${status.displayName.lowercase()} their request for ${request.listingTitle}.",
            relatedType = EntityType.LISTING,
            relatedId = request.listingId,
        )
    }

    fun listingPublished(listing: FurnitureListing) = Notification(
        recipientId = listing.ownerId,
        senderId = listing.ownerId,
        type = NotificationType.LISTING_PUBLISHED,
        title = "Listing published",
        body = "${listing.title} is now live for people in ${listing.city}.",
        relatedType = EntityType.LISTING,
        relatedId = listing.id,
    )

    // ---- exchange ----

    fun exchangeRequested(request: ExchangeRequest) = Notification(
        recipientId = request.receiverId,
        senderId = request.senderId,
        type = NotificationType.EXCHANGE_REQUEST,
        title = "Exchange request from ${request.senderName}",
        body = "${request.offeredTitle} for your ${request.requestedTitle}",
        relatedType = EntityType.EXCHANGE_REQUEST,
        relatedId = request.id,
    )

    fun exchangeDecision(request: ExchangeRequest, accepted: Boolean) = Notification(
        recipientId = request.senderId,
        senderId = request.receiverId,
        type = if (accepted) NotificationType.EXCHANGE_ACCEPTED else NotificationType.EXCHANGE_DECLINED,
        title = if (accepted) "Exchange accepted" else "Exchange declined",
        body = "${request.receiverName} ${if (accepted) "accepted" else "declined"} your swap of ${request.offeredTitle} for ${request.requestedTitle}.",
        relatedType = EntityType.EXCHANGE_REQUEST,
        relatedId = request.id,
    )

    // ---- repair ----

    fun repairRequested(request: RepairRequest) = Notification(
        recipientId = request.vendorId,
        senderId = request.userId,
        type = NotificationType.REPAIR_UPDATE,
        title = "New repair request",
        body = "${request.userName}: ${request.furnitureTitle} (${request.problemType.displayName})",
        relatedType = EntityType.REPAIR_REQUEST,
        relatedId = request.id,
    )

    fun repairStatus(request: RepairRequest, status: RepairStatus, actorId: String): Notification {
        val toUser = actorId == request.vendorId
        return Notification(
            recipientId = if (toUser) request.userId else request.vendorId,
            senderId = actorId,
            type = NotificationType.REPAIR_UPDATE,
            title = "Repair ${status.displayName.lowercase()}",
            body = if (toUser) "${request.vendorName}: your ${request.furnitureTitle} repair is now ${status.displayName.lowercase()}."
            else "${request.userName} ${status.displayName.lowercase()} the repair of ${request.furnitureTitle}.",
            relatedType = EntityType.REPAIR_REQUEST,
            relatedId = request.id,
        )
    }

    // ---- recycling ----

    /** Only a drop-off is addressed to someone; an unclaimed pickup has nobody to tell yet. */
    fun recyclingRequested(request: RecyclingRequest): Notification? {
        val vendorId = request.vendorId ?: return null
        return Notification(
            recipientId = vendorId,
            senderId = request.userId,
            type = NotificationType.RECYCLING_UPDATE,
            title = "New recycling request",
            body = "${request.userName}: ${request.material.displayName} furniture (${request.method.displayName.lowercase()})",
            relatedType = EntityType.RECYCLING_REQUEST,
            relatedId = request.id,
        )
    }

    fun recyclingStatus(request: RecyclingRequest, status: RecyclingStatus, actorId: String): Notification? {
        val vendorId = request.vendorId ?: return null
        val toUser = actorId == vendorId
        return Notification(
            recipientId = if (toUser) request.userId else vendorId,
            senderId = actorId,
            type = NotificationType.RECYCLING_UPDATE,
            title = "Recycling ${status.displayName.lowercase()}",
            body = if (toUser) "${request.vendorName}: your ${request.material.displayName.lowercase()} furniture request is now ${status.displayName.lowercase()}."
            else "${request.userName} ${status.displayName.lowercase()} the recycling request.",
            relatedType = EntityType.RECYCLING_REQUEST,
            relatedId = request.id,
        )
    }

    /** A recycler claims an unassigned pickup: the person who asked for it is told. */
    fun pickupClaimed(request: RecyclingRequest, vendorId: String, vendorName: String) = Notification(
        recipientId = request.userId,
        senderId = vendorId,
        type = NotificationType.RECYCLING_UPDATE,
        title = "Pickup accepted",
        body = "$vendorName will collect your ${request.material.displayName.lowercase()} furniture.",
        relatedType = EntityType.RECYCLING_REQUEST,
        relatedId = request.id,
    )

    // ---- material requests ----

    fun materialOffer(request: MaterialRequest, offer: MaterialOffer) = Notification(
        recipientId = request.vendorId,
        senderId = offer.offererId,
        type = NotificationType.NEW_OFFER,
        title = "New offer on ${request.title}",
        body = "${offer.offererName} offered ${offer.listingTitle}",
        relatedType = EntityType.MATERIAL_REQUEST,
        relatedId = request.id,
    )

    fun offerDecision(request: MaterialRequest, offer: MaterialOffer, accepted: Boolean) = Notification(
        recipientId = offer.offererId,
        senderId = request.vendorId,
        type = NotificationType.VENDOR_RESPONSE,
        title = "${request.vendorName} ${if (accepted) "accepted" else "declined"} your offer",
        body = "${offer.listingTitle} for \"${request.title}\"",
        relatedType = EntityType.MATERIAL_REQUEST,
        relatedId = request.id,
    )
}
