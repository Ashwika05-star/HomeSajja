package com.homesajja.app.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.homesajja.app.data.model.MaterialOffer
import com.homesajja.app.data.model.MaterialRequest
import com.homesajja.app.data.model.MaterialRequestStatus
import com.homesajja.app.data.model.OfferStatus
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

    // ---- offers: `materialRequests/{requestId}/offers/{offererId}` ----

    private fun offers(requestId: String) = requests.document(requestId).collection("offers")

    /** The offer id is the offerer's uid, so offering again replaces nothing: rules only allow one. */
    suspend fun createOffer(offer: MaterialOffer): MaterialOffer {
        val saved = offer.copy(id = offer.offererId)
        offers(offer.requestId).document(saved.id).set(saved).await()
        return saved
    }

    /** This person's own offer on the request, or null if they haven't made one. */
    suspend fun getOffer(requestId: String, offererId: String): MaterialOffer? =
        offers(requestId).document(offererId).getAs()

    /** Every offer on the request; only the request's vendor may read them all. */
    suspend fun getOffers(requestId: String): List<MaterialOffer> =
        offers(requestId).orderBy("createdAt", Query.Direction.DESCENDING).getAllAs()

    suspend fun updateOfferStatus(requestId: String, offererId: String, status: OfferStatus) {
        offers(requestId).document(offererId)
            .update(mapOf("status" to status.name, "updatedAt" to System.currentTimeMillis()))
            .await()
    }

    suspend fun withdrawOffer(requestId: String, offererId: String) {
        offers(requestId).document(offererId).delete().await()
    }
}
