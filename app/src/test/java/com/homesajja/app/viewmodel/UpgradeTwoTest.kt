package com.homesajja.app.viewmodel

import com.homesajja.app.data.model.FurnitureCategory
import com.homesajja.app.data.model.FurnitureListing
import com.homesajja.app.data.model.ListingFilters
import com.homesajja.app.data.model.ListingStatus
import com.homesajja.app.data.model.MaterialType
import com.homesajja.app.data.model.SellerType
import com.homesajja.app.data.model.visibleBrowseListings
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Upgrade 2: own listings in the Explore feed, the Individual / Vendor filter, and the owner's detail screen. */
class BrowseFeedTest {

    private val me = "me"

    private fun listing(
        id: String,
        owner: String,
        seller: SellerType = SellerType.INDIVIDUAL,
        title: String = "Item $id",
        price: Long = 1_000,
    ) = FurnitureListing(id = id, ownerId = owner, sellerType = seller, title = title, price = price, material = MaterialType.WOOD)

    private val mineIndividual = listing("m1", me, SellerType.INDIVIDUAL, title = "My sofa")
    private val otherIndividual = listing("i1", "ann", SellerType.INDIVIDUAL, title = "Ann's sofa")
    private val vendorA = listing("v1", "vic", SellerType.VENDOR, title = "Vendor sofa", price = 9_000)
    private val myVendorShop = listing("m2", "me", SellerType.VENDOR, title = "My shop sofa")
    private val all = listOf(mineIndividual, otherIndividual, vendorA, myVendorShop)

    private fun ids(list: List<FurnitureListing>) = list.map { it.id }

    private fun visible(
        includeOwn: Boolean = true,
        sellerType: SellerType? = null,
        filters: ListingFilters = ListingFilters(),
        query: String = "",
    ) = ids(visibleBrowseListings(all, me, includeOwn, sellerType, filters, query))

    @Test
    fun explore_showsOwnListingsAlongsideEveryoneElses() {
        assertEquals(listOf("m1", "i1", "v1", "m2"), visible(includeOwn = true))
    }

    @Test
    fun exchange_stillHidesOwnListings() {
        assertEquals(listOf("i1", "v1"), visible(includeOwn = false))
    }

    @Test
    fun sellerType_individualKeepsOnlyIndividuals_includingMine() {
        assertEquals(listOf("m1", "i1"), visible(sellerType = SellerType.INDIVIDUAL))
    }

    @Test
    fun sellerType_vendorKeepsOnlyVendors_includingMyShop() {
        assertEquals(listOf("v1", "m2"), visible(sellerType = SellerType.VENDOR))
    }

    @Test
    fun sellerType_all_isNoRestriction() {
        assertEquals(4, visible(sellerType = null).size)
    }

    @Test
    fun sellerType_combinesWithSearchAndFilterSheet() {
        assertEquals(listOf("v1"), visible(sellerType = SellerType.VENDOR, query = "vendor"))
        assertEquals(listOf("v1"), visible(sellerType = SellerType.VENDOR, filters = ListingFilters(minPrice = 5_000)))
        assertTrue(visible(sellerType = SellerType.INDIVIDUAL, filters = ListingFilters(minPrice = 5_000)).isEmpty())
    }

    @Test
    fun sellerType_withOwnListingsHidden_stillFiltersTheRest() {
        assertEquals(listOf("v1"), visible(includeOwn = false, sellerType = SellerType.VENDOR))
    }

    @Test
    fun noMatches_givesAnEmptyList_forTheNoResultsState() {
        assertTrue(visible(sellerType = SellerType.VENDOR, query = "zzz").isEmpty())
    }

    @Test
    fun signedOutViewer_ownsNothing() {
        // A null id must never match a listing's owner (and never hide anything for "own").
        assertEquals(4, ids(visibleBrowseListings(all, null, false, null, ListingFilters(), "")).size)
    }
}

class OwnerDetailTest {

    private fun detail(isOwner: Boolean, status: ListingStatus = ListingStatus.ACTIVE) = ListingDetailUiState.Content(
        listing = FurnitureListing(id = "l1", ownerId = "me", status = status, category = FurnitureCategory.SOFA),
        isOwner = isOwner,
        isFavourite = false,
        myRequest = null,
    )

    @Test
    fun owner_cannotRequestTheirOwnItem() {
        assertFalse(detail(isOwner = true).canRequestPurchase)
        assertTrue(detail(isOwner = false).canRequestPurchase)
    }

    @Test
    fun owner_canMarkSoldOnlyWhileTheListingIsOpen() {
        assertTrue(detail(isOwner = true, ListingStatus.ACTIVE).canMarkSold)
        assertTrue(detail(isOwner = true, ListingStatus.RESERVED).canMarkSold)
        assertFalse(detail(isOwner = true, ListingStatus.SOLD).canMarkSold)
        assertFalse(detail(isOwner = true, ListingStatus.UNAVAILABLE).canMarkSold)
    }

    @Test
    fun buyer_neverGetsOwnerActions() {
        assertFalse(detail(isOwner = false).canMarkSold)
    }
}

/** The Individual / Vendor filter runs as a Firestore query, so each query shape it can produce needs a composite index. */
class SellerTypeIndexTest {

    private fun listingIndexes(): List<List<String>> {
        // Unit tests run with the app module as working directory.
        val file = listOf(File("../firestore.indexes.json"), File("firestore.indexes.json")).first { it.exists() }
        val indexes = JSONObject(file.readText()).getJSONArray("indexes")
        return (0 until indexes.length()).map { indexes.getJSONObject(it) }
            .filter { it.getString("collectionGroup") == "listings" }
            .map { index ->
                val fields = index.getJSONArray("fields")
                (0 until fields.length()).map { fields.getJSONObject(it).getString("fieldPath") }
            }
    }

    @Test
    fun everyCategoryAndActionTypeCombination_withSellerType_hasAnIndex() {
        val indexes = listingIndexes()
        for (category in listOf(false, true)) {
            for (action in listOf(false, true)) {
                val equalityFields = buildSet {
                    addAll(listOf("city", "status", "sellerType"))
                    if (category) add("category")
                    if (action) add("actionType")
                }
                val found = indexes.any { it.last() == "createdAt" && it.dropLast(1).toSet() == equalityFields }
                assertTrue("missing listings index for $equalityFields + createdAt", found)
            }
        }
    }
}
