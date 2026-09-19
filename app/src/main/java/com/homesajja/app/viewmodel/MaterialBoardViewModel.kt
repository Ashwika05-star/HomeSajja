package com.homesajja.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homesajja.app.data.model.MaterialRequest
import com.homesajja.app.data.model.MaterialType
import com.homesajja.app.repository.AuthRepository
import com.homesajja.app.repository.MaterialRequestRepository
import com.homesajja.app.repository.UserRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface MaterialListUiState {
    data object Loading : MaterialListUiState
    data class Error(val message: String) : MaterialListUiState
    data class Content(val requests: List<MaterialRequest>) : MaterialListUiState
}

/**
 * A list of material requests. [asVendor] = the vendor's own requests (all statuses);
 * otherwise the open requests in the user's city, which can be narrowed by material.
 */
open class MaterialBoardViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val materialRepository: MaterialRequestRepository,
    private val asVendor: Boolean = false,
) : ViewModel() {

    private val _uiState = MutableStateFlow<MaterialListUiState>(MaterialListUiState.Loading)
    val uiState: StateFlow<MaterialListUiState> = _uiState

    /** Null shows every material. */
    var materialFilter by mutableStateOf<MaterialType?>(null)
        private set

    var city by mutableStateOf("")
        private set

    private val refreshGate = RefreshGate()

    init {
        load(showLoading = true)
    }

    fun selectMaterial(type: MaterialType?) {
        materialFilter = type
    }

    fun retry() = load(showLoading = true)

    /** Called whenever the screen resumes; reloads quietly only if the data is old. */
    fun refreshIfStale() {
        if (refreshGate.isStale()) load(showLoading = false)
    }

    private fun load(showLoading: Boolean) {
        val uid = authRepository.currentUserId
        if (uid == null) {
            _uiState.value = MaterialListUiState.Error("Please log in to see material requests.")
            return
        }
        if (showLoading) _uiState.value = MaterialListUiState.Loading
        refreshGate.markLoaded()
        viewModelScope.launch {
            try {
                val requests = if (asVendor) {
                    materialRepository.getRequestsByVendor(uid)
                } else {
                    city = userRepository.getUserProfile(uid)?.city.orEmpty()
                    materialRepository.getOpenRequests(city)
                }
                _uiState.value = MaterialListUiState.Content(requests)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // A failed silent refresh keeps the old list instead of replacing it with an error.
                if (showLoading || _uiState.value !is MaterialListUiState.Content) {
                    _uiState.value = MaterialListUiState.Error(mapError(e, "Couldn't load material requests."))
                }
            } finally {
                refreshGate.markLoaded()
            }
        }
    }
}

/** The vendor's own material requests, for the Materials tab of the vendor space. */
class VendorMaterialsViewModel(
    authRepository: AuthRepository,
    userRepository: UserRepository,
    materialRepository: MaterialRequestRepository,
) : MaterialBoardViewModel(authRepository, userRepository, materialRepository, asVendor = true)
