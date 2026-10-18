package com.homesajja.app.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.homesajja.app.data.model.ItemRequest
import com.homesajja.app.data.model.ItemRequestStatus
import com.homesajja.app.data.model.Payment
import com.homesajja.app.data.model.PaymentMethod
import com.homesajja.app.data.model.PaymentStatus
import com.homesajja.app.data.model.Quote
import kotlinx.coroutines.tasks.await

private const val COLLECTION = "itemRequests"

/**
 * Repository for custom user furniture item requests (`itemRequests/{id}`).
 * Users post requests for items they want; vendors browse, send quotes, agree on price,
 * update work progress, and receive payments.
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

    /** All open item requests available for vendors to browse and quote, newest first. */
    suspend fun getAllOpenRequests(): List<ItemRequest> =
        requests.whereEqualTo("status", ItemRequestStatus.OPEN.name)
            .get()
            .await()
            .documents
            .mapDocumentsSafe<ItemRequest>()
            .sortedByDescending { it.createdAt }

    /** Requests that have been quoted or accepted by a specific vendor, newest first. */
    suspend fun getRequestsByVendor(vendorId: String): List<ItemRequest> =
        requests.whereEqualTo("acceptedVendorId", vendorId)
            .get()
            .await()
            .documents
            .mapDocumentsSafe<ItemRequest>()
            .sortedByDescending { it.createdAt }

    /** All requests for vendor exploration, newest first. */
    suspend fun getAllRequests(): List<ItemRequest> =
        requests.get()
            .await()
            .documents
            .mapDocumentsSafe<ItemRequest>()
            .sortedByDescending { it.createdAt }

    /** Vendor sends a quote on an open item request. */
    suspend fun sendQuote(
        id: String,
        vendorId: String,
        vendorName: String,
        vendorPhone: String,
        quote: Quote,
    ) {
        val now = System.currentTimeMillis()
        val updates = mapOf(
            "status" to ItemRequestStatus.QUOTED.name,
            "quote" to quote,
            "acceptedVendorId" to vendorId,
            "acceptedVendorName" to vendorName,
            "vendorPhone" to vendorPhone,
            "updatedAt" to now,
        )
        requests.document(id).update(updates).await()
    }

    /** Customer accepts the vendor's quote, locking the price and opening the payment record. */
    suspend fun acceptQuote(
        id: String,
        userId: String,
        vendorId: String,
        agreedAmount: Long,
        vendorUpiId: String?,
    ) {
        val now = System.currentTimeMillis()
        val payment = Payment(
            payerId = userId,
            payeeId = vendorId,
            payeeUpiId = vendorUpiId,
            status = PaymentStatus.UNPAID,
        )
        val updates = mapOf(
            "status" to ItemRequestStatus.AGREED.name,
            "agreedAmount" to agreedAmount,
            "agreedAt" to now,
            "payment" to payment,
            "updatedAt" to now,
        )
        requests.document(id).update(updates).await()
    }

    /** Customer declines quote, returning request back to OPEN for other vendors or revised quotes. */
    suspend fun declineQuote(id: String) {
        val now = System.currentTimeMillis()
        val updates = mapOf<String, Any?>(
            "status" to ItemRequestStatus.OPEN.name,
            "quote" to null,
            "acceptedVendorId" to null,
            "acceptedVendorName" to null,
            "vendorPhone" to null,
            "updatedAt" to now,
        )
        requests.document(id).update(updates).await()
    }

    /** Vendor updates the production/fulfillment stage (IN_PROGRESS, READY, COMPLETED). */
    suspend fun updateStatus(id: String, status: ItemRequestStatus) {
        val now = System.currentTimeMillis()
        requests.document(id)
            .update(mapOf("status" to status.name, "updatedAt" to now))
            .await()
    }

    /** Customer marks that they have paid via UPI or Cash. */
    suspend fun markPaid(id: String, method: PaymentMethod, upiRef: String?) {
        val now = System.currentTimeMillis()
        requests.document(id).update(PaymentWrites.markPaid(method, upiRef, now)).await()
    }

    /** Vendor confirms receipt of payment. */
    suspend fun confirmPayment(id: String) {
        val now = System.currentTimeMillis()
        requests.document(id).update(PaymentWrites.confirmReceived(now)).await()
    }

    /** Vendor accepts the user's item request directly (legacy). */
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
            .update(mapOf("status" to ItemRequestStatus.COMPLETED.name, "updatedAt" to System.currentTimeMillis()))
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
