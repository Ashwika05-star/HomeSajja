package com.homesajja.app.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.homesajja.app.data.model.RecyclingRequest
import com.homesajja.app.data.model.RecyclingStatus
import kotlinx.coroutines.tasks.await

private const val COLLECTION = "recyclingRequests"

/** Recycling jobs at `recyclingRequests/{id}`: drop-offs addressed to one recycler, pickups unassigned until claimed. */
class RecyclingRepository(firestore: FirebaseFirestore) {

    private val requests = firestore.collection(COLLECTION)

    /** Reserves an id up front so photos can be uploaded into the request's own folder first. */
    fun newRequestId(): String = requests.document().id

    /** Saves [request] under its own id, or under a fresh one if it has none. */
    suspend fun createRequest(request: RecyclingRequest): RecyclingRequest {
        val ref = if (request.id.isEmpty()) requests.document() else requests.document(request.id)
        val saved = request.copy(id = ref.id)
        ref.set(saved).await()
        return saved
    }

    suspend fun getRequest(id: String): RecyclingRequest? = requests.document(id).getAs()

    suspend fun getRequestsByUser(userId: String, limit: Int = DEFAULT_PAGE_SIZE): List<RecyclingRequest> =
        requests.whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .getAllAs()

    suspend fun getRequestsByVendor(vendorId: String, limit: Int = DEFAULT_PAGE_SIZE): List<RecyclingRequest> =
        requests.whereEqualTo("vendorId", vendorId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .getAllAs()

    /** Pickup requests in [city] that no recycler has claimed yet. Sorted newest first here, not in the query, to avoid an index. */
    suspend fun getOpenPickups(city: String, limit: Int = DEFAULT_PAGE_SIZE): List<RecyclingRequest> =
        requests.whereEqualTo("city", city)
            .whereEqualTo("vendorId", null)
            .whereEqualTo("status", RecyclingStatus.REQUESTED.name)
            .limit(limit.toLong())
            .getAllAs<RecyclingRequest>()
            .sortedByDescending { it.createdAt }

    /** A recycler takes an unassigned pickup: it becomes theirs and moves straight to ACCEPTED. */
    suspend fun claimPickup(id: String, vendorId: String, vendorName: String) {
        requests.document(id)
            .update(
                mapOf(
                    "vendorId" to vendorId,
                    "vendorName" to vendorName,
                    "status" to RecyclingStatus.ACCEPTED.name,
                    "updatedAt" to System.currentTimeMillis(),
                ),
            )
            .await()
    }

    suspend fun updateStatus(id: String, status: RecyclingStatus) {
        requests.document(id)
            .update(mapOf("status" to status.name, "updatedAt" to System.currentTimeMillis()))
            .await()
    }

    suspend fun deleteRequest(id: String) {
        requests.document(id).delete().await()
    }
}
