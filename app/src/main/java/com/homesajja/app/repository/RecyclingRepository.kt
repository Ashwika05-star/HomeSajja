package com.homesajja.app.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.homesajja.app.data.model.RecyclingRequest
import com.homesajja.app.data.model.RecyclingStatus
import kotlinx.coroutines.tasks.await

private const val COLLECTION = "recyclingRequests"

/** Recycling pickups at `recyclingRequests/{id}`, addressed to one chosen recycler. */
class RecyclingRepository(firestore: FirebaseFirestore) {

    private val requests = firestore.collection(COLLECTION)

    suspend fun createRequest(request: RecyclingRequest): RecyclingRequest {
        val ref = requests.document()
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

    suspend fun updateStatus(id: String, status: RecyclingStatus) {
        requests.document(id)
            .update(mapOf("status" to status.name, "updatedAt" to System.currentTimeMillis()))
            .await()
    }

    /** Vendor sets the pickup date (epoch millis); moves the request to PICKUP_SCHEDULED. */
    suspend fun schedulePickup(id: String, pickupDate: Long) {
        requests.document(id)
            .update(
                mapOf(
                    "pickupDate" to pickupDate,
                    "status" to RecyclingStatus.PICKUP_SCHEDULED.name,
                    "updatedAt" to System.currentTimeMillis(),
                ),
            )
            .await()
    }

    suspend fun deleteRequest(id: String) {
        requests.document(id).delete().await()
    }
}
