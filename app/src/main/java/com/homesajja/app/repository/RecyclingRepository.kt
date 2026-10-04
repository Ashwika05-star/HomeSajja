package com.homesajja.app.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.homesajja.app.data.model.Cancellation
import com.homesajja.app.data.model.PayDirection
import com.homesajja.app.data.model.PaymentMethod
import com.homesajja.app.data.model.Quote
import com.homesajja.app.data.model.Refund
import com.homesajja.app.data.model.RecyclingRequest
import com.homesajja.app.data.model.RecyclingStatus
import com.homesajja.app.payment.newPayment
import com.homesajja.app.payment.recyclingParties
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

    /** A recycler takes an unassigned pickup: it becomes theirs and moves straight to ACCEPTED (free), or to QUOTED when they attach a [quote]. */
    suspend fun claimPickup(id: String, vendorId: String, vendorName: String, quote: Quote? = null) {
        val changes = mutableMapOf<String, Any>(
            "vendorId" to vendorId,
            "vendorName" to vendorName,
            "status" to (if (quote == null) RecyclingStatus.ACCEPTED else RecyclingStatus.QUOTED).name,
            "updatedAt" to System.currentTimeMillis(),
        )
        quote?.let { changes["quote"] = it }
        requests.document(id).update(changes).await()
    }

    /** The recycler's quote (REQUESTED -> QUOTED), or a revised one after the customer declined (DECLINED -> QUOTED). */
    suspend fun sendQuote(id: String, quote: Quote) {
        requests.document(id)
            .update(mapOf("status" to RecyclingStatus.QUOTED.name, "quote" to quote, "updatedAt" to System.currentTimeMillis()))
            .await()
    }

    /**
     * The customer accepts the quote (QUOTED -> ACCEPTED): the amount is agreed for good and the payment record opens.
     * [customerUpiId] is only used when the recycler is the one paying, so the customer says where to send it.
     */
    suspend fun acceptQuote(request: RecyclingRequest, customerUpiId: String?) {
        val quote = checkNotNull(request.quote) { "There is no quote to accept." }
        val direction = checkNotNull(quote.direction) { "The quote has no payment direction." }
        val (payerId, payeeId) = recyclingParties(request, direction)
        val payeeUpiId = if (direction == PayDirection.USER_PAYS_VENDOR) quote.payeeUpiId else customerUpiId
        val now = System.currentTimeMillis()
        requests.document(request.id)
            .update(
                mapOf(
                    "status" to RecyclingStatus.ACCEPTED.name,
                    "agreedAmount" to quote.amount,
                    "agreedAt" to now,
                    "payment" to newPayment(payerId, payeeId, payeeUpiId),
                    "updatedAt" to now,
                ),
            )
            .await()
    }

    /** The recycler cancels an accepted job with a reason; [refund] is set when they had confirmed receiving payment. */
    suspend fun cancelByVendor(id: String, cancellation: Cancellation, refund: Refund?) {
        val changes = mutableMapOf<String, Any>(
            "status" to RecyclingStatus.CANCELLED_BY_VENDOR.name,
            "cancellation" to cancellation,
            "updatedAt" to System.currentTimeMillis(),
        )
        refund?.let { changes["refund"] = it }
        requests.document(id).update(changes).await()
    }

    /** The payer (whoever the quote says pays) records that they paid. */
    suspend fun markPaid(id: String, method: PaymentMethod, upiRef: String?) {
        requests.document(id).update(PaymentWrites.markPaid(method, upiRef, System.currentTimeMillis())).await()
    }

    /** The payee records that the money arrived. */
    suspend fun confirmPayment(id: String) {
        requests.document(id).update(PaymentWrites.confirmReceived(System.currentTimeMillis())).await()
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
