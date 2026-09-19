package com.homesajja.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homesajja.app.data.model.RecyclingRequest
import com.homesajja.app.data.model.RecyclingStatus
import com.homesajja.app.repository.AuthRepository
import com.homesajja.app.repository.RecyclingRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface RecycleListUiState {
    data object Loading : RecycleListUiState
    data class Error(val message: String) : RecycleListUiState
    data class Content(val requests: List<RecyclingRequest>) : RecycleListUiState
}

/** Which recycling requests the list shows. */
enum class RecycleFilter(val label: String) {
    ALL("All"),
    ACTIVE("Active"),
    CLOSED("Closed");

    fun matches(status: RecyclingStatus): Boolean = when (this) {
        ALL -> true
        ACTIVE -> status in ACTIVE_STATUSES
        CLOSED -> status !in ACTIVE_STATUSES
    }

    private companion object {
        val ACTIVE_STATUSES = setOf(RecyclingStatus.REQUESTED, RecyclingStatus.ACCEPTED, RecyclingStatus.SCHEDULED)
    }
}

/**
 * A person's recycling requests: the ones they sent (users) or received (vendors). The same list logic
 * backs "My recycling requests" and the temporary vendor list, so [asVendor] picks the query.
 */
open class MyRecyclingViewModel(
    private val authRepository: AuthRepository,
    private val recyclingRepository: RecyclingRepository,
    private val asVendor: Boolean = false,
) : ViewModel() {

    private val _uiState = MutableStateFlow<RecycleListUiState>(RecycleListUiState.Loading)
    val uiState: StateFlow<RecycleListUiState> = _uiState

    var filter by mutableStateOf(RecycleFilter.ALL)
        private set

    private val refreshGate = RefreshGate()

    init {
        load(showLoading = true)
    }

    fun selectFilter(value: RecycleFilter) {
        filter = value
    }

    fun retry() = load(showLoading = true)

    /** Called whenever the screen resumes; reloads quietly only if the data is old. */
    fun refreshIfStale() {
        if (refreshGate.isStale()) load(showLoading = false)
    }

    private fun load(showLoading: Boolean) {
        val uid = authRepository.currentUserId
        if (uid == null) {
            _uiState.value = RecycleListUiState.Error("Please log in to see your recycling requests.")
            return
        }
        if (showLoading) _uiState.value = RecycleListUiState.Loading
        refreshGate.markLoaded()
        viewModelScope.launch {
            try {
                val requests = if (asVendor) recyclingRepository.getRequestsByVendor(uid) else recyclingRepository.getRequestsByUser(uid)
                _uiState.value = RecycleListUiState.Content(requests)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // A failed silent refresh keeps the old list instead of replacing it with an error.
                if (showLoading || _uiState.value !is RecycleListUiState.Content) {
                    _uiState.value = RecycleListUiState.Error(mapError(e, "Couldn't load recycling requests."))
                }
            } finally {
                refreshGate.markLoaded()
            }
        }
    }
}

/** The recycling requests addressed to a vendor. */
class VendorRecyclingViewModel(
    authRepository: AuthRepository,
    recyclingRepository: RecyclingRepository,
) : MyRecyclingViewModel(authRepository, recyclingRepository, asVendor = true)
