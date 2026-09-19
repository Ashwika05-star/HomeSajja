package com.homesajja.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homesajja.app.repository.AuthRepository
import com.homesajja.app.repository.ListingRepository
import com.homesajja.app.repository.VendorInboxRepository
import com.homesajja.app.repository.VendorRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface VendorDashboardUiState {
    data object Loading : VendorDashboardUiState
    data class Error(val message: String) : VendorDashboardUiState
    data class Content(val data: DashboardData) : VendorDashboardUiState
}

/** The vendor's home: shop name, verification badge, stat cards and the recent-activity feed. */
class VendorDashboardViewModel(
    private val authRepository: AuthRepository,
    private val vendorRepository: VendorRepository,
    private val listingRepository: ListingRepository,
    private val inboxRepository: VendorInboxRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<VendorDashboardUiState>(VendorDashboardUiState.Loading)
    val uiState: StateFlow<VendorDashboardUiState> = _uiState

    private val refreshGate = RefreshGate()

    init {
        load(showLoading = true)
    }

    fun retry() = load(showLoading = true)

    /** Called whenever the screen resumes; reloads quietly only if the data is old. */
    fun refreshIfStale() {
        if (refreshGate.isStale()) load(showLoading = false)
    }

    private fun load(showLoading: Boolean) {
        val uid = authRepository.currentUserId
        if (uid == null) {
            _uiState.value = VendorDashboardUiState.Error("Please log in to see your dashboard.")
            return
        }
        if (showLoading) _uiState.value = VendorDashboardUiState.Loading
        refreshGate.markLoaded()
        viewModelScope.launch {
            try {
                val data = coroutineScope {
                    val vendor = async { vendorRepository.getVendorProfile(uid) }
                    val listings = async { listingRepository.getListingsByOwner(uid) }
                    val inbox = async { inboxRepository.load(uid) }
                    val profile = vendor.await() ?: throw IllegalStateException("No vendor profile")
                    buildDashboard(profile, listings.await(), inbox.await())
                }
                _uiState.value = VendorDashboardUiState.Content(data)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // A failed silent refresh keeps the old numbers instead of replacing them with an error.
                if (showLoading || _uiState.value !is VendorDashboardUiState.Content) {
                    _uiState.value = VendorDashboardUiState.Error(mapError(e, "Couldn't load your dashboard."))
                }
            } finally {
                refreshGate.markLoaded()
            }
        }
    }
}
