package com.homesajja.app.viewmodel

import com.homesajja.app.data.model.RecycleCondition
import com.homesajja.app.data.model.RecycleMaterial
import com.homesajja.app.data.model.RecycleMethod
import com.homesajja.app.data.model.RecyclingRequest
import com.homesajja.app.data.model.RecyclingStatus
import com.homesajja.app.data.model.VendorProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecycleActionsTest {

    private fun request(status: RecyclingStatus, vendorId: String? = "recycler") =
        RecyclingRequest(id = "r", userId = "user", vendorId = vendorId, status = status)

    @Test
    fun recyclerWalksTheWholePipeline() {
        assertEquals(listOf(RecycleAction.ACCEPT, RecycleAction.REJECT), recycleActionsFor(request(RecyclingStatus.REQUESTED), "recycler"))
        assertEquals(listOf(RecycleAction.SCHEDULE), recycleActionsFor(request(RecyclingStatus.ACCEPTED), "recycler"))
        assertEquals(listOf(RecycleAction.COMPLETE), recycleActionsFor(request(RecyclingStatus.SCHEDULED), "recycler"))
    }

    @Test
    fun userCanOnlyCancelBeforeItIsScheduled() {
        assertEquals(listOf(RecycleAction.CANCEL), recycleActionsFor(request(RecyclingStatus.REQUESTED), "user"))
        assertEquals(listOf(RecycleAction.CANCEL), recycleActionsFor(request(RecyclingStatus.ACCEPTED), "user"))
        assertTrue(recycleActionsFor(request(RecyclingStatus.SCHEDULED), "user").isEmpty())
    }

    @Test
    fun unassignedPickup_canOnlyBeCancelledByItsUser() {
        val pickup = request(RecyclingStatus.REQUESTED, vendorId = null)
        assertEquals(listOf(RecycleAction.CANCEL), recycleActionsFor(pickup, "user"))
        assertTrue(recycleActionsFor(pickup, "recycler").isEmpty())
        assertTrue(recycleActionsFor(pickup, null).isEmpty())
    }

    @Test
    fun finishedRequestsGetNoActions() {
        listOf(RecyclingStatus.COMPLETED, RecyclingStatus.REJECTED, RecyclingStatus.CANCELLED).forEach {
            assertTrue(recycleActionsFor(request(it), "recycler").isEmpty())
            assertTrue(recycleActionsFor(request(it), "user").isEmpty())
        }
    }
}

class RecycleFormTest {

    private val recycler = VendorProfile(uid = "v1", name = "Rita")
    private val photo = SellPhoto.Remote("https://x/y.jpg")

    @Test
    fun eachStepNeedsItsChoice() {
        assertTrue(RecycleForm().validate(RecycleStep.PHOTOS) != null)
        assertNull(RecycleForm(photos = listOf(photo)).validate(RecycleStep.PHOTOS))
        assertTrue(RecycleForm().validate(RecycleStep.CONDITION) != null)
        assertNull(RecycleForm(condition = RecycleCondition.REUSABLE).validate(RecycleStep.CONDITION))
        assertTrue(RecycleForm().validate(RecycleStep.MATERIAL) != null)
        assertNull(RecycleForm(material = RecycleMaterial.WOOD).validate(RecycleStep.MATERIAL))
        assertTrue(RecycleForm().validate(RecycleStep.METHOD) != null)
        assertNull(RecycleForm(method = RecycleMethod.PICKUP).validate(RecycleStep.METHOD))
    }

    @Test
    fun pickup_onlyNeedsACity() {
        assertTrue(RecycleForm(method = RecycleMethod.PICKUP).validate(RecycleStep.DESTINATION) != null)
        assertNull(RecycleForm(method = RecycleMethod.PICKUP, city = "Mumbai").validate(RecycleStep.DESTINATION))
    }

    @Test
    fun dropOff_alsoNeedsARecycler() {
        val form = RecycleForm(method = RecycleMethod.DROP_OFF, city = "Mumbai")
        assertEquals("Choose a recycler to drop it off at.", form.validate(RecycleStep.DESTINATION))
        assertNull(form.copy(recycler = recycler).validate(RecycleStep.DESTINATION))
    }
}

class RecycleFilterTest {

    @Test
    fun activeAndClosedPartitionEveryStatus() {
        RecyclingStatus.entries.forEach { status ->
            assertTrue(RecycleFilter.ALL.matches(status))
            assertTrue(RecycleFilter.ACTIVE.matches(status) != RecycleFilter.CLOSED.matches(status))
        }
        assertTrue(RecycleFilter.ACTIVE.matches(RecyclingStatus.SCHEDULED))
        assertTrue(RecycleFilter.CLOSED.matches(RecyclingStatus.COMPLETED))
    }
}
