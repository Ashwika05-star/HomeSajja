package com.homesajja.app.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.homesajja.app.data.model.ItemRequest
import com.homesajja.app.data.model.ItemRequestStatus
import kotlinx.coroutines.tasks.await

private const val COLLECTION = "itemRequests"

/**
 * Repository for custom user furniture item requests (`itemRequests/{id}`).
 * Users post requests for items they want; vendors browse and accept them.
 */
class ItemRequestRepository(private val firestore: FirebaseFirestore) {

    private val requests = firestore.collection(COLLECTION)

    suspend fun createRequest(request: ItemRequest): ItemRequest {
        val ref = requests.document()
        val now = System.currentTimeMillis()
        val saved = request.copy(
            id = ref.id,
            status = ItemRequestStatus.OPEN,
            createdAt = now,
            updatedAt = now,
        )
        ref.set(saved).await()
        return saved
    }

    suspend fun getRequest(id: String): ItemRequest? = requests.document(id).getAs()

    /** All requests submitted by a specific user, newest first. */
    suspend fun getRequestsByUser(userId: String): List<ItemRequest> =
        requests.whereEqualTo("userId", userId)
            .get()
            .await()
            .documents
            .mapDocumentsSafe<ItemRequest>()
            .sortedByDescending { it.createdAt }

    /** All open item requests available for vendors to browse and accept, newest first. */
    suspend fun getAllOpenRequests(): List<ItemRequest> =
        requests.whereEqualTo("status", ItemRequestStatus.OPEN.name)
            .get()
            .await()
            .documents
            .mapDocumentsSafe<ItemRequest>()
            .sortedByDescending { it.createdAt }

    /** Requests that have been accepted by a specific vendor, newest first. */
    suspend fun getRequestsByVendor(vendorId: String): List<ItemRequest> =
        requests.whereEqualTo("acceptedVendorId", vendorId)
            .get()
            .await()
            .documents
            .mapDocumentsSafe<ItemRequest>()
            .sortedByDescending { it.createdAt }

    /** All requests (open, accepted, fulfilled) for vendor exploration, newest first. */
    suspend fun getAllRequests(): List<ItemRequest> =
        requests.get()
            .await()
            .documents
            .mapDocumentsSafe<ItemRequest>()
            .sortedByDescending { it.createdAt }

    /** Vendor accepts the user's item request. */
    suspend fun acceptRequest(id: String, vendorId: String, vendorName: String, note: String? = null) {
        val now = System.currentTimeMillis()
        val updates = mutableMapOf<String, Any>(
            "status" to ItemRequestStatus.ACCEPTED.name,
            "acceptedVendorId" to vendorId,
            "acceptedVendorName" to vendorName,
            "updatedAt" to now,
        )
        note?.takeIf { it.isNotBlank() }?.let { updates["vendorNote"] = it.trim() }
        requests.document(id).update(updates).await()
    }

    /** Either party marks the request as fulfilled once the item is delivered/received. */
    suspend fun fulfillRequest(id: String) {
        requests.document(id)
            .update(mapOf("status" to ItemRequestStatus.FULFILLED.name, "updatedAt" to System.currentTimeMillis()))
            .await()
    }

    /** User cancels their open request. */
    suspend fun cancelRequest(id: String) {
        requests.document(id)
            .update(mapOf("status" to ItemRequestStatus.CANCELLED.name, "updatedAt" to System.currentTimeMillis()))
            .await()
    }

    /** User deletes their request permanently. */
    suspend fun deleteRequest(id: String) {
        requests.document(id).delete().await()
    }
}
