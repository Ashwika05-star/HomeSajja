package com.homesajja.app.payment

import com.homesajja.app.data.model.CancelContext
import com.homesajja.app.data.model.CancelReason
import com.homesajja.app.data.model.Cancellation
import com.homesajja.app.data.model.EntityType
import com.homesajja.app.data.model.ExchangeRequest
import com.homesajja.app.data.model.ExchangeStatus
import com.homesajja.app.data.model.NotificationType
import com.homesajja.app.data.model.Payment
import com.homesajja.app.data.model.PaymentStatus
import com.homesajja.app.data.model.PurchaseRequest
import com.homesajja.app.data.model.RecyclingRequest
import com.homesajja.app.data.model.Refund
import com.homesajja.app.data.model.RefundStatus
import com.homesajja.app.data.model.RepairRequest
import com.homesajja.app.repository.NotificationTemplates
import com.homesajja.app.ui.components.refundLine
import com.homesajja.app.viewmodel.ExchangeAction
import com.homesajja.app.viewmodel.exchangeActionsFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CancelFormTest {

    @Test
    fun aReasonFromTheList_isRequired() {
        assertNotNull(cancelError(null, "", CancelContext.REPAIR))
        assertNull(cancelError(CancelReason.SCHEDULE_CONFLICT, "", CancelContext.REPAIR))
    }

    @Test
    fun noteIsOptional_butCapped() {
        assertNull(cancelError(CancelReason.OTHER, "", CancelContext.PURCHASE))
        assertNull(cancelError(CancelReason.OTHER, "   ", CancelContext.PURCHASE))
        assertNull(cancelError(CancelReason.OTHER, "x".repeat(MAX_CANCEL_NOTE_LENGTH), CancelContext.PURCHASE))
        assertNotNull(cancelError(CancelReason.OTHER, "x".repeat(MAX_CANCEL_NOTE_LENGTH + 1), CancelContext.PURCHASE))
    }

    @Test
    fun eachSystemOnlyAcceptsItsOwnShortList() {
        assertNotNull(cancelError(CancelReason.PARTS_UNAVAILABLE, "", CancelContext.PURCHASE))
        assertNotNull(cancelError(CancelReason.ITEM_UNAVAILABLE, "", CancelContext.REPAIR))
        assertNotNull(cancelError(CancelReason.PARTS_UNAVAILABLE, "", CancelContext.RECYCLING))
        assertNotNull(cancelError(CancelReason.CANNOT_COMPLETE_JOB, "", CancelContext.EXCHANGE))
        assertNull(cancelError(CancelReason.ITEM_UNAVAILABLE, "", CancelContext.EXCHANGE))
        assertNull(cancelError(CancelReason.PARTS_UNAVAILABLE, "", CancelContext.REPAIR))
    }

    @Test
    fun everyList_endsWithOther_andIsShort() {
        CancelContext.entries.forEach {
            assertEquals(CancelReason.OTHER, it.reasons.last())
            assertTrue("${it.name} has ${it.reasons.size}", it.reasons.size in 3..5)
            assertEquals(it.reasons.size, it.reasons.toSet().size)
        }
    }

    @Test
    fun buildCancellation_trimsTheNote_andRecordsWhoAndWhen() {
        val c = buildCancellation(CancelReason.CUSTOMER_UNREACHABLE, "  no answer on three calls  ", "vendor", now = 99L)
        assertEquals(CancelReason.CUSTOMER_UNREACHABLE, c.reason)
        assertEquals("no answer on three calls", c.note)
        assertEquals("vendor", c.cancelledBy)
        assertEquals(99L, c.cancelledAt)
    }
}

class RefundSafeguardTest {

    private fun payment(status: PaymentStatus, payer: String = "customer", payee: String = "vendor") =
        Payment(payerId = payer, payeeId = payee, status = status)

    @Test
    fun aRefundIsRequired_onlyWhenTheVendorConfirmedReceivingTheMoney() {
        assertTrue(refundRequired(payment(PaymentStatus.CONFIRMED), "vendor"))
        assertFalse(refundRequired(payment(PaymentStatus.MARKED_PAID), "vendor"))
        assertFalse(refundRequired(payment(PaymentStatus.UNPAID), "vendor"))
        assertFalse(refundRequired(null, "vendor"))
        assertFalse(refundRequired(payment(PaymentStatus.CONFIRMED), null))
    }

    @Test
    fun aVendorWhoPaidThePayoutThemselves_hasNothingToRefund() {
        // A recycler paying a customer: confirmed money went the other way, so no refund is asked of the recycler...
        assertFalse(refundRequired(payment(PaymentStatus.CONFIRMED, payer = "vendor", payee = "customer"), "vendor"))
        // ...and they can't cancel once money has left them.
        assertFalse(vendorCanCancelWithPayment(payment(PaymentStatus.MARKED_PAID, payer = "vendor", payee = "customer"), "vendor"))
        assertFalse(vendorCanCancelWithPayment(payment(PaymentStatus.CONFIRMED, payer = "vendor", payee = "customer"), "vendor"))
        assertTrue(vendorCanCancelWithPayment(payment(PaymentStatus.UNPAID, payer = "vendor", payee = "customer"), "vendor"))
    }

    @Test
    fun aVendorWhoIsPaid_canAlwaysCancel() {
        PaymentStatus.entries.forEach { assertTrue("$it", vendorCanCancelWithPayment(payment(it), "vendor")) }
        assertTrue(vendorCanCancelWithPayment(null, "vendor"))
    }

    @Test
    fun refundStatusShownToThePayer() {
        val refund = buildRefund(5000, now = 7L)
        assertEquals(RefundStatus.DONE, refundStatusFor(payment(PaymentStatus.CONFIRMED), refund))
        assertEquals(RefundStatus.NOT_NEEDED, refundStatusFor(payment(PaymentStatus.UNPAID), null))
        assertEquals(RefundStatus.NOT_NEEDED, refundStatusFor(null, null))
        assertEquals(RefundStatus.UNCONFIRMED, refundStatusFor(payment(PaymentStatus.MARKED_PAID), null))
    }

    @Test
    fun buildRefund_isMarkedDoneForTheFullAmount() {
        val refund = buildRefund(5000, now = 7L)
        assertEquals(5000L, refund.amount)
        assertEquals("DONE", refund.status)
        assertEquals(7L, refund.doneAt)
    }

    @Test
    fun refundLines_areWrittenFromTheViewersSide() {
        assertEquals("Refund: ₹5,000 marked as refunded by Vic.", refundLine(RefundStatus.DONE, 5000, "Vic", viewerIsPayer = true))
        assertEquals("Refund: ₹5,000 marked as refunded.", refundLine(RefundStatus.DONE, 5000, "Vic", viewerIsPayer = false))
        assertTrue(refundLine(RefundStatus.NOT_NEEDED, null, "Vic", true).contains("nothing to refund"))
        assertTrue(refundLine(RefundStatus.UNCONFIRMED, 5000, "Vic", true).contains("ask Vic for a refund"))
        assertTrue(refundLine(RefundStatus.UNCONFIRMED, 5000, "Vic", false).contains("never confirmed"))
    }

    @Test
    fun aRefundedPayment_stopsCountingAsEarned() {
        val confirmed = Payment(payerId = "customer", payeeId = "vendor", status = PaymentStatus.CONFIRMED)
        val kept = RepairRequest(agreedAmount = 2500, payment = confirmed)
        val refunded = RepairRequest(agreedAmount = 4000, payment = confirmed, refund = Refund(amount = 4000))
        val refundedPurchase = PurchaseRequest(agreedAmount = 700, payment = confirmed, refund = Refund(amount = 700))
        val refundedRecycling = RecyclingRequest(agreedAmount = 300, payment = confirmed, refund = Refund(amount = 300))
        assertEquals(2500L, confirmedEarnings("vendor", listOf(kept, refunded, refundedPurchase, refundedRecycling).map {
            when (it) {
                is RepairRequest -> it.receivable()
                is PurchaseRequest -> it.receivable()
                is RecyclingRequest -> it.receivable()
                else -> error("unexpected")
            }
        }))
    }
}

class ExchangeCancelActionsTest {

    private fun request(status: ExchangeStatus) = ExchangeRequest(id = "e", senderId = "sender", receiverId = "receiver", status = status)

    @Test
    fun aVendorPartyCanCancelAnAcceptedExchange() {
        assertEquals(
            listOf(ExchangeAction.COMPLETE, ExchangeAction.CANCEL_BY_VENDOR),
            exchangeActionsFor(request(ExchangeStatus.ACCEPTED), "receiver", viewerIsVendor = true),
        )
        assertEquals(
            listOf(ExchangeAction.COMPLETE, ExchangeAction.CANCEL_BY_VENDOR),
            exchangeActionsFor(request(ExchangeStatus.ACCEPTED), "sender", viewerIsVendor = true),
        )
    }

    @Test
    fun aPartyWithoutAVendorAccount_canOnlyComplete() {
        assertEquals(listOf(ExchangeAction.COMPLETE), exchangeActionsFor(request(ExchangeStatus.ACCEPTED), "receiver"))
        assertEquals(listOf(ExchangeAction.COMPLETE), exchangeActionsFor(request(ExchangeStatus.ACCEPTED), "sender", viewerIsVendor = false))
    }

    @Test
    fun strangersAndOtherStatuses_getNoVendorCancel() {
        assertTrue(exchangeActionsFor(request(ExchangeStatus.ACCEPTED), "stranger", viewerIsVendor = true).isEmpty())
        assertTrue(exchangeActionsFor(request(ExchangeStatus.ACCEPTED), null, viewerIsVendor = true).isEmpty())
        ExchangeStatus.entries.filter { it != ExchangeStatus.ACCEPTED }.forEach {
            assertTrue("$it", ExchangeAction.CANCEL_BY_VENDOR !in exchangeActionsFor(request(it), "receiver", viewerIsVendor = true))
            assertTrue("$it", ExchangeAction.CANCEL_BY_VENDOR !in exchangeActionsFor(request(it), "sender", viewerIsVendor = true))
        }
    }

    @Test
    fun theSendersOwnPendingCancel_isUnchanged() {
        assertEquals(listOf(ExchangeAction.CANCEL), exchangeActionsFor(request(ExchangeStatus.PENDING), "sender", viewerIsVendor = true))
        assertEquals(listOf(ExchangeAction.ACCEPT, ExchangeAction.DECLINE), exchangeActionsFor(request(ExchangeStatus.PENDING), "receiver", viewerIsVendor = true))
    }
}

class VendorCancelNotificationTest {

    private val cancellation = Cancellation(reason = CancelReason.PARTS_UNAVAILABLE, note = "The leg wood is out of stock", cancelledBy = "vendor")

    @Test
    fun theOtherPersonIsToldTheReasonAndTheNote() {
        val n = NotificationTemplates.vendorCancelled(
            "customer", "vendor", "Vic", "your repair of Sofa", cancellation, RefundStatus.NOT_NEEDED, null, EntityType.REPAIR_REQUEST, "r1",
        )
        assertEquals("customer", n.recipientId)
        assertEquals("vendor", n.senderId)
        assertEquals(NotificationType.VENDOR_CANCELLED, n.type)
        assertEquals(EntityType.REPAIR_REQUEST, n.relatedType)
        assertEquals("r1", n.relatedId)
        assertEquals("Vic cancelled your repair of Sofa", n.title)
        assertTrue(n.body.contains(CancelReason.PARTS_UNAVAILABLE.displayName))
        assertTrue(n.body.contains("The leg wood is out of stock"))
    }

    @Test
    fun theRefundIsMentioned_whenThereWasOne() {
        val done = NotificationTemplates.vendorCancelled(
            "customer", "vendor", "Vic", "your repair", cancellation, RefundStatus.DONE, 2500, EntityType.REPAIR_REQUEST, "r1",
        )
        assertTrue(done.body.contains("₹2,500"))
        assertTrue(done.body.contains("refunded"))
        val unconfirmed = NotificationTemplates.vendorCancelled(
            "customer", "vendor", "Vic", "your repair", cancellation, RefundStatus.UNCONFIRMED, null, EntityType.REPAIR_REQUEST, "r1",
        )
        assertTrue(unconfirmed.body.contains("check with Vic"))
        val none = NotificationTemplates.vendorCancelled(
            "customer", "vendor", "Vic", "your repair", cancellation, RefundStatus.NOT_NEEDED, null, EntityType.REPAIR_REQUEST, "r1",
        )
        assertFalse(none.body.contains("refund"))
    }

    @Test
    fun anExchangeCancelSaysNothingAboutMoney_andACancelWithoutANoteStaysTidy() {
        val noNote = cancellation.copy(note = "")
        val n = NotificationTemplates.vendorCancelled(
            "receiver", "sender", "Sam", "your exchange", noNote, null, null, EntityType.EXCHANGE_REQUEST, "e1",
        )
        assertEquals("Reason: ${CancelReason.PARTS_UNAVAILABLE.displayName}.", n.body)
    }
}
