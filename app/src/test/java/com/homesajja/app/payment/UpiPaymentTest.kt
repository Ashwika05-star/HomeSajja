package com.homesajja.app.payment

import com.homesajja.app.data.model.PurchaseRequest
import com.homesajja.app.data.model.PurchaseStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpiPaymentTest {

    @Test
    fun link_hasAllTheStandardParameters() {
        val link = UpiPayment.buildLinkString("meera@okhdfcbank", "Meera Shah", 12500, "HomeSajja: Teak sofa")
        assertEquals(
            "upi://pay?pa=meera%40okhdfcbank&pn=Meera%20Shah&am=12500.00&cu=INR&tn=HomeSajja%3A%20Teak%20sofa",
            link,
        )
    }

    @Test
    fun link_fallsBackToAGenericPayeeName_andTrimsTheNote() {
        val link = UpiPayment.buildLinkString(" a.b@upi ", " ", 100, "x".repeat(80))
        assertTrue(link.contains("pa=a.b%40upi"))
        assertTrue(link.contains("pn=HomeSajja%20seller"))
        assertTrue(link.endsWith("tn=" + "x".repeat(50)))
    }

    @Test
    fun upiIds_areCheckedForShape() {
        assertTrue(UpiPayment.isValidUpiId("meera@okhdfcbank"))
        assertTrue(UpiPayment.isValidUpiId("9876543210@paytm"))
        assertTrue(UpiPayment.isValidUpiId("  first.last-1@ybl  "))
        assertFalse(UpiPayment.isValidUpiId("meera"))
        assertFalse(UpiPayment.isValidUpiId("meera@"))
        assertFalse(UpiPayment.isValidUpiId("@bank"))
        assertFalse(UpiPayment.isValidUpiId("me era@bank"))
        assertFalse(UpiPayment.isValidUpiId(""))
    }

    private fun request(status: PurchaseStatus, upiId: String? = "s@bank", paid: Boolean = false) =
        PurchaseRequest(status = status, upiId = upiId, paid = paid)

    @Test
    fun buyerCanPayWithUpi_onlyAfterAcceptance_withAnIdAndNotYetPaid() {
        assertFalse(request(PurchaseStatus.REQUESTED).canPayWithUpi)
        assertTrue(request(PurchaseStatus.ACCEPTED).canPayWithUpi)
        assertTrue(request(PurchaseStatus.READY_FOR_PICKUP).canPayWithUpi)
        assertFalse(request(PurchaseStatus.ACCEPTED, upiId = null).canPayWithUpi)
        assertFalse(request(PurchaseStatus.ACCEPTED, paid = true).canPayWithUpi)
        assertFalse(request(PurchaseStatus.COMPLETED).canPayWithUpi)
    }

    @Test
    fun markingPaid_isOfferedEvenWithoutAnUpiId_butNotTwice() {
        assertTrue(request(PurchaseStatus.ACCEPTED, upiId = null).canMarkPaid)
        assertFalse(request(PurchaseStatus.ACCEPTED, paid = true).canMarkPaid)
        assertFalse(request(PurchaseStatus.REQUESTED).canMarkPaid)
        assertFalse(request(PurchaseStatus.CANCELLED).canMarkPaid)
    }
}
