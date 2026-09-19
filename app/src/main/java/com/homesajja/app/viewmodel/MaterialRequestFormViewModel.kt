package com.homesajja.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homesajja.app.data.model.MaterialRequest
import com.homesajja.app.repository.AuthRepository
import com.homesajja.app.repository.MaterialRequestRepository
import com.homesajja.app.repository.VendorRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

sealed interface MaterialFormScreenState {
    data object Loading : MaterialFormScreenState
    data class Error(val message: String) : MaterialFormScreenState
    data object Ready : MaterialFormScreenState
}

sealed interface MaterialSaveState {
    data object Idle : MaterialSaveState
    data object Saving : MaterialSaveState
    data class Saved(val requestId: String) : MaterialSaveState
    data class Failed(val message: String) : MaterialSaveState
}

/** Posts a new material request, or edits one when opened with a `requestId`. The request's city is the vendor's city. */
class MaterialRequestFormViewModel(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val vendorRepository: VendorRepository,
    private val materialRepository: MaterialRequestRepository,
) : ViewModel() {

    private val editingId: String? = savedStateHandle["requestId"]
    val isEditing: Boolean = editingId != null

    var screenState by mutableStateOf<MaterialFormScreenState>(MaterialFormScreenState.Loading)
        private set
    var form by mutableStateOf(MaterialRequestForm())
        private set
    var formError by mutableStateOf<String?>(null)
        private set
    var saveState by mutableStateOf<MaterialSaveState>(MaterialSaveState.Idle)
        private set

    private var editing: MaterialRequest? = null
    private var vendorName = ""
    var city by mutableStateOf("")
        private set

    init {
        load()
    }

    fun load() {
        screenState = MaterialFormScreenState.Loading
        viewModelScope.launch {
            try {
                val uid = authRepository.currentUserId
                val vendor = uid?.let { vendorRepository.getVendorProfile(it) }
                if (vendor == null) {
                    screenState = MaterialFormScreenState.Error("Only vendors can post material requests.")
                    return@launch
                }
                vendorName = vendor.businessName.ifBlank { vendor.name }
                city = vendor.city
                if (editingId != null) {
                    val request = materialRepository.getRequest(editingId)
                    if (request == null || request.vendorId != uid) {
                        screenState = MaterialFormScreenState.Error("This request can't be edited.")
                        return@launch
                    }
                    editing = request
                    form = MaterialRequestForm(
                        title = request.title,
                        materialType = request.materialType,
                        quantity = request.quantity,
                        budgetMin = request.budgetMin?.toString().orEmpty(),
                        budgetMax = request.budgetMax?.toString().orEmpty(),
                        description = request.description,
                    )
                }
                screenState = MaterialFormScreenState.Ready
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                screenState = MaterialFormScreenState.Error(mapError(e, "Couldn't open the form."))
            }
        }
    }

    fun update(transform: (MaterialRequestForm) -> MaterialRequestForm) {
        form = transform(form)
        formError = null
    }

    fun dismissSaveError() {
        saveState = MaterialSaveState.Idle
    }

    fun save() {
        val uid = authRepository.currentUserId ?: return
        val problem = form.validate()
        if (problem != null) {
            formError = problem
            return
        }
        val type = form.materialType ?: return
        saveState = MaterialSaveState.Saving
        viewModelScope.launch {
            try {
                val base = editing ?: MaterialRequest(vendorId = uid, vendorName = vendorName, city = city)
                val request = base.copy(
                    title = form.title.trim(),
                    materialType = type,
                    quantity = form.quantity.trim(),
                    budgetMin = form.parsedBudgetMin,
                    budgetMax = form.parsedBudgetMax,
                    description = form.description.trim(),
                )
                if (editing == null) {
                    saveState = MaterialSaveState.Saved(materialRepository.createRequest(request).id)
                } else {
                    materialRepository.updateRequest(request)
                    saveState = MaterialSaveState.Saved(request.id)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                saveState = MaterialSaveState.Failed(mapError(e, "Couldn't save the request. Please try again."))
            }
        }
    }
}
