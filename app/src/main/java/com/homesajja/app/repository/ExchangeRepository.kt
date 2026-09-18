package com.homesajja.app.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.homesajja.app.data.model.ExchangeRequest
import com.homesajja.app.data.model.ExchangeStatus
import com.homesajja.app.data.model.FurnitureListing
import com.homesajja.app.data.model.ListingStatus
import kotlinx.coroutines.tasks.await

private const val REQUESTS = "exchangeRequests"
private const val LISTINGS = "listings"

/** Thrown when an exchange can't go ahead because one of its items is no longer available. */
class ItemUnavailableException(message: String) : Exception(message)

/**
 * Exchange requests at `exchangeRequests/{id}` — a system of its own, separate from
 * purchase requests. Accepting and completing also move the two listings in the
 * same atomic write, which firestore.rules allows because the write names the request.
 */
class ExchangeRepository(private val firestore: FirebaseFirestore) {

    private val requests = firestore.collection(REQUESTS)
    private val listings = firestore.collection(LISTINGS)

    suspend fun createRequest(request: ExchangeRequest): ExchangeRequest {
        val ref = requests.document()
        val saved = request.copy(id = ref.id)
        ref.set(saved).await()
        return saved
    }

    suspend fun getRequest(id: String): ExchangeRequest? = requests.document(id).getAs()

    /** Requests the user has sent (outgoing), newest first. */
    suspend fun getRequestsBySender(senderId: String, limit: Int = DEFAULT_PAGE_SIZE): List<ExchangeRequest> =
        requests.whereEqualTo("senderId", senderId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .getAllAs()

    /** Requests the user has received (incoming), newest first. */
    suspend fun getRequestsByReceiver(receiverId: String, limit: Int = DEFAULT_PAGE_SIZE): List<ExchangeRequest> =
        requests.whereEqualTo("receiverId", receiverId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .getAllAs()

    /**
     * Receiver accepts: the request becomes ACCEPTED and both listings RESERVED, linked to
     * the request. Runs as a transaction that first re-checks that the request is still
     * pending and both items are still active, so two people can't win the same item.
     */
    suspend fun acceptRequest(request: ExchangeRequest) {
        firestore.runTransaction { tx ->
            val requestRef = requests.document(request.id)
            val offeredRef = listings.document(request.offeredListingId)
            val requestedRef = listings.document(request.requestedListingId)

            val current = tx.get(requestRef).toObject(ExchangeRequest::class.java)
            val offered = tx.get(offeredRef).toObject(FurnitureListing::class.java)
            val requested = tx.get(requestedRef).toObject(FurnitureListing::class.java)

            if (current?.status != ExchangeStatus.PENDING) {
                throw ItemUnavailableException("This request is no longer pending.")
            }
            if (offered?.status != ListingStatus.ACTIVE || requested?.status != ListingStatus.ACTIVE) {
                throw ItemUnavailableException("One of the items is no longer available.")
            }

            val now = System.currentTimeMillis()
            tx.update(requestRef, mapOf("status" to ExchangeStatus.ACCEPTED.name, "updatedAt" to now))
            listOf(offeredRef, requestedRef).forEach { ref ->
                tx.update(
                    ref,
                    mapOf(
                        "status" to ListingStatus.RESERVED.name,
                        "exchangeRequestId" to request.id,
                        "updatedAt" to now,
                    ),
                )
            }
        }.await()
    }

    /** Receiver turns down a pending request. Nothing else changes: no item was reserved yet. */
    suspend fun declineRequest(id: String) = setStatus(id, ExchangeStatus.DECLINED)

    /** Sender withdraws a pending request. */
    suspend fun cancelRequest(id: String) = setStatus(id, ExchangeStatus.CANCELLED)

    /** Either party marks an accepted exchange as done: the request is COMPLETED and both listings EXCHANGED. */
    suspend fun completeRequest(request: ExchangeRequest) {
        val now = System.currentTimeMillis()
        firestore.batch()
            .update(requests.document(request.id), mapOf("status" to ExchangeStatus.COMPLETED.name, "updatedAt" to now))
            .apply {
                listOf(request.offeredListingId, request.requestedListingId).forEach { listingId ->
                    update(
                        listings.document(listingId),
                        mapOf(
                            "status" to ListingStatus.EXCHANGED.name,
                            "exchangeRequestId" to request.id,
                            "updatedAt" to now,
                        ),
                    )
                }
            }
            .commit()
            .await()
    }

    suspend fun deleteRequest(id: String) {
        requests.document(id).delete().await()
    }

    private suspend fun setStatus(id: String, status: ExchangeStatus) {
        requests.document(id)
            .update(mapOf("status" to status.name, "updatedAt" to System.currentTimeMillis()))
            .await()
    }
}
