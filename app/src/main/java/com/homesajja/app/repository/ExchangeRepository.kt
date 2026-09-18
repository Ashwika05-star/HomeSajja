package com.homesajja.app.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.homesajja.app.data.model.ExchangeRequest
import com.homesajja.app.data.model.ExchangeStatus
import kotlinx.coroutines.tasks.await

private const val COLLECTION = "exchangeRequests"

/** Exchange proposals at `exchangeRequests/{id}`; status-only updates after creation. */
class ExchangeRepository(firestore: FirebaseFirestore) {

    private val requests = firestore.collection(COLLECTION)

    suspend fun createRequest(request: ExchangeRequest): ExchangeRequest {
        val ref = requests.document()
        val saved = request.copy(id = ref.id)
        ref.set(saved).await()
        return saved
    }

    suspend fun getRequest(id: String): ExchangeRequest? = requests.document(id).getAs()

    /** Proposals the user has sent. */
    suspend fun getRequestsByRequester(requesterId: String, limit: Int = DEFAULT_PAGE_SIZE): List<ExchangeRequest> =
        requests.whereEqualTo("requesterId", requesterId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .getAllAs()

    /** Proposals made for the user's listings. */
    suspend fun getRequestsByOwner(ownerId: String, limit: Int = DEFAULT_PAGE_SIZE): List<ExchangeRequest> =
        requests.whereEqualTo("ownerId", ownerId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .getAllAs()

    suspend fun updateStatus(id: String, status: ExchangeStatus) {
        requests.document(id)
            .update(mapOf("status" to status.name, "updatedAt" to System.currentTimeMillis()))
            .await()
    }

    suspend fun deleteRequest(id: String) {
        requests.document(id).delete().await()
    }
}
