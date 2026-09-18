package com.homesajja.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homesajja.app.data.model.EntityType
import com.homesajja.app.data.model.FurnitureListing
import com.homesajja.app.data.model.ListingActionType
import com.homesajja.app.data.model.ListingStatus
import com.homesajja.app.data.model.PurchaseRequest
import com.homesajja.app.data.model.PurchaseStatus
import com.homesajja.app.repository.AuthRepository
import com.homesajja.app.repository.ChatRepository
import com.homesajja.app.repository.FavouriteRepository
import com.homesajja.app.repository.ListingRepository
import com.homesajja.app.repository.PurchaseRequestRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface ListingDetailUiState {
    data object Loading : ListingDetailUiState
    data class Error(val message: String) : ListingDetailUiState

    /** [myRequest] is this buyer's latest purchase request for the listing, if any. */
    data class Content(
        val listing: FurnitureListing,
        val isOwner: Boolean,
        val isFavourite: Boolean,
        val myRequest: PurchaseRequest?,
        val isBusy: Boolean = false,
    ) : ListingDetailUiState {
        /** A buyer can send a request while a for-sale listing is open and they have no live request.
         * Exchange listings are never bought — they get exchange proposals instead. */
        val canRequestPurchase: Boolean
            get() = !isOwner && listing.actionType == ListingActionType.SELL && listing.status == ListingStatus.ACTIVE &&
                (myRequest == null || myRequest.status in ENDED_STATUSES)
    }
}

private val ENDED_STATUSES = setOf(PurchaseStatus.REJECTED, PurchaseStatus.CANCELLED)

/** Checks a "Make offer" amount; returns an error message, or null if it is fine. */
fun validateOffer(amountText: String, askingPrice: Long): String? {
    val amount = amountText.trim().toLongOrNull() ?: return "Enter a valid amount"
    return when {
        amount <= 0 -> "Enter a valid amount"
        amount > askingPrice -> "An offer can't be higher than the asking price"
        else -> null
    }
}

class ListingDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val listingRepository: ListingRepository,
    private val favouriteRepository: FavouriteRepository,
    private val chatRepository: ChatRepository,
    private val purchaseRequestRepository: PurchaseRequestRepository,
) : ViewModel() {

    private val listingId: String = checkNotNull(savedStateHandle["listingId"])
    private val myId: String? = authRepository.currentUserId
    private val myName: String
        get() = authRepository.currentUserDisplayName ?: authRepository.currentUserEmail ?: "Buyer"

    private val _uiState = MutableStateFlow<ListingDetailUiState>(ListingDetailUiState.Loading)
    val uiState: StateFlow<ListingDetailUiState> = _uiState

    /** One-off messages for a snackbar (confirmations and action failures). */
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages

    init {
        load()
    }

    fun load() {
        _uiState.value = ListingDetailUiState.Loading
        viewModelScope.launch {
            try {
                val listing = listingRepository.getListing(listingId)
                if (listing == null) {
                    _uiState.value = ListingDetailUiState.Error("This listing is no longer available.")
                    return@launch
                }
                val uid = myId.orEmpty()
                val isOwner = listing.ownerId == uid
                _uiState.value = ListingDetailUiState.Content(
                    listing = listing,
                    isOwner = isOwner,
                    isFavourite = !isOwner && favouriteRepository.isFavourite(uid, listingId),
                    myRequest = if (isOwner) null else latestRequestFor(uid),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = ListingDetailUiState.Error(mapError(e, "Couldn't load this listing."))
            }
        }
    }

    /** "Buy": a request at the full asking price. */
    fun buy() {
        val content = content() ?: return
        sendRequest(content, content.listing.price, "Buy request sent to ${content.listing.ownerName}.")
    }

    /** "Make offer": a request at a lower price the seller can accept or reject. */
    fun makeOffer(amount: Long) {
        val content = content() ?: return
        sendRequest(content, amount, "Offer of ₹$amount sent to ${content.listing.ownerName}.")
    }

    /** Writes the chat document (or finds the existing one); the chat screen itself comes in a later phase. */
    fun chatWithSeller() {
        val content = content() ?: return
        val uid = myId ?: return
        runBusy(content, failure = "Couldn't start the chat.") {
            chatRepository.getOrCreateChat(
                contextType = EntityType.LISTING,
                contextId = content.listing.id,
                contextTitle = content.listing.title,
                participantNames = mapOf(uid to myName, content.listing.ownerId to content.listing.ownerName),
            )
            _messages.tryEmit("Chat started with ${content.listing.ownerName}.")
        }
    }

    fun toggleFavourite() {
        val content = content() ?: return
        val uid = myId ?: return
        runBusy(content, failure = "Couldn't update your saved items.") {
            if (content.isFavourite) {
                favouriteRepository.removeFavourite(uid, listingId)
                _messages.tryEmit("Removed from saved items.")
            } else {
                favouriteRepository.addFavourite(uid, listingId)
                _messages.tryEmit("Saved to your favourites.")
            }
            update { it.copy(isFavourite = !content.isFavourite) }
        }
    }

    private fun sendRequest(content: ListingDetailUiState.Content, price: Long, confirmation: String) {
        val uid = myId ?: return
        if (!content.canRequestPurchase) {
            _messages.tryEmit("You already have an active request for this item.")
            return
        }
        runBusy(content, failure = "Couldn't send your request.") {
            val request = purchaseRequestRepository.createRequest(
                PurchaseRequest(
                    listingId = content.listing.id,
                    listingTitle = content.listing.title,
                    buyerId = uid,
                    buyerName = myName,
                    sellerId = content.listing.ownerId,
                    offeredPrice = price,
                ),
            )
            update { it.copy(myRequest = request) }
            _messages.tryEmit(confirmation)
        }
    }

    private suspend fun latestRequestFor(uid: String): PurchaseRequest? =
        purchaseRequestRepository.getRequestsByBuyer(uid)
            .filter { it.listingId == listingId }
            .maxByOrNull { it.createdAt }

    private fun content() = (_uiState.value as? ListingDetailUiState.Content)?.takeIf { !it.isBusy }

    private fun update(transform: (ListingDetailUiState.Content) -> ListingDetailUiState.Content) {
        (_uiState.value as? ListingDetailUiState.Content)?.let { _uiState.value = transform(it) }
    }

    /** Runs [block] with the buttons disabled, and reports [failure] if it throws. */
    private fun runBusy(
        content: ListingDetailUiState.Content,
        failure: String,
        block: suspend () -> Unit,
    ) {
        _uiState.value = content.copy(isBusy = true)
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _messages.tryEmit(mapError(e, failure))
            } finally {
                update { it.copy(isBusy = false) }
            }
        }
    }
}
