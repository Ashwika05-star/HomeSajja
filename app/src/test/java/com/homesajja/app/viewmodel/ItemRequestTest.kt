package com.homesajja.app.viewmodel

import com.homesajja.app.data.model.EntityType
import com.homesajja.app.data.model.FurnitureCategory
import com.homesajja.app.data.model.ItemRequest
import com.homesajja.app.data.model.ItemRequestStatus
import com.homesajja.app.data.model.VendorBusinessType
import com.homesajja.app.repository.NotificationTemplates
import com.homesajja.app.ui.i18n.VendorStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemRequestTest {

    @Test
    fun itemRequest_defaultValues_areOpenAndUnaccepted() {
        val request = ItemRequest(
            id = "req_1",
            userId = "user_123",
            userName = "Rahul",
            title = "Dining Table",
            category = FurnitureCategory.TABLE,
            targetBudget = 8000L,
            city = "Bengaluru",
        )

        assertEquals("req_1", request.id)
        assertEquals("user_123", request.userId)
        assertEquals(ItemRequestStatus.OPEN, request.status)
        assertNull(request.acceptedVendorId)
        assertNull(request.acceptedVendorName)
        assertNull(request.vendorNote)
    }

    @Test
    fun itemRequest_acceptedByVendor_recordsVendorDetails() {
        val initial = ItemRequest(
            id = "req_1",
            userId = "user_123",
            userName = "Rahul",
            title = "Teak Coffee Table",
            category = FurnitureCategory.TABLE,
            targetBudget = 5000L,
            city = "Bengaluru",
        )

        val accepted = initial.copy(
            status = ItemRequestStatus.ACCEPTED,
            acceptedVendorId = "vendor_456",
            acceptedVendorName = "Classic Furnishings",
            vendorNote = "We have this in stock and can deliver in 2 days",
        )

        assertEquals(ItemRequestStatus.ACCEPTED, accepted.status)
        assertEquals("vendor_456", accepted.acceptedVendorId)
        assertEquals("Classic Furnishings", accepted.acceptedVendorName)
        assertEquals("We have this in stock and can deliver in 2 days", accepted.vendorNote)
    }

    @Test
    fun itemRequest_fulfilled_updatesStatus() {
        val accepted = ItemRequest(
            id = "req_1",
            userId = "user_123",
            status = ItemRequestStatus.ACCEPTED,
            acceptedVendorId = "vendor_456",
        )

        val fulfilled = accepted.copy(status = ItemRequestStatus.FULFILLED)
        assertEquals(ItemRequestStatus.FULFILLED, fulfilled.status)
    }

    @Test
    fun itemRequest_cancelled_updatesStatus() {
        val open = ItemRequest(
            id = "req_1",
            userId = "user_123",
            status = ItemRequestStatus.OPEN,
        )

        val cancelled = open.copy(status = ItemRequestStatus.CANCELLED)
        assertEquals(ItemRequestStatus.CANCELLED, cancelled.status)
    }

    @Test
    fun sectionsFor_allVendorsGetItemRequestsSection() {
        // Every business type should have RequestSection.ITEM_REQUESTS
        val businessTypes = VendorBusinessType.entries + listOf(null)

        for (type in businessTypes) {
            val sections = sectionsFor(type)
            assertTrue("Expected ITEM_REQUESTS for $type", sections.contains(RequestSection.ITEM_REQUESTS))
        }
    }

    @Test
    fun notificationTemplate_itemRequestAccepted_targetsCustomerWithDetails() {
        val request = ItemRequest(
            id = "req_99",
            userId = "customer_1",
            title = "Solid Oak Study Desk",
        )

        val notification = NotificationTemplates.itemRequestAccepted(
            request = request,
            vendorName = "Bangalore Woodworks",
            vendorId = "vendor_7",
        )

        assertEquals("customer_1", notification.recipientId)
        assertEquals("vendor_7", notification.senderId)
        assertEquals(EntityType.ITEM_REQUEST, notification.relatedType)
        assertEquals("req_99", notification.relatedId)
        assertTrue(notification.title.contains("Bangalore Woodworks"))
        assertTrue(notification.body.contains("Solid Oak Study Desk"))
    }

    @Test
    fun itemRequest_hindiTranslations_areAccurate() {
        assertEquals("अनुरोध", VendorStrings.itemRequests(true))
        assertEquals("आइटम का अनुरोध करें", VendorStrings.requestItem(true))
        assertEquals("फर्नीचर का अनुरोध करें", VendorStrings.requestItemTitle(true))
        assertEquals("अनुरोध स्वीकार करें", VendorStrings.acceptRequest(true))
        assertEquals("Royal Wood द्वारा स्वीकृत", VendorStrings.acceptedByVendor("Royal Wood", true))
        assertEquals("आपके द्वारा स्वीकृत", VendorStrings.acceptedByYou(true))
        assertEquals("पूर्ण हुआ चिह्नित करें", VendorStrings.markFulfilled(true))
        assertEquals("ग्राहक से चैट करें", VendorStrings.chatWithBuyer(true))
        assertEquals("विक्रेता से चैट करें", VendorStrings.chatWithVendor(true))
        assertEquals("अनुरोध रद्द करें", VendorStrings.cancelRequest(true))

        // English fallbacks
        assertEquals("Item requested", VendorStrings.itemRequests(false))
        assertEquals("Request item", VendorStrings.requestItem(false))
        assertEquals("Accept request", VendorStrings.acceptRequest(false))
        assertEquals("Accepted by Royal Wood", VendorStrings.acceptedByVendor("Royal Wood", false))
    }

    @Test
    fun itemRequestStatus_localizedLabel_formatsCorrectly() {
        assertEquals("खुला", ItemRequestStatus.OPEN.localizedLabel(true))
        assertEquals("स्वीकृत", ItemRequestStatus.ACCEPTED.localizedLabel(true))
        assertEquals("पूर्ण हुआ", ItemRequestStatus.FULFILLED.localizedLabel(true))
        assertEquals("रद्द", ItemRequestStatus.CANCELLED.localizedLabel(true))

        assertEquals("Open", ItemRequestStatus.OPEN.localizedLabel(false))
        assertEquals("Accepted", ItemRequestStatus.ACCEPTED.localizedLabel(false))
        assertEquals("Fulfilled", ItemRequestStatus.FULFILLED.localizedLabel(false))
        assertEquals("Cancelled", ItemRequestStatus.CANCELLED.localizedLabel(false))
    }
}
