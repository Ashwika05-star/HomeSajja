package com.homesajja.app.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.homesajja.app.data.model.FurnitureCategory
import com.homesajja.app.data.model.FurnitureListing
import com.homesajja.app.data.model.ListingActionType
import com.homesajja.app.data.model.ListingStatus
import kotlinx.coroutines.tasks.await

private const val COLLECTION = "listings"

/** Marketplace listings at `listings/{id}` (Buy/Sell and Exchange listings). */
class ListingRepository(firestore: FirebaseFirestore) {

    private val listings = firestore.collection(COLLECTION)

    suspend fun createListing(listing: FurnitureListing): FurnitureListing {
        val ref = listings.document()
        val saved = listing.copy(id = ref.id)
        ref.set(saved).await()
        return saved
    }

    suspend fun getListing(id: String): FurnitureListing? = listings.document(id).getAs()

    /** City-scoped discovery of ACTIVE listings, newest first. Needs the
     * composite indexes declared in firestore.indexes.json. */
    suspend fun getListings(
        city: String,
        category: FurnitureCategory? = null,
        actionType: ListingActionType? = null,
        limit: Int = DEFAULT_PAGE_SIZE,
    ): List<FurnitureListing> {
        var query: Query = listings
            .whereEqualTo("city", city)
            .whereEqualTo("status", ListingStatus.ACTIVE.name)
        category?.let { query = query.whereEqualTo("category", it.name) }
        actionType?.let { query = query.whereEqualTo("actionType", it.name) }
        return query.orderBy("createdAt", Query.Direction.DESCENDING).limit(limit.toLong()).getAllAs()
    }

    /** Everything one owner has listed, in any status — for their "My listings" view. */
    suspend fun getListingsByOwner(ownerId: String, limit: Int = DEFAULT_PAGE_SIZE): List<FurnitureListing> =
        listings.whereEqualTo("ownerId", ownerId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .getAllAs()

    suspend fun updateListing(listing: FurnitureListing) {
        listings.document(listing.id).set(listing.copy(updatedAt = System.currentTimeMillis())).await()
    }

    suspend fun updateListingStatus(id: String, status: ListingStatus) {
        listings.document(id)
            .update(mapOf("status" to status.name, "updatedAt" to System.currentTimeMillis()))
            .await()
    }

    suspend fun deleteListing(id: String) {
        listings.document(id).delete().await()
    }
}
