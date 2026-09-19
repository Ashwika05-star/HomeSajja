package com.homesajja.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homesajja.app.data.model.ListingStatus
import com.homesajja.app.data.model.MaterialOffer
import com.homesajja.app.data.model.MaterialRequest
import com.homesajja.app.data.model.MaterialRequestStatus
import com.homesajja.app.data.model.OfferStatus
import com.homesajja.app.data.model.FurnitureListing
import com.homesajja.app.repository.AuthRepository
import com.homesajja.app.repository.ListingRepository
import com.homesajja.app.repository.MaterialRequestRepository
import com.homesajja.app.repository.NotificationSender
import com.homesajja.app.repository.NotificationTemplates
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface MaterialDetailUiState {
    data object Loading : MaterialDetailUiState
    data class Error(val message: String) : MaterialDetailUiState

    /**
     * [offers] is filled only for the request's own vendor (everyone's offers); [myOffer] only for someone else
     * (their own offer, if any).
     */
    data class Content(
        val request: MaterialRequest,
        val isOwner: Boolean,
        val offers: List<MaterialOffer>,
        val myOffer: MaterialOffer?,
        val isBusy: Boolean = false,
    ) : MaterialDetailUiState
}

/** State of the "pick a listing to offer" chooser. */
sealed interface OfferChooserState {
    data object Closed : OfferChooserState
    data object Loading : OfferChooserState
    data class Error(val message: String) : OfferChooserState
    data class Choosing(val listings: List<FurnitureListing>) : OfferChooserState
}

/**
 * One material request. The vendor who posted it sees the offers and accepts or declines them, and
 * can mark it fulfilled, close it or delete it. Anyone else sees the request and can offer one of their
 * own listings against it (one offer each).
 */
class MaterialRequestDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val materialRepository: MaterialRequestRepository,
    private val listingRepository: ListingRepository,
    private val notificationSender: NotificationSender,
) : ViewModel() {

    private val requestId: String = checkNotNull(savedStateHandle["requestId"])
    private val myId: String? = authRepository.currentUserId

    private val _uiState = MutableStateFlow<MaterialDetailUiState>(MaterialDetailUiState.Loading)
    val uiState: StateFlow<MaterialDetailUiState> = _uiState

    private val _chooser = MutableStateFlow<OfferChooserState>(OfferChooserState.Closed)
    val chooser: StateFlow<OfferChooserState> = _chooser

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages

    /** Emits once when the request has been deleted, so the screen can close. */
    private val _deleted = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val deleted: SharedFlow<Unit> = _deleted

    init {
        load(showLoading = true)
    }

    fun retry() = load(showLoading = true)

    // ---- vendor actions ----

    fun decideOffer(offer: MaterialOffer, status: OfferStatus) = act("Couldn't update the offer.") {
        materialRepository.updateOfferStatus(requestId, offer.offererId, status)
        (_uiState.value as? MaterialDetailUiState.Content)?.request?.let {
            notificationSender.send(NotificationTemplates.offerDecision(it, offer, accepted = status == OfferStatus.ACCEPTED))
        }
        _messages.tryEmit("Offer ${status.displayName.lowercase()}.")
    }

    fun setRequestStatus(status: MaterialRequestStatus) = act("Couldn't update the request.") {
        materialRepository.updateStatus(requestId, status)
        _messages.tryEmit("Request marked as ${status.displayName.lowercase()}.")
    }

    fun deleteRequest() = act("Couldn't delete the request.") {
        materialRepository.deleteRequest(requestId)
        _deleted.tryEmit(Unit)
    }

    // ---- user actions ----

    fun openChooser() {
        val uid = myId ?: return
        _chooser.value = OfferChooserState.Loading
        viewModelScope.launch {
            _chooser.value = try {
                OfferChooserState.Choosing(listingRepository.getListingsByOwner(uid).filter { it.status == ListingStatus.ACTIVE })
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                OfferChooserState.Error(mapError(e, "Couldn't load your listings."))
            }
        }
    }

    fun closeChooser() {
        _chooser.value = OfferChooserState.Closed
    }

    fun offer(listing: FurnitureListing) {
        val uid = myId ?: return
        _chooser.value = OfferChooserState.Closed
        act("Couldn't send your offer.") {
            val saved = materialRepository.createOffer(
                MaterialOffer(
                    requestId = requestId,
                    offererId = uid,
                    offererName = authRepository.currentUserDisplayName ?: authRepository.currentUserEmail ?: "Someone",
                    listingId = listing.id,
                    listingTitle = listing.title,
                    listingImage = listing.images.firstOrNull(),
                ),
            )
            (_uiState.value as? MaterialDetailUiState.Content)?.request?.let {
                notificationSender.send(NotificationTemplates.materialOffer(it, saved))
            }
            _messages.tryEmit("Offer sent.")
        }
    }

    fun withdrawOffer() {
        val uid = myId ?: return
        act("Couldn't withdraw your offer.") {
            materialRepository.withdrawOffer(requestId, uid)
            _messages.tryEmit("Offer withdrawn.")
        }
    }

    private fun act(failure: String, block: suspend () -> Unit) {
        val content = _uiState.value as? MaterialDetailUiState.Content ?: return
        if (content.isBusy) return
        _uiState.value = content.copy(isBusy = true)
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _messages.tryEmit(mapError(e, failure))
            } finally {
                // Show what is really stored now, whether the action worked or not.
                load(showLoading = false)
            }
        }
    }

    private fun load(showLoading: Boolean) {
        if (showLoading) _uiState.value = MaterialDetailUiState.Loading
        viewModelScope.launch {
            try {
                val request = materialRepository.getRequest(requestId)
                if (request == null) {
                    // After the vendor deletes it, the screen is already closing; otherwise it really is gone.
                    _uiState.value = MaterialDetailUiState.Error("This request no longer exists.")
                    return@launch
                }
                val isOwner = myId != null && myId == request.vendorId
                _uiState.value = MaterialDetailUiState.Content(
                    request = request,
                    isOwner = isOwner,
                    offers = if (isOwner) materialRepository.getOffers(requestId) else emptyList(),
                    myOffer = if (!isOwner && myId != null) materialRepository.getOffer(requestId, myId) else null,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (showLoading || _uiState.value !is MaterialDetailUiState.Content) {
                    _uiState.value = MaterialDetailUiState.Error(mapError(e, "Couldn't load this request."))
                } else {
                    (_uiState.value as? MaterialDetailUiState.Content)?.let { _uiState.value = it.copy(isBusy = false) }
                }
            }
        }
    }
}
