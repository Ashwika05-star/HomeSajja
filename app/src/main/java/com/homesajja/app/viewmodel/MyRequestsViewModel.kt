package com.homesajja.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homesajja.app.data.model.ListingStatus
import com.homesajja.app.data.model.PurchaseRequest
import com.homesajja.app.data.model.PurchaseStatus
import com.homesajja.app.repository.AuthRepository
import com.homesajja.app.repository.NotificationSender
import com.homesajja.app.repository.NotificationTemplates
import com.homesajja.app.repository.PurchaseRequestRepository
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
    CANCEL("Cancel", PurchaseStatus.CANCELLED),
}

/** The actions offered to the seller for a request in [status]; the pipeline is
 * REQUESTED -> ACCEPTED -> READY_FOR_PICKUP -> COMPLETED, and the seller can back out
 * of a request they accepted. Finished requests offer nothing. */
fun sellerActionsFor(status: PurchaseStatus): List<SellerAction> = when (status) {
    PurchaseStatus.REQUESTED -> listOf(SellerAction.ACCEPT, SellerAction.REJECT)
    PurchaseStatus.ACCEPTED -> listOf(SellerAction.MARK_READY, SellerAction.CANCEL)
    PurchaseStatus.READY_FOR_PICKUP -> listOf(SellerAction.COMPLETE, SellerAction.CANCEL)
    PurchaseStatus.COMPLETED, PurchaseStatus.REJECTED, PurchaseStatus.CANCELLED -> emptyList()
}

/** The listing status each seller action should leave the item in, or null to leave it alone. */
private fun SellerAction.listingStatus(): ListingStatus? = when (this) {
    SellerAction.ACCEPT -> ListingStatus.RESERVED
    SellerAction.COMPLETE -> ListingStatus.SOLD
    SellerAction.CANCEL -> ListingStatus.ACTIVE
    SellerAction.REJECT, SellerAction.MARK_READY -> null
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
) : ViewModel() {

    private val _uiState = MutableStateFlow<MyRequestsUiState>(MyRequestsUiState.Loading)
    val uiState: StateFlow<MyRequestsUiState> = _uiState

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages

    var busyRequestId by mutableStateOf<String?>(null)
        private set

    private val refreshGate = RefreshGate()

    init {
        load(showLoading = true)
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

    /** [upiId] is only meaningful when accepting: the id the buyer can pay to (null means "pay in person"). */
    fun performSellerAction(request: PurchaseRequest, action: SellerAction, upiId: String? = null) =
        runAction(request, "Couldn't update the request.") {
            val listingStatus = action.listingStatus()
            if (listingStatus == null) {
                purchaseRequestRepository.updateStatus(request.id, action.resultingStatus)
            } else {
                purchaseRequestRepository.updateStatusAndListing(
                    request.id, action.resultingStatus, request.listingId, listingStatus,
                    upiId = upiId.takeIf { action == SellerAction.ACCEPT },
                )
            }
            authRepository.currentUserId?.let {
                notificationSender.send(NotificationTemplates.purchaseStatus(request, action.resultingStatus, it))
            }
            _messages.tryEmit("Request marked as ${action.resultingStatus.displayName.lowercase()}.")
        }

    /** Records that the money has changed hands. Payment itself happened in GPay or in person. */
    fun markPaid(request: PurchaseRequest) = runAction(request, "Couldn't mark it as paid.") {
        purchaseRequestRepository.markPaid(request.id)
        authRepository.currentUserId?.let { notificationSender.send(NotificationTemplates.purchasePaid(request, it)) }
        _messages.tryEmit("Marked as paid.")
    }

    /** Shows a message from the screen, e.g. when GPay isn't installed. */
    fun say(message: String) {
        _messages.tryEmit(message)
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
