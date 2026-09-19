package com.homesajja.app.viewmodel

import com.homesajja.app.data.model.Cities
import com.homesajja.app.data.model.ExchangeRequest
import com.homesajja.app.data.model.ExchangeStatus
import com.homesajja.app.data.model.FurnitureListing
import com.homesajja.app.data.model.ListingStatus
import com.homesajja.app.data.model.MaterialType
import com.homesajja.app.data.model.PurchaseRequest
import com.homesajja.app.data.model.PurchaseStatus
import com.homesajja.app.data.model.RecycleMaterial
import com.homesajja.app.data.model.RecyclingRequest
import com.homesajja.app.data.model.RecyclingStatus
import com.homesajja.app.data.model.RepairRequest
import com.homesajja.app.data.model.RepairStatus
import com.homesajja.app.data.model.VendorBusinessType
import com.homesajja.app.data.model.VendorProfile
import com.homesajja.app.repository.VendorInbox
import com.homesajja.app.ui.util.budgetLabel
import com.homesajja.app.ui.util.formatTimeAgo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardTest {

    private val vendor = VendorProfile(uid = "v", name = "Vic", businessName = "Vic Repairs", city = "Mumbai")

    private fun listing(status: ListingStatus) = FurnitureListing(id = status.name, ownerId = "v", status = status)

    @Test
    fun activeListings_countsOnlyActiveOnes() {
        val listings = listOf(
            listing(ListingStatus.ACTIVE), listing(ListingStatus.ACTIVE), listing(ListingStatus.RESERVED),
            listing(ListingStatus.SOLD), listing(ListingStatus.UNAVAILABLE),
        )
        assertEquals(2, buildDashboard(vendor, listings, VendorInbox()).stats.activeListings)
    }

    @Test
    fun pendingRequests_addUpAcrossTheFourSystems() {
        val inbox = VendorInbox(
            purchases = listOf(
                PurchaseRequest(id = "p1", status = PurchaseStatus.REQUESTED),
                PurchaseRequest(id = "p2", status = PurchaseStatus.ACCEPTED),
            ),
            exchanges = listOf(ExchangeRequest(id = "e1", status = ExchangeStatus.PENDING), ExchangeRequest(id = "e2", status = ExchangeStatus.COMPLETED)),
            repairs = listOf(RepairRequest(id = "r1", status = RepairStatus.REQUESTED)),
            recycling = listOf(RecyclingRequest(id = "c1", status = RecyclingStatus.REQUESTED), RecyclingRequest(id = "c2", status = RecyclingStatus.SCHEDULED)),
        )
        assertEquals(4, buildDashboard(vendor, emptyList(), inbox).stats.pendingRequests)
    }

    @Test
    fun completedSales_areCompletedPurchasesOnly() {
        val inbox = VendorInbox(
            purchases = listOf(
                PurchaseRequest(id = "p1", status = PurchaseStatus.COMPLETED),
                PurchaseRequest(id = "p2", status = PurchaseStatus.COMPLETED),
                PurchaseRequest(id = "p3", status = PurchaseStatus.REJECTED),
            ),
            exchanges = listOf(ExchangeRequest(id = "e1", status = ExchangeStatus.COMPLETED)),
        )
        assertEquals(2, buildDashboard(vendor, emptyList(), inbox).stats.completedSales)
    }

    @Test
    fun activityFeed_isNewestFirst_andCapped() {
        val purchases = (1..4).map { PurchaseRequest(id = "p$it", buyerName = "B$it", listingTitle = "Item", updatedAt = it * 1000L) }
        val repairs = listOf(RepairRequest(id = "r1", userName = "U", furnitureTitle = "Sofa", updatedAt = 10_000L))
        val recycling = listOf(RecyclingRequest(id = "c1", userName = "R", material = RecycleMaterial.WOOD, updatedAt = 500L))
        val activity = buildDashboard(vendor, emptyList(), VendorInbox(purchases = purchases, repairs = repairs, recycling = recycling)).activity

        assertEquals(RECENT_ACTIVITY_COUNT, activity.size)
        assertEquals(ActivityKind.REPAIR, activity.first().kind)
        assertEquals(listOf(10_000L, 4_000L, 3_000L, 2_000L, 1_000L), activity.map { it.timestamp })
    }

    @Test
    fun emptyInbox_givesZerosAndNoActivity() {
        val data = buildDashboard(vendor, emptyList(), VendorInbox())
        assertEquals(DashboardStats(0, 0, 0), data.stats)
        assertTrue(data.activity.isEmpty())
    }

    @Test
    fun profileIncomplete_untilDescriptionAndLocationAreSet() {
        assertTrue(buildDashboard(vendor, emptyList(), VendorInbox()).profileIncomplete)
        val noPin = vendor.copy(description = "We fix furniture")
        assertTrue(buildDashboard(noPin, emptyList(), VendorInbox()).profileIncomplete)
        val done = noPin.copy(shopLatitude = 19.0, shopLongitude = 72.8)
        assertFalse(buildDashboard(done, emptyList(), VendorInbox()).profileIncomplete)
    }
}

class RequestSectionsTest {

    @Test
    fun everyVendorGetsPurchasesAndExchanges() {
        val sections = sectionsFor(VendorBusinessType.SHOP)
        assertEquals(listOf(RequestSection.PURCHASES, RequestSection.EXCHANGES), sections)
    }

    @Test
    fun repairProvidersAlsoGetRepairs_recyclersGetRecycling() {
        assertTrue(RequestSection.REPAIRS in sectionsFor(VendorBusinessType.REPAIR_PROFESSIONAL))
        assertTrue(RequestSection.REPAIRS in sectionsFor(VendorBusinessType.CARPENTER))
        assertFalse(RequestSection.RECYCLING in sectionsFor(VendorBusinessType.CARPENTER))
        assertTrue(RequestSection.RECYCLING in sectionsFor(VendorBusinessType.RECYCLER))
        assertFalse(RequestSection.REPAIRS in sectionsFor(VendorBusinessType.RECYCLER))
    }

    @Test
    fun unknownBusinessType_getsTheBasics() {
        assertEquals(2, sectionsFor(null).size)
    }
}

class MaterialRequestFormTest {

    private val valid = MaterialRequestForm(
        title = "Old teak wood",
        materialType = MaterialType.WOOD,
        quantity = "20 kg",
        description = "Looking for seasoned teak planks",
    )

    @Test
    fun aCompleteFormPasses_andBudgetIsOptional() {
        assertNull(valid.validate())
    }

    @Test
    fun eachRequiredFieldIsChecked() {
        assertTrue(valid.copy(title = "ab").validate() != null)
        assertTrue(valid.copy(materialType = null).validate() != null)
        assertTrue(valid.copy(quantity = " ").validate() != null)
        assertTrue(valid.copy(description = "short").validate() != null)
    }

    @Test
    fun budget_mustBeNumbersInOrder() {
        assertNull(valid.copy(budgetMin = "500", budgetMax = "2000").validate())
        assertNull(valid.copy(budgetMax = "2000").validate())
        assertEquals("The minimum budget can't be above the maximum.", valid.copy(budgetMin = "3000", budgetMax = "2000").validate())
        assertTrue(valid.copy(budgetMin = "abc").validate() != null)
        assertEquals(500L, valid.copy(budgetMin = " 500 ").parsedBudgetMin)
    }
}

class VendorProfileFormTest {

    @Test
    fun needsBusinessNameAndType() {
        assertEquals("Enter your business name.", VendorProfileForm().validate())
        assertEquals("Pick your business type.", VendorProfileForm(businessName = "Vic").validate())
        assertNull(VendorProfileForm(businessName = "Vic", businessType = VendorBusinessType.SHOP).validate())
    }
}

class FormattingTest {

    @Test
    fun timeAgo_picksTheRightUnit() {
        val now = 10_000_000_000L
        assertEquals("just now", formatTimeAgo(now - 30_000, now))
        assertEquals("5 min ago", formatTimeAgo(now - 5 * 60_000, now))
        assertEquals("3 h ago", formatTimeAgo(now - 3 * 3_600_000, now))
        assertEquals("2 d ago", formatTimeAgo(now - 2 * 86_400_000, now))
    }

    @Test
    fun budgetLabel_handlesOpenEnds() {
        assertNull(budgetLabel(null, null))
        assertEquals("From ₹500", budgetLabel(500, null))
        assertEquals("Up to ₹2,000", budgetLabel(null, 2000))
        assertEquals("₹500 – ₹2,000", budgetLabel(500, 2000))
    }

    @Test
    fun cityCentres_fallBackToMumbai() {
        assertEquals(Cities.centreOf("Mumbai"), Cities.centreOf("Atlantis"))
        assertTrue(Cities.ALL.all { Cities.centreOf(it).first in 8.0..35.0 })
    }
}
