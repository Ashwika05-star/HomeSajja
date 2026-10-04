package com.homesajja.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homesajja.app.data.model.CancelContext
import com.homesajja.app.data.model.CancelReason
import com.homesajja.app.payment.buildCancellation
import com.homesajja.app.payment.buildRefund
import com.homesajja.app.payment.cancelError
import com.homesajja.app.payment.refundRequired
import com.homesajja.app.payment.refundStatusFor
import com.homesajja.app.data.model.EntityType
import com.homesajja.app.data.model.ListingStatus
import com.homesajja.app.data.model.PaymentMethod
import com.homesajja.app.payment.paymentOpen
import com.homesajja.app.data.model.PurchaseRequest
import com.homesajja.app.data.model.PurchaseStatus
import com.homesajja.app.repository.AuthRepository
import com.homesajja.app.repository.NotificationSender
import com.homesajja.app.repository.NotificationTemplates
import com.homesajja.app.repository.PurchaseRequestRepository
import com.homesajja.app.repository.UserRepository
import com.homesajja.app.repository.VendorRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** A step the seller can take on a request they received. */
enum class SellerAction(val label: String, val resultingStatus: PurchaseStatus) {
    ACCEPT("Accept", PurchaseStatus.ACCEPTED),
    REJECT("Reject", PurchaseStatus.REJECTED),
    MARK_READY("Ready for pickup", PurchaseStatus.READY_FOR_PICKUP),
    COMPLETE("Mark completed", PurchaseStatus.COMPLETED),
    /** The seller backs out after accepting: opens the reason form (and the refund step) first. */
    CANCEL_ORDER("Cancel order", PurchaseStatus.CANCELLED_BY_VENDOR),
}

/** The actions offered to the seller for a request in [status]; the pipeline is
 * REQUESTED -> ACCEPTED -> READY_FOR_PICKUP -> COMPLETED, and the seller can back out of a request they accepted
 * (with a reason, and a refund first if they had confirmed payment). Finished requests offer nothing.
 * Accepting is the agreement: the price (the asking price, or the accepted offer) becomes the agreed amount. */
fun sellerActionsFor(status: PurchaseStatus): List<SellerAction> = when (status) {
    PurchaseStatus.REQUESTED -> listOf(SellerAction.ACCEPT, SellerAction.REJECT)
    PurchaseStatus.ACCEPTED -> listOf(SellerAction.MARK_READY, SellerAction.CANCEL_ORDER)
    PurchaseStatus.READY_FOR_PICKUP -> listOf(SellerAction.COMPLETE, SellerAction.CANCEL_ORDER)
    PurchaseStatus.COMPLETED, PurchaseStatus.REJECTED, PurchaseStatus.CANCELLED, PurchaseStatus.CANCELLED_BY_VENDOR -> emptyList()
}

/** The listing status each seller action should leave the item in, or null to leave it alone. */
private fun SellerAction.listingStatus(): ListingStatus? = when (this) {
    SellerAction.ACCEPT -> ListingStatus.RESERVED
    SellerAction.COMPLETE -> ListingStatus.SOLD
    SellerAction.REJECT, SellerAction.MARK_READY, SellerAction.CANCEL_ORDER -> null
}

sealed interface MyRequestsUiState {
    data object Loading : MyRequestsUiState
    data class Error(val message: String) : MyRequestsUiState

    /** [sent] = requests I made as a buyer; [received] = requests on my listings. */
    data class Content(
        val sent: List<PurchaseRequest>,
        val received: List<PurchaseRequest>,
    ) : MyRequestsUiState
}

class MyRequestsViewModel(
    private val authRepository: AuthRepository,
    private val purchaseRequestRepository: PurchaseRequestRepository,
    private val notificationSender: NotificationSender,
    private val vendorRepository: VendorRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<MyRequestsUiState>(MyRequestsUiState.Loading)
    val uiState: StateFlow<MyRequestsUiState> = _uiState

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages

    var busyRequestId by mutableStateOf<String?>(null)
        private set

    /** The signed-in person's saved UPI id (vendor or user), offered when they accept a request. */
    var myUpiId by mutableStateOf<String?>(null)
        private set

    private val refreshGate = RefreshGate()

    init {
        load(showLoading = true)
        loadMyUpiId()
    }

    private fun loadMyUpiId() {
        val uid = authRepository.currentUserId ?: return
        viewModelScope.launch {
            myUpiId = runCatching {
                vendorRepository.getVendorProfile(uid)?.upiId ?: userRepository.getUserProfile(uid)?.upiId
            }.getOrNull()
        }
    }

    fun retry() = load(showLoading = true)

    fun refresh() = load(showLoading = false)

    /** Called whenever the screen resumes; reloads only if the data is old. */
    fun refreshIfStale() {
        if (refreshGate.isStale()) refresh()
    }

    /** Buyers can withdraw a request only until the seller accepts it. */
    fun cancelRequest(request: PurchaseRequest) = runAction(request, "Couldn't cancel the request.") {
        purchaseRequestRepository.updateStatus(request.id, PurchaseStatus.CANCELLED)
        authRepository.currentUserId?.let { notificationSender.send(NotificationTemplates.purchaseStatus(request, PurchaseStatus.CANCELLED, it)) }
        _messages.tryEmit("Request cancelled.")
    }

    /** [upiId] is only meaningful when accepting: the id the buyer can pay to (null means cash only). */
    fun performSellerAction(request: PurchaseRequest, action: SellerAction, upiId: String? = null) =
        runAction(request, "Couldn't update the request.") {
            val listingStatus = action.listingStatus()
            when {
                // Accepting fixes the agreed amount and opens the payment record, and reserves the listing.
                action == SellerAction.ACCEPT -> purchaseRequestRepository.acceptRequest(request, upiId)
                listingStatus == null -> purchaseRequestRepository.updateStatus(request.id, action.resultingStatus)
                else -> purchaseRequestRepository.updateStatusAndListing(request.id, action.resultingStatus, request.listingId, listingStatus)
            }
            authRepository.currentUserId?.let {
                notificationSender.send(NotificationTemplates.purchaseStatus(request, action.resultingStatus, it))
            }
            _messages.tryEmit("Request marked as ${action.resultingStatus.displayName.lowercase()}.")
        }

    /**
     * The seller cancels an order they had accepted. A [reason] from the purchase list is required, the [note] is optional, and if they had confirmed
     * receiving payment they must say they refunded it ([refundDone]) first. The listing goes back on sale, and the buyer is told the reason.
     */
    fun cancelOrder(request: PurchaseRequest, reason: CancelReason, note: String, refundDone: Boolean) =
        runAction(request, "Couldn't cancel the order.") {
            val sellerId = authRepository.currentUserId ?: return@runAction
            if (sellerId != request.sellerId || SellerAction.CANCEL_ORDER !in sellerActionsFor(request.status)) return@runAction
            val problem = cancelError(reason, note, CancelContext.PURCHASE)
            if (problem != null) {
                _messages.tryEmit(problem)
                return@runAction
            }
            val needsRefund = refundRequired(request.payment, sellerId)
            if (needsRefund && !refundDone) {
                _messages.tryEmit("Mark the refund as done before cancelling.")
                return@runAction
            }
            val cancellation = buildCancellation(reason, note, sellerId)
            val refund = if (needsRefund) buildRefund(request.agreedAmount ?: request.offeredPrice) else null
            purchaseRequestRepository.cancelByVendor(request, cancellation, refund)
            notificationSender.send(
                NotificationTemplates.vendorCancelled(
                    request.buyerId, sellerId, request.sellerName.ifBlank { "The seller" }, "your order for ${request.listingTitle}", cancellation,
                    refundStatusFor(request.payment, refund), refund?.amount, EntityType.LISTING, request.listingId,
                ),
            )
            _messages.tryEmit("Order cancelled and the item is back on sale. ${request.buyerName} has been told why.")
        }

    /** The buyer says they paid, by UPI or in cash (the payment itself happened in a UPI app or in person). */
    fun markPaid(request: PurchaseRequest, method: PaymentMethod, upiRef: String?) = runAction(request, "Couldn't mark it as paid.") {
        val payment = request.payment ?: return@runAction
        if (!request.paymentOpen || authRepository.currentUserId != payment.payerId) return@runAction
        purchaseRequestRepository.markPaid(request.id, method, upiRef)
        notificationSender.send(
            NotificationTemplates.paymentMarked(payment, request.agreedAmount ?: request.offeredPrice, request.listingTitle, request.buyerName, EntityType.LISTING, request.listingId),
        )
        _messages.tryEmit("Marked as paid. ${request.sellerName.ifBlank { "The seller" }} will confirm it.")
    }

    /** The seller says the money arrived. Only this counts as money earned. */
    fun confirmPayment(request: PurchaseRequest) = runAction(request, "Couldn't confirm the payment.") {
        val payment = request.payment ?: return@runAction
        if (!request.paymentOpen || authRepository.currentUserId != payment.payeeId) return@runAction
        purchaseRequestRepository.confirmPayment(request.id)
        notificationSender.send(
            NotificationTemplates.paymentConfirmed(payment, request.agreedAmount ?: request.offeredPrice, request.listingTitle, request.sellerName.ifBlank { "The seller" }, EntityType.LISTING, request.listingId),
        )
        _messages.tryEmit("Payment confirmed. It now counts as earned.")
    }

    private fun load(showLoading: Boolean) {
        val uid = authRepository.currentUserId
        if (uid == null) {
            _uiState.value = MyRequestsUiState.Error("Please log in to see your requests.")
            return
        }
        if (showLoading) _uiState.value = MyRequestsUiState.Loading
        refreshGate.markLoaded()
        viewModelScope.launch {
            try {
                _uiState.value = MyRequestsUiState.Content(
                    sent = purchaseRequestRepository.getRequestsByBuyer(uid),
                    received = purchaseRequestRepository.getRequestsBySeller(uid),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (showLoading || _uiState.value !is MyRequestsUiState.Content) {
                    _uiState.value = MyRequestsUiState.Error(mapError(e, "Couldn't load your requests."))
                }
            } finally {
                refreshGate.markLoaded()
            }
        }
    }

    private fun runAction(request: PurchaseRequest, failure: String, block: suspend () -> Unit) {
        if (busyRequestId != null) return
        busyRequestId = request.id
        viewModelScope.launch {
            try {
                block()
                load(showLoading = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _messages.tryEmit(mapError(e, failure))
            } finally {
                busyRequestId = null
            }
        }
    }
}
