package com.homesajja.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homesajja.app.data.model.RecyclingRequest
import com.homesajja.app.data.model.VendorBusinessType
import com.homesajja.app.data.model.VendorProfile
import com.homesajja.app.repository.AuthRepository
import com.homesajja.app.repository.NotificationSender
import com.homesajja.app.repository.NotificationTemplates
import com.homesajja.app.repository.RecyclingRepository
import com.homesajja.app.repository.VendorRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** The kinds of incoming request a vendor can see, in tab order. */
enum class RequestSection(val label: String) {
    PURCHASES("Purchases"),
    EXCHANGES("Exchanges"),
    REPAIRS("Repairs"),
    RECYCLING("Recycling"),
}

/**
 * Which sections apply to a vendor: everyone gets purchase and exchange requests (they list items like anyone);
 * repair requests only go to repair providers and recycling requests only to recyclers.
 */
fun sectionsFor(businessType: VendorBusinessType?): List<RequestSection> = buildList {
    add(RequestSection.PURCHASES)
    add(RequestSection.EXCHANGES)
    if (businessType in VendorBusinessType.REPAIR_PROVIDERS) add(RequestSection.REPAIRS)
    if (businessType in VendorBusinessType.RECYCLERS) add(RequestSection.RECYCLING)
}

sealed interface VendorRequestsUiState {
    data object Loading : VendorRequestsUiState
    data class Error(val message: String) : VendorRequestsUiState
    data class Content(val sections: List<RequestSection>) : VendorRequestsUiState
}

/** Works out which request sections the signed-in vendor should see. */
class VendorRequestsViewModel(
    private val authRepository: AuthRepository,
    private val vendorRepository: VendorRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<VendorRequestsUiState>(VendorRequestsUiState.Loading)
    val uiState: StateFlow<VendorRequestsUiState> = _uiState

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        _uiState.value = VendorRequestsUiState.Loading
        viewModelScope.launch {
            try {
                val uid = authRepository.currentUserId
                val vendor = uid?.let { vendorRepository.getVendorProfile(it) }
                _uiState.value = if (vendor == null) {
                    VendorRequestsUiState.Error("Couldn't find your vendor profile.")
                } else {
                    VendorRequestsUiState.Content(sectionsFor(VendorBusinessType.fromNameOrNull(vendor.businessType)))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = VendorRequestsUiState.Error(mapError(e, "Couldn't load your requests."))
            }
        }
    }
}

sealed interface OpenPickupsUiState {
    data object Loading : OpenPickupsUiState
    data class Error(val message: String) : OpenPickupsUiState
    data class Content(val pickups: List<RecyclingRequest>, val city: String) : OpenPickupsUiState
}

/** For recyclers: pickup requests in their city that nobody has claimed yet, and the claim action. */
class OpenPickupsViewModel(
    private val authRepository: AuthRepository,
    private val vendorRepository: VendorRepository,
    private val recyclingRepository: RecyclingRepository,
    private val notificationSender: NotificationSender,
) : ViewModel() {

    private val _uiState = MutableStateFlow<OpenPickupsUiState>(OpenPickupsUiState.Loading)
    val uiState: StateFlow<OpenPickupsUiState> = _uiState

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages

    var busyId by mutableStateOf<String?>(null)
        private set

    private var vendor: VendorProfile? = null

    init {
        load(showLoading = true)
    }

    fun retry() = load(showLoading = true)

    fun claim(request: RecyclingRequest) {
        val me = vendor ?: return
        if (busyId != null) return
        busyId = request.id
        viewModelScope.launch {
            try {
                val vendorName = me.businessName.ifBlank { me.name }
                recyclingRepository.claimPickup(request.id, me.uid, vendorName)
                notificationSender.send(NotificationTemplates.pickupClaimed(request, me.uid, vendorName))
                _messages.tryEmit("Pickup claimed. Find it under My requests.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Most likely another recycler claimed it first.
                _messages.tryEmit(mapError(e, "Couldn't claim this pickup. Someone else may have taken it."))
            } finally {
                busyId = null
                load(showLoading = false)
            }
        }
    }

    private fun load(showLoading: Boolean) {
        if (showLoading) _uiState.value = OpenPickupsUiState.Loading
        viewModelScope.launch {
            try {
                val uid = authRepository.currentUserId
                val profile = vendor ?: uid?.let { vendorRepository.getVendorProfile(it) }
                if (profile == null) {
                    _uiState.value = OpenPickupsUiState.Error("Couldn't find your vendor profile.")
                    return@launch
                }
                vendor = profile
                _uiState.value = OpenPickupsUiState.Content(recyclingRepository.getOpenPickups(profile.city), profile.city)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (showLoading || _uiState.value !is OpenPickupsUiState.Content) {
                    _uiState.value = OpenPickupsUiState.Error(mapError(e, "Couldn't load open pickups."))
                }
            }
        }
    }
}
