package com.homesajja.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homesajja.app.data.model.RepairRequest
import com.homesajja.app.repository.AuthRepository
import com.homesajja.app.repository.ChatRepository
import com.homesajja.app.repository.NotificationSender
import com.homesajja.app.repository.NotificationTemplates
import com.homesajja.app.repository.RepairRepository
import com.homesajja.app.data.model.EntityType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface RepairDetailUiState {
    data object Loading : RepairDetailUiState
    data class Error(val message: String) : RepairDetailUiState

    /** [viewerIsVendor] decides who the "other party" is and which actions apply. */
    data class Content(
        val request: RepairRequest,
        val actions: List<RepairAction>,
        val viewerIsVendor: Boolean,
        val isBusy: Boolean = false,
    ) : RepairDetailUiState
}

/** One repair request: its tracking pipeline and the actions the viewer may take. */
class RepairDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val repairRepository: RepairRepository,
    private val chatRepository: ChatRepository,
    private val notificationSender: NotificationSender,
) : ViewModel() {

    private val requestId: String = checkNotNull(savedStateHandle["requestId"])
    private val myId: String? = authRepository.currentUserId

    private val _uiState = MutableStateFlow<RepairDetailUiState>(RepairDetailUiState.Loading)
    val uiState: StateFlow<RepairDetailUiState> = _uiState

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages

    /** Emits a chat id once the thread with the other party exists, so the screen can open it. */
    private val _openChat = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val openChat: SharedFlow<String> = _openChat

    init {
        load(showLoading = true)
    }

    fun retry() = load(showLoading = true)

    fun perform(action: RepairAction) {
        val content = _uiState.value as? RepairDetailUiState.Content ?: return
        if (content.isBusy) return
        _uiState.value = content.copy(isBusy = true)

        viewModelScope.launch {
            try {
                repairRepository.updateStatus(content.request.id, action.target)
                myId?.let { notificationSender.send(NotificationTemplates.repairStatus(content.request, action.target, it)) }
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

    /** Finds or creates the chat between the customer and the provider about this repair, then opens it. */
    fun openChat() {
        val content = _uiState.value as? RepairDetailUiState.Content ?: return
        val request = content.request
        viewModelScope.launch {
            try {
                val chat = chatRepository.getOrCreateChat(
                    contextType = EntityType.REPAIR_REQUEST,
                    contextId = request.id,
                    contextTitle = "${request.furnitureTitle} · ${request.problemType.displayName}",
                    participantNames = mapOf(request.userId to request.userName, request.vendorId to request.vendorName),
                    contextImage = request.images.firstOrNull(),
                )
                _openChat.tryEmit(chat.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _messages.tryEmit(mapError(e, "Couldn't open the chat."))
            }
        }
    }

    private fun load(showLoading: Boolean) {
        if (showLoading) _uiState.value = RepairDetailUiState.Loading
        viewModelScope.launch {
            try {
                val request = repairRepository.getRequest(requestId)
                if (request == null) {
                    _uiState.value = RepairDetailUiState.Error("This repair request no longer exists.")
                    return@launch
                }
                _uiState.value = RepairDetailUiState.Content(
                    request = request,
                    actions = repairActionsFor(request, myId),
                    viewerIsVendor = myId == request.vendorId,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (showLoading || _uiState.value !is RepairDetailUiState.Content) {
                    _uiState.value = RepairDetailUiState.Error(mapError(e, "Couldn't load this repair request."))
                } else {
                    (_uiState.value as? RepairDetailUiState.Content)?.let { _uiState.value = it.copy(isBusy = false) }
                }
            }
        }
    }
}
