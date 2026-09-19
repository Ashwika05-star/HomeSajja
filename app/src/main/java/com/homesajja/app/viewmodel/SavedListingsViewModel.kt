package com.homesajja.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homesajja.app.data.model.FurnitureListing
import com.homesajja.app.data.model.ListingStatus
import com.homesajja.app.repository.AuthRepository
import com.homesajja.app.repository.FavouriteRepository
import com.homesajja.app.repository.ListingRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** One saved listing. [listing] is null if it was deleted since it was saved. */
data class SavedItem(val listingId: String, val listing: FurnitureListing?) {
    /** A deleted listing, or one that is sold, exchanged, removed or hidden, can no longer be bought. */
    val isAvailable: Boolean
        get() = listing != null && listing.status in AVAILABLE_STATUSES

    private companion object {
        val AVAILABLE_STATUSES = setOf(ListingStatus.ACTIVE, ListingStatus.RESERVED)
    }
}

sealed interface SavedUiState {
    data object Loading : SavedUiState
    data class Error(val message: String) : SavedUiState
    data class Content(val items: List<SavedItem>) : SavedUiState
}

/** The person's saved furniture, most recently saved first, with unavailable ones marked instead of silently dropped. */
class SavedListingsViewModel(
    private val authRepository: AuthRepository,
    private val favouriteRepository: FavouriteRepository,
    private val listingRepository: ListingRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SavedUiState>(SavedUiState.Loading)
    val uiState: StateFlow<SavedUiState> = _uiState

    private val refreshGate = RefreshGate()

    init {
        load(showLoading = true)
    }

    fun retry() = load(showLoading = true)

    /** Called whenever the screen resumes (e.g. back from a listing); reloads quietly if the data is old. */
    fun refreshIfStale() {
        if (refreshGate.isStale()) load(showLoading = false)
    }

    /** Removes it from the list straight away, then from the saved items. */
    fun remove(listingId: String) {
        val uid = authRepository.currentUserId ?: return
        val content = _uiState.value as? SavedUiState.Content ?: return
        _uiState.value = SavedUiState.Content(content.items.filter { it.listingId != listingId })
        viewModelScope.launch {
            try {
                favouriteRepository.removeFavourite(uid, listingId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = content // it wasn't removed after all
            }
        }
    }

    private fun load(showLoading: Boolean) {
        val uid = authRepository.currentUserId
        if (uid == null) {
            _uiState.value = SavedUiState.Error("Please log in to see your saved furniture.")
            return
        }
        if (showLoading) _uiState.value = SavedUiState.Loading
        refreshGate.markLoaded()
        viewModelScope.launch {
            try {
                val favourites = favouriteRepository.getFavourites(uid, SAVED_LIMIT)
                val items = coroutineScope {
                    favourites.map { favourite -> async { SavedItem(favourite.listingId, listingRepository.getListing(favourite.listingId)) } }.awaitAll()
                }
                _uiState.value = SavedUiState.Content(items)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (showLoading || _uiState.value !is SavedUiState.Content) {
                    _uiState.value = SavedUiState.Error(mapError(e, "Couldn't load your saved furniture."))
                }
            } finally {
                refreshGate.markLoaded()
            }
        }
    }

    private companion object {
        const val SAVED_LIMIT = 100
    }
}
