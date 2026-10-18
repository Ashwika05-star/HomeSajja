package com.homesajja.app.viewmodel

import com.homesajja.app.data.model.EntityType
import com.homesajja.app.data.model.FurnitureCategory
import com.homesajja.app.data.model.ItemRequest
import com.homesajja.app.data.model.ItemRequestStatus
import com.homesajja.app.data.model.Payment
import com.homesajja.app.data.model.PaymentMethod
import com.homesajja.app.data.model.PaymentStatus
import com.homesajja.app.data.model.Quote
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
        assertNull(request.quote)
        assertNull(request.payment)
        assertFalse(request.isAgreed)
    }

    @Test
    fun itemRequest_quotedByVendor_recordsQuoteDetails() {
        val initial = ItemRequest(
            id = "req_1",
            userId = "user_123",
            userName = "Rahul",
            title = "Teak Coffee Table",
            category = FurnitureCategory.TABLE,
            targetBudget = 5000L,
            city = "Bengaluru",
        )

        val quote = Quote(
            amount = 4800L,
            estimatedDays = 4,
            note = "Can craft with solid sheesham wood",
            payeeUpiId = "vendor@okaxis",
        )

        val quoted = initial.copy(
            status = ItemRequestStatus.QUOTED,
            quote = quote,
            acceptedVendorId = "vendor_456",
            acceptedVendorName = "Classic Furnishings",
            vendorPhone = "9876543210",
        )

        assertEquals(ItemRequestStatus.QUOTED, quoted.status)
        assertEquals(4800L, quoted.quote?.amount)
        assertEquals(4, quoted.quote?.estimatedDays)
        assertEquals("vendor_456", quoted.acceptedVendorId)
        assertEquals("Classic Furnishings", quoted.acceptedVendorName)
    }

    @Test
    fun itemRequest_quoteAccepted_locksPriceAndOpensPayment() {
        val quote = Quote(amount = 4500L, estimatedDays = 3, payeeUpiId = "shop@upi")
        val quoted = ItemRequest(
            id = "req_1",
            userId = "user_1",
            userName = "Amit",
            title = "Bookshelf",
            status = ItemRequestStatus.QUOTED,
            quote = quote,
            acceptedVendorId = "vendor_9",
            acceptedVendorName = "Urban Crafts",
        )

        val agreed = quoted.copy(
            status = ItemRequestStatus.AGREED,
            agreedAmount = quote.amount,
            agreedAt = 1000L,
            payment = Payment(
                payerId = "user_1",
                payeeId = "vendor_9",
                payeeUpiId = quote.payeeUpiId,
                status = PaymentStatus.UNPAID,
            ),
        )

        assertTrue(agreed.isAgreed)
        assertTrue(agreed.paymentOpen)
        assertEquals(4500L, agreed.agreedAmount)
        assertEquals(PaymentStatus.UNPAID, agreed.payment?.status)
        assertEquals("user_1", agreed.payment?.payerId)
        assertEquals("vendor_9", agreed.payment?.payeeId)
    }

    @Test
    fun itemRequest_productionProgress_movesThroughStages() {
        val agreed = ItemRequest(
            id = "req_1",
            status = ItemRequestStatus.AGREED,
            agreedAmount = 5000L,
        )

        val inProgress = agreed.copy(status = ItemRequestStatus.IN_PROGRESS)
        assertEquals(ItemRequestStatus.IN_PROGRESS, inProgress.status)
        assertTrue(inProgress.isAgreed)

        val ready = inProgress.copy(status = ItemRequestStatus.READY)
        assertEquals(ItemRequestStatus.READY, ready.status)
        assertTrue(ready.isAgreed)

        val completed = ready.copy(status = ItemRequestStatus.COMPLETED)
        assertEquals(ItemRequestStatus.COMPLETED, completed.status)
        assertTrue(completed.isAgreed)
    }

    @Test
    fun itemRequest_paymentConfirmation_integratesWithEarnings() {
        val vendorId = "vendor_9"
        val request = ItemRequest(
            id = "req_100",
            userId = "user_1",
            userName = "Pooja",
            title = "Custom TV Unit",
            status = ItemRequestStatus.COMPLETED,
            acceptedVendorId = vendorId,
            agreedAmount = 7500L,
            payment = Payment(
                payerId = "user_1",
                payeeId = vendorId,
                method = PaymentMethod.UPI,
                status = PaymentStatus.CONFIRMED,
                confirmedAt = 12345678L,
            ),
        )

        val earned = request.toEarnedItem(vendorId)
        assertNotNull(earned)
        assertEquals("req_100", earned?.id)
        assertEquals(7500L, earned?.amount)
        assertEquals(EarnedKind.ITEM_REQUEST, earned?.kind)
        assertEquals("Custom TV Unit", earned?.title)
        assertEquals("Pooja", earned?.counterpart)
        assertEquals(PaymentMethod.UPI, earned?.method)

        // Other person (not payee) earns nothing
        assertNull(request.toEarnedItem("other_person"))

        // Unpaid request earns nothing
        val unpaid = request.copy(payment = request.payment?.copy(status = PaymentStatus.UNPAID))
        assertNull(unpaid.toEarnedItem(vendorId))
    }

    @Test
    fun sectionsFor_allVendorsGetItemRequestsSection() {
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
        assertEquals("कोटेशन भेजें", VendorStrings.sendQuote(true))
        assertEquals("कोटेशन स्वीकार करें", VendorStrings.acceptQuote(true))
        assertEquals("अस्वीकार करें", VendorStrings.declineQuote(true))

        // English fallbacks
        assertEquals("Item requested", VendorStrings.itemRequests(false))
        assertEquals("Request item", VendorStrings.requestItem(false))
        assertEquals("Accept request", VendorStrings.acceptRequest(false))
        assertEquals("Accepted by Royal Wood", VendorStrings.acceptedByVendor("Royal Wood", false))
        assertEquals("Send quote", VendorStrings.sendQuote(false))
        assertEquals("Accept quote", VendorStrings.acceptQuote(false))
    }

    @Test
    fun itemRequestStatus_localizedLabel_formatsCorrectly() {
        assertEquals("खुला", ItemRequestStatus.OPEN.localizedLabel(true))
        assertEquals("स्वीकृत", ItemRequestStatus.ACCEPTED.localizedLabel(true))
        assertEquals("पूर्ण हुआ", ItemRequestStatus.FULFILLED.localizedLabel(true))
        assertEquals("रद्द", ItemRequestStatus.CANCELLED.localizedLabel(true))
        assertEquals("कोटेशन प्राप्त", ItemRequestStatus.QUOTED.localizedLabel(true))
        assertEquals("प्रगति पर", ItemRequestStatus.IN_PROGRESS.localizedLabel(true))
        assertEquals("तैयार", ItemRequestStatus.READY.localizedLabel(true))

        assertEquals("Open", ItemRequestStatus.OPEN.localizedLabel(false))
        assertEquals("Accepted", ItemRequestStatus.ACCEPTED.localizedLabel(false))
        assertEquals("Fulfilled", ItemRequestStatus.FULFILLED.localizedLabel(false))
        assertEquals("Cancelled", ItemRequestStatus.CANCELLED.localizedLabel(false))
        assertEquals("Quoted", ItemRequestStatus.QUOTED.localizedLabel(false))
        assertEquals("In progress", ItemRequestStatus.IN_PROGRESS.localizedLabel(false))
        assertEquals("Ready", ItemRequestStatus.READY.localizedLabel(false))
    }
}
