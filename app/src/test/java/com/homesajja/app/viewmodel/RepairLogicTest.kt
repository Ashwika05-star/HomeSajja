package com.homesajja.app.viewmodel

import android.net.Uri
import com.homesajja.app.data.model.FurnitureCategory
import com.homesajja.app.data.model.FurnitureListing
import com.homesajja.app.data.model.Payment
import com.homesajja.app.data.model.PaymentStatus
import com.homesajja.app.data.model.RepairProblemType
import com.homesajja.app.data.model.RepairRequest
import com.homesajja.app.data.model.RepairStatus
import com.homesajja.app.data.model.VendorProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RepairActionsTest {

    private fun request(status: RepairStatus) = RepairRequest(id = "r", userId = "user", vendorId = "vendor", status = status)

    @Test
    fun vendorWalksTheWholePipeline_quoteFirst() {
        assertEquals(listOf(RepairAction.SEND_QUOTE, RepairAction.REJECT), repairActionsFor(request(RepairStatus.REQUESTED), "vendor"))
        assertTrue(repairActionsFor(request(RepairStatus.QUOTED), "vendor").isEmpty())
        assertEquals(listOf(RepairAction.START), repairActionsFor(request(RepairStatus.AGREED), "vendor"))
        assertEquals(listOf(RepairAction.MARK_READY), repairActionsFor(request(RepairStatus.IN_PROGRESS), "vendor"))
        assertEquals(listOf(RepairAction.COMPLETE), repairActionsFor(request(RepairStatus.READY), "vendor"))
    }

    @Test
    fun vendorCannotStartWork_beforeTheUserAgrees() {
        listOf(RepairStatus.REQUESTED, RepairStatus.QUOTED, RepairStatus.DECLINED).forEach {
            assertTrue("$it", RepairAction.START !in repairActionsFor(request(it), "vendor"))
        }
    }

    @Test
    fun userAcceptsOrDeclinesAQuote_onlyWhileItIsOpen() {
        assertEquals(listOf(RepairAction.ACCEPT_QUOTE, RepairAction.DECLINE_QUOTE), repairActionsFor(request(RepairStatus.QUOTED), "user"))
        assertTrue(RepairAction.ACCEPT_QUOTE !in repairActionsFor(request(RepairStatus.REQUESTED), "user"))
        assertTrue(RepairAction.ACCEPT_QUOTE !in repairActionsFor(request(RepairStatus.AGREED), "user"))
        assertTrue(repairActionsFor(request(RepairStatus.QUOTED), "vendor").none { it == RepairAction.ACCEPT_QUOTE || it == RepairAction.DECLINE_QUOTE })
    }

    @Test
    fun afterADeclinedQuote_vendorRevisesOrCloses_userCanCancel() {
        assertEquals(listOf(RepairAction.REVISE_QUOTE, RepairAction.CLOSE), repairActionsFor(request(RepairStatus.DECLINED), "vendor"))
        assertEquals(listOf(RepairAction.CANCEL), repairActionsFor(request(RepairStatus.DECLINED), "user"))
        assertTrue(RepairAction.REVISE_QUOTE.needsQuote && RepairAction.SEND_QUOTE.needsQuote)
        assertEquals(RepairStatus.QUOTED, RepairAction.REVISE_QUOTE.target)
        assertEquals(RepairStatus.REJECTED, RepairAction.CLOSE.target)
    }

    @Test
    fun legacyAcceptedRequests_carryOnAsAgreed() {
        assertEquals(listOf(RepairAction.START), repairActionsFor(request(RepairStatus.ACCEPTED), "vendor"))
        assertEquals(listOf(RepairAction.CANCEL), repairActionsFor(request(RepairStatus.ACCEPTED), "user"))
    }

    @Test
    fun userCanOnlyCancelBeforeWorkStarts_andNotOnceAPaymentIsMarked() {
        assertEquals(listOf(RepairAction.CANCEL), repairActionsFor(request(RepairStatus.REQUESTED), "user"))
        assertEquals(listOf(RepairAction.CANCEL), repairActionsFor(request(RepairStatus.AGREED), "user"))
        assertTrue(repairActionsFor(request(RepairStatus.IN_PROGRESS), "user").isEmpty())
        assertTrue(repairActionsFor(request(RepairStatus.READY), "user").isEmpty())
        val paid = request(RepairStatus.AGREED).copy(payment = Payment(payerId = "user", payeeId = "vendor", status = PaymentStatus.MARKED_PAID))
        assertTrue(repairActionsFor(paid, "user").isEmpty())
        val unpaid = request(RepairStatus.AGREED).copy(payment = Payment(payerId = "user", payeeId = "vendor"))
        assertEquals(listOf(RepairAction.CANCEL), repairActionsFor(unpaid, "user"))
    }

    @Test
    fun finishedRequestsAndStrangersGetNoActions() {
        listOf(RepairStatus.COMPLETED, RepairStatus.REJECTED, RepairStatus.CANCELLED).forEach {
            assertTrue(repairActionsFor(request(it), "vendor").isEmpty())
            assertTrue(repairActionsFor(request(it), "user").isEmpty())
        }
        assertTrue(repairActionsFor(request(RepairStatus.REQUESTED), "someone-else").isEmpty())
        assertTrue(repairActionsFor(request(RepairStatus.REQUESTED), null).isEmpty())
    }

    @Test
    fun everyActionMovesToItsTargetStatus() {
        assertEquals(RepairStatus.READY, RepairAction.MARK_READY.target)
        assertEquals(RepairStatus.CANCELLED, RepairAction.CANCEL.target)
    }
}

class RepairFormTest {

    private val listing = FurnitureListing(id = "l1", title = "Teak sofa", category = FurnitureCategory.SOFA)
    private val vendor = VendorProfile(uid = "v1", name = "Vic")

    @Test
    fun furnitureStep_needsAListingOrAFreeformCategory() {
        assertEquals("Pick one of your items, or choose \"Furniture not listed\".", RepairForm().validate(RepairStep.FURNITURE))
        assertEquals("Pick a category for your furniture.", RepairForm(notListed = true).validate(RepairStep.FURNITURE))
        assertNull(RepairForm(listing = listing).validate(RepairStep.FURNITURE))
        assertNull(RepairForm(notListed = true, category = FurnitureCategory.CHAIR).validate(RepairStep.FURNITURE))
    }

    @Test
    fun photosStep_needsAtLeastOnePhoto() {
        assertEquals("Add at least one photo of the damage.", RepairForm().validate(RepairStep.PHOTOS))
        assertNull(RepairForm(photos = listOf(SellPhoto.Remote("https://x/y.jpg"))).validate(RepairStep.PHOTOS))
    }

    @Test
    fun problemStep_needsAProblem() {
        assertEquals("Pick the problem that fits best.", RepairForm().validate(RepairStep.PROBLEM))
        assertNull(RepairForm(problem = RepairProblemType.SCRATCHES).validate(RepairStep.PROBLEM))
    }

    @Test
    fun descriptionStep_needsEnoughText() {
        assertTrue(RepairForm(description = "  short ").validate(RepairStep.DESCRIPTION) != null)
        assertNull(RepairForm(description = "Front leg cracked").validate(RepairStep.DESCRIPTION))
    }

    @Test
    fun locationAndProviderSteps() {
        assertTrue(RepairForm().validate(RepairStep.LOCATION) != null)
        assertNull(RepairForm(city = "Mumbai").validate(RepairStep.LOCATION))
        assertTrue(RepairForm().validate(RepairStep.PROVIDER) != null)
        assertNull(RepairForm(provider = vendor).validate(RepairStep.PROVIDER))
    }

    @Test
    fun furnitureTitle_prefersListingThenNameThenCategory() {
        assertEquals("Teak sofa", RepairForm(listing = listing).furnitureTitle)
        assertEquals("Rocking chair", RepairForm(notListed = true, category = FurnitureCategory.CHAIR, name = " Rocking chair ").furnitureTitle)
        assertEquals("Chair", RepairForm(notListed = true, category = FurnitureCategory.CHAIR).furnitureTitle)
        assertEquals(FurnitureCategory.SOFA, RepairForm(listing = listing).furnitureCategory)
    }
}

class RepairFilterTest {

    @Test
    fun activeAndClosedPartitionEveryStatus() {
        RepairStatus.entries.forEach { status ->
            assertTrue(RepairFilter.ALL.matches(status))
            assertTrue(RepairFilter.ACTIVE.matches(status) != RepairFilter.CLOSED.matches(status))
        }
        assertTrue(RepairFilter.ACTIVE.matches(RepairStatus.READY))
        assertTrue(RepairFilter.CLOSED.matches(RepairStatus.REJECTED))
    }
}
