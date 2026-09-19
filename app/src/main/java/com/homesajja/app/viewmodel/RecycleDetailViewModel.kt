package com.homesajja.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homesajja.app.data.model.RecyclingRequest
import com.homesajja.app.repository.AuthRepository
import com.homesajja.app.repository.RecyclingRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface RecycleDetailUiState {
    data object Loading : RecycleDetailUiState
    data class Error(val message: String) : RecycleDetailUiState

    /** [viewerIsRecycler] decides who the "other party" is and which actions apply. */
    data class Content(
        val request: RecyclingRequest,
        val actions: List<RecycleAction>,
        val viewerIsRecycler: Boolean,
        val isBusy: Boolean = false,
    ) : RecycleDetailUiState
}

/** One recycling request: its tracking pipeline and the actions the viewer may take. */
class RecycleDetailViewModel(
    savedStateHandle: SavedStateHandle,
    authRepository: AuthRepository,
    private val recyclingRepository: RecyclingRepository,
) : ViewModel() {

    private val requestId: String = checkNotNull(savedStateHandle["requestId"])
    private val myId: String? = authRepository.currentUserId

    private val _uiState = MutableStateFlow<RecycleDetailUiState>(RecycleDetailUiState.Loading)
    val uiState: StateFlow<RecycleDetailUiState> = _uiState

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages

    init {
        load(showLoading = true)
    }

    fun retry() = load(showLoading = true)

    fun perform(action: RecycleAction) {
        val content = _uiState.value as? RecycleDetailUiState.Content ?: return
        if (content.isBusy) return
        _uiState.value = content.copy(isBusy = true)

        viewModelScope.launch {
            try {
                recyclingRepository.updateStatus(content.request.id, action.target)
                _messages.tryEmit("Status updated to ${action.target.displayName}.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _messages.tryEmit(mapError(e, "Couldn't update the request."))
            } finally {
                // Show what is really stored now, whether the update worked or not.
                load(showLoading = false)
            }
        }
    }

    private fun load(showLoading: Boolean) {
        if (showLoading) _uiState.value = RecycleDetailUiState.Loading
        viewModelScope.launch {
            try {
                val request = recyclingRepository.getRequest(requestId)
                if (request == null) {
                    _uiState.value = RecycleDetailUiState.Error("This recycling request no longer exists.")
                    return@launch
                }
                _uiState.value = RecycleDetailUiState.Content(
                    request = request,
                    actions = recycleActionsFor(request, myId),
                    viewerIsRecycler = myId != null && myId == request.vendorId,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (showLoading || _uiState.value !is RecycleDetailUiState.Content) {
                    _uiState.value = RecycleDetailUiState.Error(mapError(e, "Couldn't load this recycling request."))
                } else {
                    (_uiState.value as? RecycleDetailUiState.Content)?.let { _uiState.value = it.copy(isBusy = false) }
                }
            }
        }
    }
}
