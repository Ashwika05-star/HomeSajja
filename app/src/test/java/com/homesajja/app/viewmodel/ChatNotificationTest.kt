package com.homesajja.app.viewmodel

import com.homesajja.app.data.model.Chat
import com.homesajja.app.data.model.EntityType
import com.homesajja.app.data.model.ExchangeRequest
import com.homesajja.app.data.model.MaterialOffer
import com.homesajja.app.data.model.MaterialRequest
import com.homesajja.app.data.model.Notification
import com.homesajja.app.data.model.NotificationType
import com.homesajja.app.data.model.PurchaseRequest
import com.homesajja.app.data.model.PurchaseStatus
import com.homesajja.app.data.model.RecycleMaterial
import com.homesajja.app.data.model.RecyclingRequest
import com.homesajja.app.data.model.RecyclingStatus
import com.homesajja.app.data.model.RepairRequest
import com.homesajja.app.data.model.RepairStatus
import com.homesajja.app.notification.NotificationRouting
import com.homesajja.app.repository.NotificationTemplates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatUnreadTest {

    private val chat = Chat(
        id = "c1",
        participantIds = listOf("alice", "bob"),
        participantNames = mapOf("alice" to "Alice", "bob" to "Bob"),
        lastMessage = "hi",
        lastMessageAt = 1_000,
        lastMessageSenderId = "bob",
    )

    @Test
    fun aMessageFromTheOtherPerson_isUnreadUntilTheChatIsOpened() {
        assertTrue(chat.isUnreadFor("alice"))
        assertFalse(chat.copy(readAt = mapOf("alice" to 1_000L)).isUnreadFor("alice"))
        assertTrue(chat.copy(readAt = mapOf("alice" to 500L)).isUnreadFor("alice"))
    }

    @Test
    fun yourOwnMessages_areNeverUnreadForYou() {
        assertFalse(chat.isUnreadFor("bob"))
    }

    @Test
    fun aChatWithNoMessages_isNotUnread() {
        assertFalse(Chat(participantIds = listOf("a", "b")).isUnreadFor("a"))
    }

    @Test
    fun otherParticipant_isFoundFromEitherSide() {
        assertEquals("bob", chat.otherParticipantId("alice"))
        assertEquals("Bob", chat.otherParticipantName("alice"))
        assertEquals("Alice", chat.otherParticipantName("bob"))
        assertEquals("Someone", Chat(participantIds = listOf("a", "b")).otherParticipantName("a"))
    }

    @Test
    fun unreadFilter_keepsOnlyUnreadChats() {
        val read = chat.copy(id = "c2", readAt = mapOf("alice" to 5_000L))
        assertEquals(listOf("c1"), ChatFilter.UNREAD.apply(listOf(chat, read), "alice").map { it.id })
        assertEquals(2, ChatFilter.ALL.apply(listOf(chat, read), "alice").size)
    }
}

class NotificationFilterTest {

    @Test
    fun unreadFilter_dropsSeenOnes() {
        val list = listOf(Notification(id = "1", seen = false), Notification(id = "2", seen = true))
        assertEquals(listOf("1"), NotificationFilter.UNREAD.apply(list).map { it.id })
        assertEquals(2, NotificationFilter.ALL.apply(list).size)
    }
}

class NotificationTemplatesTest {

    private val chat = Chat(id = "c1", participantIds = listOf("alice", "bob"), participantNames = mapOf("alice" to "Alice", "bob" to "Bob"))

    @Test
    fun newMessage_goesToTheOtherPerson_andPreviewsTheText() {
        val n = NotificationTemplates.newMessage(chat, senderId = "alice", text = "Is it still available?")!!
        assertEquals("bob", n.recipientId)
        assertEquals("alice", n.senderId)
        assertEquals(NotificationType.NEW_MESSAGE, n.type)
        assertEquals("New message from Alice", n.title)
        assertEquals(EntityType.CHAT, n.relatedType)
        assertEquals("c1", n.relatedId)
    }

    @Test
    fun newMessage_previewIsShortened() {
        assertEquals(80, NotificationTemplates.newMessage(chat, "alice", "x".repeat(500))!!.body.length)
    }

    @Test
    fun purchase_requestGoesToSeller_statusGoesToTheOtherSide() {
        val request = PurchaseRequest(id = "p", listingId = "l1", listingTitle = "Sofa", buyerId = "buyer", buyerName = "Bea", sellerId = "seller", offeredPrice = 9000)
        val created = NotificationTemplates.purchaseRequested(request)
        assertEquals("seller", created.recipientId)
        assertEquals(NotificationType.NEW_OFFER, created.type)
        assertEquals(EntityType.LISTING, created.relatedType)
        assertTrue(created.body.contains("9,000"))

        val accepted = NotificationTemplates.purchaseStatus(request, PurchaseStatus.ACCEPTED, actorId = "seller")
        assertEquals("buyer", accepted.recipientId)
        assertEquals(NotificationType.PURCHASE_UPDATE, accepted.type)

        val cancelled = NotificationTemplates.purchaseStatus(request, PurchaseStatus.CANCELLED, actorId = "buyer")
        assertEquals("seller", cancelled.recipientId)
    }

    @Test
    fun completingAPurchase_isASaleCompleted() {
        val request = PurchaseRequest(listingId = "l1", listingTitle = "Sofa", buyerId = "buyer", sellerId = "seller")
        assertEquals(NotificationType.SALE_COMPLETED, NotificationTemplates.purchaseStatus(request, PurchaseStatus.COMPLETED, "seller").type)
    }

    @Test
    fun exchange_requestGoesToReceiver_decisionGoesBackToSender() {
        val request = ExchangeRequest(id = "e", senderId = "s", senderName = "Sam", receiverId = "r", receiverName = "Rita", offeredTitle = "Chair", requestedTitle = "Table")
        assertEquals("r", NotificationTemplates.exchangeRequested(request).recipientId)
        val accepted = NotificationTemplates.exchangeDecision(request, accepted = true)
        assertEquals("s", accepted.recipientId)
        assertEquals(NotificationType.EXCHANGE_ACCEPTED, accepted.type)
        assertEquals(NotificationType.EXCHANGE_DECLINED, NotificationTemplates.exchangeDecision(request, accepted = false).type)
        assertEquals(EntityType.EXCHANGE_REQUEST, accepted.relatedType)
    }

    @Test
    fun repair_updatesGoToWhoeverDidNotAct() {
        val request = RepairRequest(id = "r", userId = "user", userName = "Uma", vendorId = "vendor", vendorName = "Vic", furnitureTitle = "Sofa")
        assertEquals("vendor", NotificationTemplates.repairRequested(request).recipientId)
        assertEquals("user", NotificationTemplates.repairStatus(request, RepairStatus.IN_PROGRESS, actorId = "vendor").recipientId)
        assertEquals("vendor", NotificationTemplates.repairStatus(request, RepairStatus.CANCELLED, actorId = "user").recipientId)
        assertEquals(NotificationType.REPAIR_UPDATE, NotificationTemplates.repairStatus(request, RepairStatus.READY, "vendor").type)
    }

    @Test
    fun recycling_anUnclaimedPickupHasNobodyToTell() {
        val pickup = RecyclingRequest(id = "c", userId = "user", vendorId = null, material = RecycleMaterial.WOOD)
        assertNull(NotificationTemplates.recyclingRequested(pickup))
        assertNull(NotificationTemplates.recyclingStatus(pickup, RecyclingStatus.CANCELLED, "user"))
    }

    @Test
    fun recycling_dropOffAndClaim() {
        val dropOff = RecyclingRequest(id = "c", userId = "user", userName = "Uma", vendorId = "vendor", vendorName = "Green Loop")
        assertEquals("vendor", NotificationTemplates.recyclingRequested(dropOff)!!.recipientId)
        assertEquals("user", NotificationTemplates.recyclingStatus(dropOff, RecyclingStatus.SCHEDULED, "vendor")!!.recipientId)

        val claimed = NotificationTemplates.pickupClaimed(RecyclingRequest(id = "c", userId = "user"), "vendor", "Green Loop")
        assertEquals("user", claimed.recipientId)
        assertEquals(NotificationType.RECYCLING_UPDATE, claimed.type)
    }

    @Test
    fun materialOffer_goesToTheVendor_andTheirAnswerBack() {
        val request = MaterialRequest(id = "m", vendorId = "vendor", vendorName = "Vic", title = "Teak wood")
        val offer = MaterialOffer(requestId = "m", offererId = "user", offererName = "Uma", listingTitle = "Old table")
        val offered = NotificationTemplates.materialOffer(request, offer)
        assertEquals("vendor", offered.recipientId)
        assertEquals(NotificationType.NEW_OFFER, offered.type)
        assertEquals(EntityType.MATERIAL_REQUEST, offered.relatedType)

        val answer = NotificationTemplates.offerDecision(request, offer, accepted = true)
        assertEquals("user", answer.recipientId)
        assertEquals(NotificationType.VENDOR_RESPONSE, answer.type)
    }

    @Test
    fun everyNotificationTemplate_hasARecipientAndAScreenToOpen() {
        val n = NotificationTemplates.newMessage(chat(), "alice", "hi")
        assertNotNull(n)
        assertTrue(n!!.recipientId.isNotBlank() && n.relatedId.isNotBlank())
    }

    private fun chat() = Chat(id = "c1", participantIds = listOf("alice", "bob"), participantNames = mapOf("alice" to "Alice", "bob" to "Bob"))
}

class NotificationRoutingTest {

    @Test
    fun eachRecordType_opensItsOwnScreen() {
        assertEquals("listing/l1", NotificationRouting.routeFor(EntityType.LISTING, "l1"))
        assertEquals("exchange/e1", NotificationRouting.routeFor(EntityType.EXCHANGE_REQUEST, "e1"))
        assertEquals("repair/r1", NotificationRouting.routeFor(EntityType.REPAIR_REQUEST, "r1"))
        assertEquals("recycling/c1", NotificationRouting.routeFor(EntityType.RECYCLING_REQUEST, "c1"))
        assertEquals("material/m1", NotificationRouting.routeFor(EntityType.MATERIAL_REQUEST, "m1"))
        assertEquals("chat/ch1", NotificationRouting.routeFor(EntityType.CHAT, "ch1"))
    }

    @Test
    fun aRecordWithNoScreen_orNoId_opensNothing() {
        assertNull(NotificationRouting.routeFor(EntityType.REVIEW, "x"))
        assertNull(NotificationRouting.routeFor(EntityType.LISTING, ""))
    }
}
