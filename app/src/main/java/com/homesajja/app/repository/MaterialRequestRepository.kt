package com.homesajja.app.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.homesajja.app.data.model.MaterialRequest
import com.homesajja.app.data.model.MaterialRequestStatus
import kotlinx.coroutines.tasks.await

private const val COLLECTION = "materialRequests"

/** Vendor material-sourcing board at `materialRequests/{id}`. */
class MaterialRequestRepository(firestore: FirebaseFirestore) {

    private val requests = firestore.collection(COLLECTION)

    suspend fun createRequest(request: MaterialRequest): MaterialRequest {
        val ref = requests.document()
        val saved = request.copy(id = ref.id)
        ref.set(saved).await()
        return saved
    }

    suspend fun getRequest(id: String): MaterialRequest? = requests.document(id).getAs()

    /** Open requests in a city — what users browse to see who needs materials. */
    suspend fun getOpenRequests(city: String, limit: Int = DEFAULT_PAGE_SIZE): List<MaterialRequest> =
        requests.whereEqualTo("city", city)
            .whereEqualTo("status", MaterialRequestStatus.OPEN.name)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .getAllAs()

    suspend fun getRequestsByVendor(vendorId: String, limit: Int = DEFAULT_PAGE_SIZE): List<MaterialRequest> =
        requests.whereEqualTo("vendorId", vendorId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .getAllAs()

    suspend fun updateRequest(request: MaterialRequest) {
        requests.document(request.id).set(request.copy(updatedAt = System.currentTimeMillis())).await()
    }

    suspend fun updateStatus(id: String, status: MaterialRequestStatus) {
        requests.document(id)
            .update(mapOf("status" to status.name, "updatedAt" to System.currentTimeMillis()))
            .await()
    }

    suspend fun deleteRequest(id: String) {
        requests.document(id).delete().await()
    }
}
