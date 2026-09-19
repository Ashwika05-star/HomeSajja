package com.homesajja.app.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.homesajja.app.data.model.ListingStatus
import com.homesajja.app.data.model.PurchaseRequest
import com.homesajja.app.data.model.PurchaseStatus
import kotlinx.coroutines.tasks.await

private const val COLLECTION = "purchaseRequests"

/** Marketplace buy requests at `purchaseRequests/{id}`. Mutation after creation
 * is status-only (see firestore.rules), so there is no full-document update. */
class PurchaseRequestRepository(private val firestore: FirebaseFirestore) {

    private val requests = firestore.collection(COLLECTION)

    suspend fun createRequest(request: PurchaseRequest): PurchaseRequest {
        val ref = requests.document()
        val saved = request.copy(id = ref.id)
        ref.set(saved).await()
        return saved
    }

    suspend fun getRequest(id: String): PurchaseRequest? = requests.document(id).getAs()

    /** Requests the user has sent as a buyer. */
    suspend fun getRequestsByBuyer(buyerId: String, limit: Int = DEFAULT_PAGE_SIZE): List<PurchaseRequest> =
        requests.whereEqualTo("buyerId", buyerId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .getAllAs()

    /** Requests the user has received as a seller. */
    suspend fun getRequestsBySeller(sellerId: String, limit: Int = DEFAULT_PAGE_SIZE): List<PurchaseRequest> =
        requests.whereEqualTo("sellerId", sellerId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .getAllAs()

    suspend fun updateStatus(id: String, status: PurchaseStatus) {
        requests.document(id)
            .update(mapOf("status" to status.name, "updatedAt" to System.currentTimeMillis()))
            .await()
    }

    /** Seller-side transition that also moves the listing (e.g. ACCEPTED reserves it,
     * COMPLETED marks it sold). One batch, so the two never disagree. */
    suspend fun updateStatusAndListing(
        id: String,
        status: PurchaseStatus,
        listingId: String,
        listingStatus: ListingStatus,
        upiId: String? = null,
    ) {
        val now = System.currentTimeMillis()
        val requestChanges = mutableMapOf<String, Any>("status" to status.name, "updatedAt" to now)
        upiId?.let { requestChanges["upiId"] = it }
        firestore.batch()
            .update(requests.document(id), requestChanges)
            .update(firestore.collection("listings").document(listingId), mapOf("status" to listingStatus.name, "updatedAt" to now))
            .commit()
            .await()
    }

    /** Either party records that the money has changed hands (outside HomeSajja). */
    suspend fun markPaid(id: String) {
        val now = System.currentTimeMillis()
        requests.document(id).update(mapOf("paid" to true, "paidAt" to now, "updatedAt" to now)).await()
    }

    suspend fun deleteRequest(id: String) {
        requests.document(id).delete().await()
    }
}
