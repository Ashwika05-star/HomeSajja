package com.homesajja.app.payment

import com.homesajja.app.data.model.EntityType
import com.homesajja.app.data.model.NotificationType
import com.homesajja.app.data.model.PayDirection
import com.homesajja.app.data.model.Payment
import com.homesajja.app.data.model.PaymentMethod
import com.homesajja.app.data.model.PaymentStatus
import com.homesajja.app.data.model.PurchaseRequest
import com.homesajja.app.data.model.PurchaseStatus
import com.homesajja.app.data.model.Quote
import com.homesajja.app.data.model.RecyclingRequest
import com.homesajja.app.data.model.RecyclingStatus
import com.homesajja.app.data.model.RepairRequest
import com.homesajja.app.data.model.RepairStatus
import com.homesajja.app.data.model.VendorProfile
import com.homesajja.app.repository.NotificationTemplates
import com.homesajja.app.repository.PaymentWrites
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuoteFormTest {

    private fun repairForm(amount: String = "2500", days: String = "3", note: String = "") = QuoteForm(amount = amount, days = days, note = note)
    private fun recyclingForm(amount: String = "800", direction: PayDirection? = PayDirection.USER_PAYS_VENDOR) =
        QuoteForm(amount = amount, direction = direction)

    @Test
    fun aGoodRepairQuote_passes() {
        assertNull(quoteError(repairForm(), needsDays = true, needsDirection = false))
    }

    @Test
    fun amount_mustBeAPositiveWholeNumberWithinTheCap() {
        listOf("", " ", "abc", "0", "-5", "12.5").forEach {
            assertNotNull("'$it'", quoteError(repairForm(amount = it), needsDays = true, needsDirection = false))
        }
        assertNull(quoteError(repairForm(amount = "1000000"), needsDays = true, needsDirection = false))
        assertNotNull(quoteError(repairForm(amount = "1000001"), needsDays = true, needsDirection = false))
    }

    @Test
    fun repairQuote_needsEstimatedDays_withinRange() {
        assertNotNull(quoteError(repairForm(days = ""), needsDays = true, needsDirection = false))
        assertNotNull(quoteError(repairForm(days = "0"), needsDays = true, needsDirection = false))
        assertNotNull(quoteError(repairForm(days = "366"), needsDays = true, needsDirection = false))
        assertNull(quoteError(repairForm(days = "365"), needsDays = true, needsDirection = false))
    }

    @Test
    fun recyclingQuote_needsADirection_butNotDays() {
        assertNull(quoteError(recyclingForm(), needsDays = false, needsDirection = true))
        assertNotNull(quoteError(recyclingForm(direction = null), needsDays = false, needsDirection = true))
        assertNull(quoteError(recyclingForm(direction = PayDirection.VENDOR_PAYS_USER), needsDays = false, needsDirection = true))
    }

    @Test
    fun note_isOptional_butCapped() {
        assertNull(quoteError(repairForm(note = ""), needsDays = true, needsDirection = false))
        assertNull(quoteError(repairForm(note = "x".repeat(MAX_QUOTE_NOTE_LENGTH)), needsDays = true, needsDirection = false))
        assertNotNull(quoteError(repairForm(note = "x".repeat(MAX_QUOTE_NOTE_LENGTH + 1)), needsDays = true, needsDirection = false))
    }

    @Test
    fun buildQuote_trimsAndCarriesTheUpiIdOnlyWhenThereIsOne() {
        val quote = buildQuote(repairForm(amount = " 2500 ", days = " 4 ", note = "  new leg  "), " vic@okhdfcbank ", revision = 1, now = 7L)
        assertEquals(2500L, quote.amount)
        assertEquals(4, quote.estimatedDays)
        assertEquals("new leg", quote.note)
        assertEquals("vic@okhdfcbank", quote.payeeUpiId)
        assertEquals(7L, quote.sentAt)
        assertNull(buildQuote(repairForm(), "  ", revision = 1).payeeUpiId)
        assertNull(buildQuote(repairForm(), null, revision = 1).payeeUpiId)
    }

    @Test
    fun revisions_countUpFromOne() {
        assertEquals(1, nextRevision(null))
        assertEquals(2, nextRevision(Quote(amount = 100, revision = 1)))
        assertEquals(5, nextRevision(Quote(amount = 100, revision = 4)))
    }
}

class PaymentTurnTest {

    private fun payment(status: PaymentStatus) = Payment(payerId = "payer", payeeId = "payee", status = status)

    @Test
    fun payerPaysFirst_thenWaits() {
        assertEquals(PaymentTurn.PAY, payment(PaymentStatus.UNPAID).turnFor("payer"))
        assertEquals(PaymentTurn.WAIT_FOR_PAYEE, payment(PaymentStatus.MARKED_PAID).turnFor("payer"))
        assertEquals(PaymentTurn.DONE, payment(PaymentStatus.CONFIRMED).turnFor("payer"))
    }

    @Test
    fun payeeWaits_thenConfirms() {
        assertEquals(PaymentTurn.WAIT_FOR_PAYER, payment(PaymentStatus.UNPAID).turnFor("payee"))
        assertEquals(PaymentTurn.CONFIRM, payment(PaymentStatus.MARKED_PAID).turnFor("payee"))
        assertEquals(PaymentTurn.DONE, payment(PaymentStatus.CONFIRMED).turnFor("payee"))
    }

    @Test
    fun onlyThePayeeCanConfirm_onlyThePayerCanMarkPaid() {
        PaymentStatus.entries.forEach { status ->
            assertEquals(status == PaymentStatus.MARKED_PAID, payment(status).turnFor("payee") == PaymentTurn.CONFIRM)
            assertEquals(status == PaymentStatus.UNPAID, payment(status).turnFor("payer") == PaymentTurn.PAY)
        }
    }

    @Test
    fun strangersAndSignedOutViewers_getNothing() {
        PaymentStatus.entries.forEach {
            assertEquals(PaymentTurn.NONE, payment(it).turnFor("someone-else"))
            assertEquals(PaymentTurn.NONE, payment(it).turnFor(null))
        }
    }

    @Test
    fun newPayment_startsUnpaid_andTrimsTheUpiId() {
        val p = newPayment("a", "b", " b@okhdfcbank ")
        assertEquals(PaymentStatus.UNPAID, p.status)
        assertEquals("a", p.payerId)
        assertEquals("b", p.payeeId)
        assertEquals("b@okhdfcbank", p.payeeUpiId)
        assertNull(p.method)
        assertNull(p.confirmedAt)
        assertNull(newPayment("a", "b", "").payeeUpiId)
        assertNull(newPayment("a", "b", null).payeeUpiId)
    }

    @Test
    fun untouched_untilAPaymentIsMarked() {
        assertTrue((null as Payment?).isUntouched)
        assertTrue(payment(PaymentStatus.UNPAID).isUntouched)
        assertFalse(payment(PaymentStatus.MARKED_PAID).isUntouched)
        assertFalse(payment(PaymentStatus.CONFIRMED).isUntouched)
    }
}

class PaymentOpenTest {

    private val payment = Payment(payerId = "a", payeeId = "b")

    @Test
    fun purchase_isPayableFromAcceptanceOnward() {
        assertFalse(PurchaseRequest(status = PurchaseStatus.REQUESTED, payment = payment).paymentOpen)
        assertTrue(PurchaseRequest(status = PurchaseStatus.ACCEPTED, payment = payment).paymentOpen)
        assertTrue(PurchaseRequest(status = PurchaseStatus.READY_FOR_PICKUP, payment = payment).paymentOpen)
        assertTrue(PurchaseRequest(status = PurchaseStatus.COMPLETED, payment = payment).paymentOpen)
        assertFalse(PurchaseRequest(status = PurchaseStatus.CANCELLED, payment = payment).paymentOpen)
        assertFalse(PurchaseRequest(status = PurchaseStatus.ACCEPTED, payment = null).paymentOpen)
    }

    @Test
    fun repair_isPayableOnlyAfterTheUserAgrees() {
        listOf(RepairStatus.REQUESTED, RepairStatus.QUOTED, RepairStatus.DECLINED, RepairStatus.REJECTED, RepairStatus.CANCELLED).forEach {
            assertFalse("$it", RepairRequest(status = it, payment = payment).paymentOpen)
        }
        listOf(RepairStatus.AGREED, RepairStatus.IN_PROGRESS, RepairStatus.READY, RepairStatus.COMPLETED).forEach {
            assertTrue("$it", RepairRequest(status = it, payment = payment).paymentOpen)
        }
    }

    @Test
    fun recycling_isPayableOnceAccepted() {
        assertFalse(RecyclingRequest(status = RecyclingStatus.QUOTED, payment = payment).paymentOpen)
        assertFalse(RecyclingRequest(status = RecyclingStatus.DECLINED, payment = payment).paymentOpen)
        assertTrue(RecyclingRequest(status = RecyclingStatus.ACCEPTED, payment = payment).paymentOpen)
        assertTrue(RecyclingRequest(status = RecyclingStatus.SCHEDULED, payment = payment).paymentOpen)
        assertTrue(RecyclingRequest(status = RecyclingStatus.COMPLETED, payment = payment).paymentOpen)
        assertFalse(RecyclingRequest(status = RecyclingStatus.ACCEPTED, payment = null).paymentOpen)
    }

    @Test
    fun recyclingParties_followTheQuoteDirection() {
        val request = RecyclingRequest(userId = "customer", vendorId = "recycler")
        assertEquals("customer" to "recycler", recyclingParties(request, PayDirection.USER_PAYS_VENDOR))
        assertEquals("recycler" to "customer", recyclingParties(request, PayDirection.VENDOR_PAYS_USER))
    }
}

class EarningsTest {

    private fun payment(status: PaymentStatus, payee: String = "vendor", payer: String = "customer") =
        Payment(payerId = payer, payeeId = payee, status = status)

    @Test
    fun onlyConfirmedPaymentsCountAsEarned() {
        val items = listOf(
            Receivable(1000, payment(PaymentStatus.CONFIRMED)),
            Receivable(500, payment(PaymentStatus.MARKED_PAID)),
            Receivable(300, payment(PaymentStatus.UNPAID)),
        )
        assertEquals(1000L, confirmedEarnings("vendor", items))
    }

    @Test
    fun aPayersClaim_neverCountsUntilThePayeeConfirms() {
        assertEquals(0L, confirmedEarnings("vendor", listOf(Receivable(5000, payment(PaymentStatus.MARKED_PAID)))))
    }

    @Test
    fun moneyThePersonPaidOut_isNotEarnings() {
        // A recycler paying a customer: the recycler is the payer, so a confirmed payout is not income for them.
        val payout = Receivable(300, payment(PaymentStatus.CONFIRMED, payee = "customer", payer = "vendor"))
        assertEquals(0L, confirmedEarnings("vendor", listOf(payout)))
        assertEquals(300L, confirmedEarnings("customer", listOf(payout)))
    }

    @Test
    fun noPaymentRecordOrAmount_countsForNothing() {
        assertEquals(0L, confirmedEarnings("vendor", listOf(Receivable(100, null), Receivable(null, payment(PaymentStatus.CONFIRMED)))))
        assertEquals(0L, confirmedEarnings("vendor", emptyList()))
    }

    @Test
    fun sumsAcrossPurchasesRepairsAndRecycling() {
        val confirmed = payment(PaymentStatus.CONFIRMED)
        val purchase = PurchaseRequest(agreedAmount = 4000, payment = confirmed).receivable()
        val repair = RepairRequest(agreedAmount = 2500, payment = confirmed).receivable()
        val recycling = RecyclingRequest(agreedAmount = 800, payment = confirmed).receivable()
        assertEquals(7300L, confirmedEarnings("vendor", listOf(purchase, repair, recycling)))
    }
}

class PaymentWritesTest {

    @Test
    fun markPaidByUpi_keepsATrimmedReference() {
        val changes = PaymentWrites.markPaid(PaymentMethod.UPI, "  UTR123  ", now = 50L)
        assertEquals("MARKED_PAID", changes["payment.status"])
        assertEquals("UPI", changes["payment.method"])
        assertEquals("UTR123", changes["payment.upiRef"])
        assertEquals(50L, changes["payment.markedPaidAt"])
        assertEquals(50L, changes["updatedAt"])
    }

    @Test
    fun markPaidInCash_neverKeepsAReference() {
        assertNull(PaymentWrites.markPaid(PaymentMethod.CASH, "UTR123", now = 1L)["payment.upiRef"])
        assertNull(PaymentWrites.markPaid(PaymentMethod.UPI, "   ", now = 1L)["payment.upiRef"])
    }

    @Test
    fun theWritesTouchOnlyThePaymentStepFields() {
        assertEquals(
            setOf("payment.status", "payment.method", "payment.upiRef", "payment.markedPaidAt", "updatedAt"),
            PaymentWrites.markPaid(PaymentMethod.UPI, null, 1L).keys,
        )
        val confirm = PaymentWrites.confirmReceived(now = 9L)
        assertEquals(setOf("payment.status", "payment.confirmedAt", "updatedAt"), confirm.keys)
        assertEquals("CONFIRMED", confirm["payment.status"])
        assertEquals(9L, confirm["payment.confirmedAt"])
    }
}

class AgreementNotificationTest {

    private val repair = RepairRequest(
        id = "r", userId = "user", userName = "Uma", vendorId = "vendor", vendorName = "Vic", furnitureTitle = "Sofa",
    )
    private val quote = Quote(amount = 2500, estimatedDays = 3, revision = 1)

    @Test
    fun repairQuote_goesToTheCustomer_withThePriceAndDays() {
        val n = NotificationTemplates.repairQuoteSent(repair, quote)
        assertEquals("user", n.recipientId)
        assertEquals("vendor", n.senderId)
        assertEquals(NotificationType.REPAIR_UPDATE, n.type)
        assertEquals(EntityType.REPAIR_REQUEST, n.relatedType)
        assertEquals("r", n.relatedId)
        assertTrue(n.body.contains("₹2,500"))
        assertTrue(n.body.contains("3 days"))
        assertEquals("Repair quote received", n.title)
    }

    @Test
    fun aRevisedQuote_isLabelledAsRevised() {
        assertEquals("Revised repair quote", NotificationTemplates.repairQuoteSent(repair, quote.copy(revision = 2)).title)
    }

    @Test
    fun repairQuoteDecision_goesBackToTheVendor() {
        val accepted = NotificationTemplates.repairQuoteDecision(repair.copy(quote = quote), accepted = true)
        assertEquals("vendor", accepted.recipientId)
        assertEquals("user", accepted.senderId)
        assertEquals("Quote accepted", accepted.title)
        assertTrue(accepted.body.contains("₹2,500"))
        val declined = NotificationTemplates.repairQuoteDecision(repair.copy(quote = quote), accepted = false)
        assertEquals("Quote declined", declined.title)
        assertEquals("vendor", declined.recipientId)
    }

    @Test
    fun recyclingQuote_saysWhoPays_andAnUnclaimedPickupHasNobodyToTell() {
        val request = RecyclingRequest(id = "c", userId = "user", userName = "Uma", vendorId = "vendor", vendorName = "Green Loop")
        val toCustomer = NotificationTemplates.recyclingQuoteSent(request, Quote(amount = 800, direction = PayDirection.USER_PAYS_VENDOR))!!
        assertEquals("user", toCustomer.recipientId)
        assertTrue(toCustomer.body.contains("you pay the recycler"))
        val payout = NotificationTemplates.recyclingQuoteSent(request, Quote(amount = 300, direction = PayDirection.VENDOR_PAYS_USER))!!
        assertTrue(payout.body.contains("the recycler pays you"))
        assertNull(NotificationTemplates.recyclingQuoteSent(request.copy(vendorId = null), quote))
        assertNull(NotificationTemplates.recyclingQuoteDecision(request.copy(vendorId = null), accepted = true))
        assertEquals("vendor", NotificationTemplates.recyclingQuoteDecision(request, accepted = false)!!.recipientId)
    }

    @Test
    fun paymentMarked_goesToThePayee_andConfirmedGoesToThePayer() {
        val payment = Payment(payerId = "user", payeeId = "vendor")
        val marked = NotificationTemplates.paymentMarked(payment, 2500, "the repair of Sofa", "Uma", EntityType.REPAIR_REQUEST, "r")
        assertEquals("vendor", marked.recipientId)
        assertEquals("user", marked.senderId)
        assertEquals(NotificationType.PAYMENT_UPDATE, marked.type)
        assertTrue(marked.body.contains("₹2,500"))
        val confirmed = NotificationTemplates.paymentConfirmed(payment, 2500, "the repair of Sofa", "Vic", EntityType.REPAIR_REQUEST, "r")
        assertEquals("user", confirmed.recipientId)
        assertEquals("vendor", confirmed.senderId)
        assertEquals("Payment received", confirmed.title)
    }
}
