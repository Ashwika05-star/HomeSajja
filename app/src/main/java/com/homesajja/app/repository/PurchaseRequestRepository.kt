package com.homesajja.app.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.homesajja.app.data.model.PurchaseRequest
import com.homesajja.app.data.model.PurchaseStatus
import kotlinx.coroutines.tasks.await

private const val COLLECTION = "purchaseRequests"

/** Marketplace buy requests at `purchaseRequests/{id}`. Mutation after creation
 * is status-only (see firestore.rules), so there is no full-document update. */
class PurchaseRequestRepository(firestore: FirebaseFirestore) {

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

    suspend fun deleteRequest(id: String) {
        requests.document(id).delete().await()
    }
}
