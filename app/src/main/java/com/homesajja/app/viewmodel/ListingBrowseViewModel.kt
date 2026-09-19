package com.homesajja.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homesajja.app.data.model.FurnitureCategory
import com.homesajja.app.data.model.FurnitureListing
import com.homesajja.app.data.model.ListingActionType
import com.homesajja.app.data.model.ListingFilters
import com.homesajja.app.data.model.matchesSearch
import com.homesajja.app.repository.AuthRepository
import com.homesajja.app.repository.FavouriteRepository
import com.homesajja.app.repository.ListingRepository
import com.homesajja.app.repository.UserRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private const val PAGE_SIZE = 20
private const val SEARCH_FETCH_DELAY_MS = 300L

sealed interface BrowseUiState {
    data object Loading : BrowseUiState
    data class Error(val message: String) : BrowseUiState

    /** [listings] already has the search, filters and "not my own listing" rules applied.
     * An empty list means Empty (nothing in the city) or No Results (search/filters
     * hid everything) — the screen tells them apart using [ListingBrowseViewModel.hasActiveNarrowing]. */
    data class Content(
        val listings: List<FurnitureListing>,
        val isLoadingMore: Boolean = false,
        val endReached: Boolean = false,
        val loadMoreError: String? = null,
    ) : BrowseUiState
}

/**
 * Browse the listings of one kind ([actionType]) in the user's city.
 *
 * Firestore does the city + category query, newest first, one page (20) at a
 * time using a createdAt cursor. Search text and the filter sheet then narrow
 * the fetched listings on the device; if that leaves fewer than a page, more
 * pages are fetched until there are enough or the city runs out.
 *
 * Buy (Explore) and Exchange each subclass this with their own action type, so each
 * system keeps its own ViewModel and state while sharing the paging logic.
 */
abstract class ListingBrowseViewModel(
    authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val listingRepository: ListingRepository,
    favouriteRepository: FavouriteRepository,
    private val actionType: ListingActionType,
) : ViewModel() {

    private val saved = SavedIds(authRepository, favouriteRepository, viewModelScope)

    /** The listings the person has saved, for the hearts on the cards. */
    val savedIds: Set<String> get() = saved.ids

    fun toggleSaved(listingId: String) = saved.toggle(listingId)

    var query by mutableStateOf("")
        private set
    var category by mutableStateOf<FurnitureCategory?>(null)
        private set
    var filters by mutableStateOf(ListingFilters())
        private set
    var city by mutableStateOf("")
        private set
    var isRefreshing by mutableStateOf(false)
        private set

    private val _uiState = MutableStateFlow<BrowseUiState>(BrowseUiState.Loading)
    val uiState: StateFlow<BrowseUiState> = _uiState

    val hasActiveNarrowing: Boolean
        get() = query.isNotBlank() || category != null || filters.isActive

    private val myId = authRepository.currentUserId
    private var fetched = emptyList<FurnitureListing>()
    private var cursor: Long? = null
    private var endReached = false
    private var job: Job? = null

    init {
        saved.load()
        reload(showFullScreenLoading = true)
    }

    fun onQueryChange(value: String) {
        query = value
        refilter()
    }

    fun onFiltersChange(value: ListingFilters) {
        filters = value
        refilter()
    }

    /** Category is a server-side filter, so changing it starts over from page one. */
    fun onCategoryChange(value: FurnitureCategory?) {
        category = value
        reload(showFullScreenLoading = true)
    }

    fun clearNarrowing() {
        query = ""
        filters = ListingFilters()
        if (category != null) {
            category = null
            reload(showFullScreenLoading = true)
        } else {
            refilter()
        }
    }

    fun retry() = reload(showFullScreenLoading = true)

    fun refresh() = reload(showFullScreenLoading = false)

    fun loadMore() {
        val current = _uiState.value as? BrowseUiState.Content ?: return
        if (current.isLoadingMore || endReached) return
        _uiState.value = current.copy(isLoadingMore = true, loadMoreError = null)
        job = viewModelScope.launch {
            try {
                fetchUntil(current.listings.size + PAGE_SIZE)
                publish()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = current.copy(
                    isLoadingMore = false,
                    loadMoreError = mapError(e, "Couldn't load more listings."),
                )
            }
        }
    }

    private fun reload(showFullScreenLoading: Boolean) {
        job?.cancel()
        fetched = emptyList()
        cursor = null
        endReached = false
        if (showFullScreenLoading) _uiState.value = BrowseUiState.Loading else isRefreshing = true

        job = viewModelScope.launch {
            try {
                if (city.isBlank()) {
                    city = myId?.let { userRepository.getUserProfile(it)?.city }.orEmpty()
                }
                if (city.isBlank()) {
                    _uiState.value = BrowseUiState.Error("We couldn't find your city. Please log in again.")
                } else {
                    fetchUntil(PAGE_SIZE)
                    publish()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = BrowseUiState.Error(mapError(e, "Couldn't load listings."))
            } finally {
                isRefreshing = false
            }
        }
    }

    /** Re-applies search/filters to what is already fetched, then tops up if the result is short. */
    private fun refilter() {
        job?.cancel()
        if (_uiState.value !is BrowseUiState.Content) return
        publish()
        if (visible().size < PAGE_SIZE && !endReached) {
            job = viewModelScope.launch {
                delay(SEARCH_FETCH_DELAY_MS)
                loadMore()
            }
        }
    }

    private suspend fun fetchUntil(target: Int) {
        while (!endReached && visible().size < target) {
            val page = listingRepository.getListings(
                city = city,
                category = category,
                actionType = actionType,
                afterCreatedAt = cursor,
                limit = PAGE_SIZE,
            )
            fetched = (fetched + page).distinctBy { it.id }
            cursor = page.lastOrNull()?.createdAt ?: cursor
            if (page.size < PAGE_SIZE) endReached = true
        }
    }

    private fun visible(): List<FurnitureListing> = fetched.filter {
        it.ownerId != myId && filters.matches(it) && it.matchesSearch(query)
    }

    private fun publish() {
        _uiState.value = BrowseUiState.Content(listings = visible(), endReached = endReached)
    }
}
