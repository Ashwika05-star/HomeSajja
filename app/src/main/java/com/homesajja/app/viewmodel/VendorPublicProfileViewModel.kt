package com.homesajja.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homesajja.app.data.model.FurnitureListing
import com.homesajja.app.data.model.ListingStatus
import com.homesajja.app.data.model.VendorProfile
import com.homesajja.app.repository.AuthRepository
import com.homesajja.app.data.model.Review
import com.homesajja.app.repository.FavouriteRepository
import com.homesajja.app.repository.ListingRepository
import com.homesajja.app.repository.ReviewRepository
import com.homesajja.app.repository.VendorRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface VendorProfileUiState {
    data object Loading : VendorProfileUiState
    data class Error(val message: String) : VendorProfileUiState

    /** [catalogue] is the vendor's listings that are open for sale or exchange right now. */
    data class Content(
        val vendor: VendorProfile,
        val catalogue: List<FurnitureListing>,
        val reviews: List<Review>,
        val isOwner: Boolean,
    ) : VendorProfileUiState
}

/**
 * A vendor's public page: business info, map pin, brochure photos and listing catalogue.
 * Opened with a `vendorId` argument by users; with none (the vendor's own Profile tab) it shows the
 * signed-in vendor's own page.
 */
class VendorPublicProfileViewModel(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val vendorRepository: VendorRepository,
    private val listingRepository: ListingRepository,
    private val reviewRepository: ReviewRepository,
    favouriteRepository: FavouriteRepository,
) : ViewModel() {

    private val saved = SavedIds(authRepository, favouriteRepository, viewModelScope)

    /** The listings the person has saved, for the hearts on the catalogue cards. */
    val savedIds: Set<String> get() = saved.ids

    fun toggleSaved(listingId: String) = saved.toggle(listingId)

    private val myId: String? = authRepository.currentUserId
    private val vendorId: String? = savedStateHandle.get<String>("vendorId") ?: myId

    private val _uiState = MutableStateFlow<VendorProfileUiState>(VendorProfileUiState.Loading)
    val uiState: StateFlow<VendorProfileUiState> = _uiState

    private val refreshGate = RefreshGate()

    init {
        saved.load()
        load(showLoading = true)
    }

    fun retry() = load(showLoading = true)

    /** Called whenever the screen resumes (e.g. after editing the profile); reloads quietly if the data is old. */
    fun refreshIfStale() {
        if (refreshGate.isStale()) load(showLoading = false)
    }

    private fun load(showLoading: Boolean) {
        val id = vendorId
        if (id == null) {
            _uiState.value = VendorProfileUiState.Error("Please log in to see this profile.")
            return
        }
        if (showLoading) _uiState.value = VendorProfileUiState.Loading
        refreshGate.markLoaded()
        viewModelScope.launch {
            try {
                val (vendor, listings, reviews) = coroutineScope {
                    val profile = async { vendorRepository.getVendorProfile(id) }
                    val owned = async { listingRepository.getListingsByOwner(id) }
                    val received = async { reviewRepository.getReviewsForTarget(id) }
                    Triple(profile.await(), owned.await(), received.await())
                }
                if (vendor == null) {
                    _uiState.value = VendorProfileUiState.Error("This vendor profile no longer exists.")
                } else {
                    _uiState.value = VendorProfileUiState.Content(
                        vendor = vendor,
                        catalogue = listings.filter { it.status == ListingStatus.ACTIVE },
                        reviews = reviews,
                        isOwner = id == myId,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (showLoading || _uiState.value !is VendorProfileUiState.Content) {
                    _uiState.value = VendorProfileUiState.Error(mapError(e, "Couldn't load this vendor."))
                }
            } finally {
                refreshGate.markLoaded()
            }
        }
    }
}
