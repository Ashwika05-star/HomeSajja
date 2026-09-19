package com.homesajja.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homesajja.app.data.model.FurnitureListing
import com.homesajja.app.data.model.ListingStatus
import com.homesajja.app.repository.AuthRepository
import com.homesajja.app.repository.ListingRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface MyListingsUiState {
    data object Loading : MyListingsUiState
    data class Error(val message: String) : MyListingsUiState

    /** [active] = for sale, reserved or temporarily unavailable; [closed] = sold, exchanged or removed. */
    data class Content(
        val active: List<FurnitureListing>,
        val closed: List<FurnitureListing>,
    ) : MyListingsUiState
}

private val ACTIVE_STATUSES = setOf(ListingStatus.ACTIVE, ListingStatus.RESERVED, ListingStatus.UNAVAILABLE)

class MyListingsViewModel(
    private val authRepository: AuthRepository,
    private val listingRepository: ListingRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<MyListingsUiState>(MyListingsUiState.Loading)
    val uiState: StateFlow<MyListingsUiState> = _uiState

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages

    /** Id of the listing an action is running on, so its buttons can be disabled. */
    var busyListingId by mutableStateOf<String?>(null)
        private set

    private val refreshGate = RefreshGate()

    init {
        load(showLoading = true)
    }

    fun retry() = load(showLoading = true)

    /** Silent reload, used when returning to the screen (e.g. after editing). */
    fun refresh() = load(showLoading = false)

    /** Called whenever the screen resumes; reloads only if the data is old. */
    fun refreshIfStale() {
        if (refreshGate.isStale()) refresh()
    }

    fun markAsSold(listing: FurnitureListing) = runAction(listing, "Couldn't mark it as sold.") {
        listingRepository.updateListingStatus(listing.id, ListingStatus.SOLD)
        _messages.tryEmit("Marked \"${listing.title}\" as sold.")
    }

    /** Vendors hide an item from browsing (ACTIVE -> UNAVAILABLE) and bring it back again. */
    fun toggleAvailability(listing: FurnitureListing) {
        val target = if (listing.status == ListingStatus.ACTIVE) ListingStatus.UNAVAILABLE else ListingStatus.ACTIVE
        runAction(listing, "Couldn't update the listing.") {
            listingRepository.updateListingStatus(listing.id, target)
            _messages.tryEmit(if (target == ListingStatus.ACTIVE) "\"${listing.title}\" is available again." else "\"${listing.title}\" is now unavailable.")
        }
    }

    fun delete(listing: FurnitureListing) = runAction(listing, "Couldn't delete the listing.") {
        listingRepository.deleteListing(listing.id)
        _messages.tryEmit("Deleted \"${listing.title}\".")
    }

    private fun load(showLoading: Boolean) {
        val uid = authRepository.currentUserId
        if (uid == null) {
            _uiState.value = MyListingsUiState.Error("Please log in to see your listings.")
            return
        }
        if (showLoading) _uiState.value = MyListingsUiState.Loading
        refreshGate.markLoaded()
        viewModelScope.launch {
            try {
                val all = listingRepository.getListingsByOwner(uid)
                _uiState.value = MyListingsUiState.Content(
                    active = all.filter { it.status in ACTIVE_STATUSES },
                    closed = all.filter { it.status !in ACTIVE_STATUSES },
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // A failed silent refresh keeps showing the old list instead of an error screen.
                if (showLoading || _uiState.value !is MyListingsUiState.Content) {
                    _uiState.value = MyListingsUiState.Error(mapError(e, "Couldn't load your listings."))
                }
            } finally {
                refreshGate.markLoaded()
            }
        }
    }

    private fun runAction(listing: FurnitureListing, failure: String, block: suspend () -> Unit) {
        if (busyListingId != null) return
        busyListingId = listing.id
        viewModelScope.launch {
            try {
                block()
                load(showLoading = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _messages.tryEmit(mapError(e, failure))
            } finally {
                busyListingId = null
            }
        }
    }
}
