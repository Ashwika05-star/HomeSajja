package com.homesajja.app.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.homesajja.app.data.model.Cancellation
import com.homesajja.app.data.model.PaymentMethod
import com.homesajja.app.data.model.Refund
import com.homesajja.app.data.model.Quote
import com.homesajja.app.data.model.RepairRequest
import com.homesajja.app.data.model.RepairStatus
import com.homesajja.app.payment.newPayment
import kotlinx.coroutines.tasks.await

private const val COLLECTION = "repairRequests"

/** Repair jobs at `repairRequests/{id}`, addressed to one chosen vendor. */
class RepairRepository(firestore: FirebaseFirestore) {

    private val requests = firestore.collection(COLLECTION)

    /** Reserves an id up front so photos can be uploaded into the request's own folder first. */
    fun newRequestId(): String = requests.document().id

    /** Saves [request] under its own id, or under a fresh one if it has none. */
    suspend fun createRequest(request: RepairRequest): RepairRequest {
        val ref = if (request.id.isEmpty()) requests.document() else requests.document(request.id)
        val saved = request.copy(id = ref.id)
        ref.set(saved).await()
        return saved
    }

    suspend fun getRequest(id: String): RepairRequest? = requests.document(id).getAs()

    suspend fun getRequestsByUser(userId: String, limit: Int = DEFAULT_PAGE_SIZE): List<RepairRequest> =
        requests.whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .getAllAs()

    suspend fun getRequestsByVendor(vendorId: String, limit: Int = DEFAULT_PAGE_SIZE): List<RepairRequest> =
        requests.whereEqualTo("vendorId", vendorId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .getAllAs()

    suspend fun updateStatus(id: String, status: RepairStatus) {
        requests.document(id)
            .update(mapOf("status" to status.name, "updatedAt" to System.currentTimeMillis()))
            .await()
    }

    /** The vendor's quote (REQUESTED -> QUOTED), or a revised one after the user declined (DECLINED -> QUOTED). */
    suspend fun sendQuote(id: String, quote: Quote) {
        requests.document(id)
            .update(mapOf("status" to RepairStatus.QUOTED.name, "quote" to quote, "updatedAt" to System.currentTimeMillis()))
            .await()
    }

    /** The user accepts the quote: the amount is agreed for good, and the payment record opens. */
    suspend fun acceptQuote(request: RepairRequest) {
        val quote = checkNotNull(request.quote) { "There is no quote to accept." }
        val now = System.currentTimeMillis()
        requests.document(request.id)
            .update(
                mapOf(
                    "status" to RepairStatus.AGREED.name,
                    "agreedAmount" to quote.amount,
                    "agreedAt" to now,
                    "payment" to newPayment(request.userId, request.vendorId, quote.payeeUpiId),
                    "updatedAt" to now,
                ),
            )
            .await()
    }

    /** The vendor cancels an agreed job with a reason; [refund] is set when they had confirmed receiving payment. */
    suspend fun cancelByVendor(id: String, cancellation: Cancellation, refund: Refund?) {
        val changes = mutableMapOf<String, Any>(
            "status" to RepairStatus.CANCELLED_BY_VENDOR.name,
            "cancellation" to cancellation,
            "updatedAt" to System.currentTimeMillis(),
        )
        refund?.let { changes["refund"] = it }
        requests.document(id).update(changes).await()
    }

    /** The payer (the customer) says they paid. */
    suspend fun markPaid(id: String, method: PaymentMethod, upiRef: String?) {
        requests.document(id).update(PaymentWrites.markPaid(method, upiRef, System.currentTimeMillis())).await()
    }

    /** The payee (the vendor) says the money arrived. */
    suspend fun confirmPayment(id: String) {
        requests.document(id).update(PaymentWrites.confirmReceived(System.currentTimeMillis())).await()
    }

    suspend fun deleteRequest(id: String) {
        requests.document(id).delete().await()
    }
}
