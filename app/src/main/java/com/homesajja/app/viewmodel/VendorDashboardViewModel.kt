package com.homesajja.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homesajja.app.repository.AuthRepository
import com.homesajja.app.repository.StatsRepository
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

/** How long the dashboard's numbers are trusted when the screen comes back into view, to keep Firestore reads low. */
private const val DASHBOARD_STALE_MILLIS = 20_000L

/**
 * The vendor's home: total and monthly earnings, the last six months as a chart, completed and pending work by type, the average rating and
 * shortcuts. Everything is read with count and aggregation queries and one ordered query of confirmed payments per collection (see [StatsRepository]).
 */
class VendorDashboardViewModel(
    private val authRepository: AuthRepository,
    private val vendorRepository: VendorRepository,
    private val statsRepository: StatsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<VendorDashboardUiState>(VendorDashboardUiState.Loading)
    val uiState: StateFlow<VendorDashboardUiState> = _uiState

    private val refreshGate = RefreshGate(DASHBOARD_STALE_MILLIS)

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
                    val listings = async { statsRepository.activeListings(uid) }
                    val tasks = async { statsRepository.vendorTaskCounts(uid) }
                    val earned = async { statsRepository.vendorEarned(uid) }
                    val rating = async { statsRepository.rating(uid) }
                    val profile = vendor.await() ?: throw IllegalStateException("No vendor profile")
                    val batch = earned.await()
                    buildDashboard(profile, listings.await(), tasks.await(), batch.items, rating.await(), batch.isPartial)
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
