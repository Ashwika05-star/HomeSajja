package com.homesajja.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homesajja.app.data.model.RepairRequest
import com.homesajja.app.data.model.RepairStatus
import com.homesajja.app.repository.AuthRepository
import com.homesajja.app.repository.RepairRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface RepairListUiState {
    data object Loading : RepairListUiState
    data class Error(val message: String) : RepairListUiState
    data class Content(val requests: List<RepairRequest>) : RepairListUiState
}

/** Which requests the list shows. */
enum class RepairFilter(val label: String) {
    ALL("All"),
    ACTIVE("Active"),
    CLOSED("Closed");

    fun matches(status: RepairStatus): Boolean = when (this) {
        ALL -> true
        ACTIVE -> status in ACTIVE_STATUSES
        CLOSED -> status !in ACTIVE_STATUSES
    }

    private companion object {
        val ACTIVE_STATUSES = setOf(
            RepairStatus.REQUESTED, RepairStatus.ACCEPTED, RepairStatus.IN_PROGRESS, RepairStatus.READY,
        )
    }
}

/**
 * A person's repair requests: the ones they sent (users) or received (vendors). The same list logic
 * backs "My repair requests" and the temporary vendor list, so [asVendor] picks the query.
 */
open class MyRepairsViewModel(
    private val authRepository: AuthRepository,
    private val repairRepository: RepairRepository,
    private val asVendor: Boolean = false,
) : ViewModel() {

    private val _uiState = MutableStateFlow<RepairListUiState>(RepairListUiState.Loading)
    val uiState: StateFlow<RepairListUiState> = _uiState

    var filter by mutableStateOf(RepairFilter.ALL)
        private set

    private val refreshGate = RefreshGate()

    init {
        load(showLoading = true)
    }

    fun selectFilter(value: RepairFilter) {
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
            _uiState.value = RepairListUiState.Error("Please log in to see your repair requests.")
            return
        }
        if (showLoading) _uiState.value = RepairListUiState.Loading
        refreshGate.markLoaded()
        viewModelScope.launch {
            try {
                val requests = if (asVendor) repairRepository.getRequestsByVendor(uid) else repairRepository.getRequestsByUser(uid)
                _uiState.value = RepairListUiState.Content(requests)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // A failed silent refresh keeps the old list instead of replacing it with an error.
                if (showLoading || _uiState.value !is RepairListUiState.Content) {
                    _uiState.value = RepairListUiState.Error(mapError(e, "Couldn't load repair requests."))
                }
            } finally {
                refreshGate.markLoaded()
            }
        }
    }
}

/** The repair requests addressed to a vendor. */
class VendorRepairsViewModel(
    authRepository: AuthRepository,
    repairRepository: RepairRepository,
) : MyRepairsViewModel(authRepository, repairRepository, asVendor = true)
