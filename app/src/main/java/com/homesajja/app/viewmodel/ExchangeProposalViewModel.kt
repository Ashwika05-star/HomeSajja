package com.homesajja.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homesajja.app.data.model.ExchangeRequest
import com.homesajja.app.data.model.ExchangeStatus
import com.homesajja.app.data.model.FurnitureListing
import com.homesajja.app.data.model.ListingActionType
import com.homesajja.app.data.model.ListingStatus
import com.homesajja.app.repository.AuthRepository
import com.homesajja.app.repository.ExchangeRepository
import com.homesajja.app.repository.ListingRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

const val MAX_EXCHANGE_MESSAGE_LENGTH = 300

/** The steps of proposing an exchange, in order. */
enum class ExchangeStep(val title: String) {
    OFFER("Your furniture"),
    WANT("What you want"),
    REVIEW("Review & send"),
}

/** The user's own active listings, from which they pick what they offer. */
sealed interface MyFurnitureState {
    data object Loading : MyFurnitureState
    data class Error(val message: String) : MyFurnitureState
    data class Content(val items: List<FurnitureListing>) : MyFurnitureState
}

/** Only used when the flow starts from a listing ("Propose exchange"): loading that listing. */
sealed interface WantedItemState {
    data object NotPreselected : WantedItemState
    data object Loading : WantedItemState
    data class Error(val message: String) : WantedItemState
    data object Ready : WantedItemState
}

sealed interface SendState {
    data object Idle : SendState
    data object Sending : SendState
    data class Sent(val requestId: String) : SendState
    data class Failed(val message: String) : SendState
}

/**
 * Drives "propose an exchange": pick your item, pick the item you want (or arrive with it
 * already chosen from a listing), then review, add a message and send. Browsing for the
 * wanted item is done by [ExchangeBrowseViewModel]; this one holds the choices and sends.
 */
class ExchangeProposalViewModel(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val listingRepository: ListingRepository,
    private val exchangeRepository: ExchangeRepository,
) : ViewModel() {

    private val preselectedId: String? = savedStateHandle["requestedListingId"]

    var step by mutableStateOf(ExchangeStep.OFFER)
        private set
    var myFurniture by mutableStateOf<MyFurnitureState>(MyFurnitureState.Loading)
        private set
    var wantedItem by mutableStateOf<WantedItemState>(
        if (preselectedId == null) WantedItemState.NotPreselected else WantedItemState.Loading,
    )
        private set
    var offered by mutableStateOf<FurnitureListing?>(null)
        private set
    var requested by mutableStateOf<FurnitureListing?>(null)
        private set
    var message by mutableStateOf("")
        private set
    var sendState by mutableStateOf<SendState>(SendState.Idle)
        private set

    private val refreshGate = RefreshGate()

    init {
        loadMyFurniture()
        if (preselectedId != null) loadPreselected(preselectedId)
    }

    /** Called whenever the screen resumes (e.g. after listing a new item); reloads only if the data is old. */
    fun refreshMyFurnitureIfStale() {
        if (refreshGate.isStale()) loadMyFurniture(showLoading = false)
    }

    fun loadMyFurniture(showLoading: Boolean = true) {
        val uid = authRepository.currentUserId
        if (uid == null) {
            myFurniture = MyFurnitureState.Error("Please log in to propose an exchange.")
            return
        }
        if (showLoading) myFurniture = MyFurnitureState.Loading
        refreshGate.markLoaded()
        viewModelScope.launch {
            try {
                val active = listingRepository.getListingsByOwner(uid).filter { it.status == ListingStatus.ACTIVE }
                myFurniture = MyFurnitureState.Content(active)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (showLoading || myFurniture !is MyFurnitureState.Content) {
                    myFurniture = MyFurnitureState.Error(mapError(e, "Couldn't load your furniture."))
                }
            } finally {
                refreshGate.markLoaded()
            }
        }
    }

    fun loadPreselected(listingId: String = checkNotNull(preselectedId)) {
        wantedItem = WantedItemState.Loading
        viewModelScope.launch {
            try {
                val listing = listingRepository.getListing(listingId)
                when {
                    listing == null -> wantedItem = WantedItemState.Error("This item is no longer available.")
                    listing.actionType != ListingActionType.EXCHANGE || listing.status != ListingStatus.ACTIVE ->
                        wantedItem = WantedItemState.Error("This item isn't open for exchange.")
                    listing.ownerId == authRepository.currentUserId ->
                        wantedItem = WantedItemState.Error("You can't exchange with yourself.")
                    else -> {
                        requested = listing
                        wantedItem = WantedItemState.Ready
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                wantedItem = WantedItemState.Error(mapError(e, "Couldn't load that item."))
            }
        }
    }

    fun selectOffered(listing: FurnitureListing) {
        offered = listing
        // Arriving from a listing means the wanted item is already chosen, so skip browsing.
        step = if (requested != null) ExchangeStep.REVIEW else ExchangeStep.WANT
    }

    fun selectRequested(listing: FurnitureListing) {
        requested = listing
        step = ExchangeStep.REVIEW
    }

    fun onMessageChange(value: String) {
        message = value.take(MAX_EXCHANGE_MESSAGE_LENGTH)
    }

    /** Steps back; returns false if already at the first step (the caller should leave the screen). */
    fun back(): Boolean {
        val target = when (step) {
            ExchangeStep.OFFER -> return false
            ExchangeStep.WANT -> ExchangeStep.OFFER
            ExchangeStep.REVIEW -> if (preselectedId != null) ExchangeStep.OFFER else ExchangeStep.WANT
        }
        sendState = SendState.Idle
        step = target
        return true
    }

    fun dismissSendError() {
        sendState = SendState.Idle
    }

    fun send() {
        val uid = authRepository.currentUserId ?: return
        val mine = offered ?: return
        val theirs = requested ?: return
        if (sendState is SendState.Sending) return
        sendState = SendState.Sending

        viewModelScope.launch {
            try {
                val alreadyProposed = exchangeRepository.getRequestsBySender(uid).any {
                    it.offeredListingId == mine.id && it.requestedListingId == theirs.id &&
                        it.status in LIVE_STATUSES
                }
                if (alreadyProposed) {
                    sendState = SendState.Failed("You've already proposed this swap.")
                    return@launch
                }
                val saved = exchangeRepository.createRequest(
                    ExchangeRequest(
                        offeredListingId = mine.id,
                        offeredTitle = mine.title,
                        offeredImageUrl = mine.images.firstOrNull(),
                        requestedListingId = theirs.id,
                        requestedTitle = theirs.title,
                        requestedImageUrl = theirs.images.firstOrNull(),
                        senderId = uid,
                        senderName = authRepository.currentUserDisplayName ?: authRepository.currentUserEmail ?: "Someone",
                        receiverId = theirs.ownerId,
                        receiverName = theirs.ownerName,
                        message = message.trim(),
                    ),
                )
                sendState = SendState.Sent(saved.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                sendState = SendState.Failed(mapError(e, "Couldn't send your request. Please try again."))
            }
        }
    }

    private companion object {
        val LIVE_STATUSES = setOf(ExchangeStatus.PENDING, ExchangeStatus.ACCEPTED)
    }
}
