package com.homesajja.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homesajja.app.data.model.EntityType
import com.homesajja.app.data.model.ExchangeRequest
import com.homesajja.app.data.model.ExchangeStatus
import com.homesajja.app.data.model.FurnitureListing
import com.homesajja.app.repository.AuthRepository
import com.homesajja.app.repository.ChatRepository
import com.homesajja.app.repository.NotificationSender
import com.homesajja.app.repository.NotificationTemplates
import com.homesajja.app.repository.ExchangeRepository
import com.homesajja.app.repository.ListingRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface ExchangeDetailUiState {
    data object Loading : ExchangeDetailUiState
    data class Error(val message: String) : ExchangeDetailUiState

    /** [offered] / [requested] are null if that listing has since been deleted; the request's
     * own snapshot fields still describe it. [chatId] is set once the request's chat thread exists. */
    data class Content(
        val request: ExchangeRequest,
        val offered: FurnitureListing?,
        val requested: FurnitureListing?,
        val actions: List<ExchangeAction>,
        val isSender: Boolean,
        val chatId: String?,
        val isBusy: Boolean = false,
    ) : ExchangeDetailUiState
}

/**
 * One exchange request: loads it with both items, offers the actions the viewer is allowed,
 * and makes sure the request's chat thread exists once it has been accepted.
 */
class ExchangeDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val exchangeRepository: ExchangeRepository,
    private val listingRepository: ListingRepository,
    private val chatRepository: ChatRepository,
    private val notificationSender: NotificationSender,
) : ViewModel() {

    private val requestId: String = checkNotNull(savedStateHandle["requestId"])
    private val myId: String? = authRepository.currentUserId

    private val _uiState = MutableStateFlow<ExchangeDetailUiState>(ExchangeDetailUiState.Loading)
    val uiState: StateFlow<ExchangeDetailUiState> = _uiState

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages

    init {
        load(showLoading = true)
    }

    fun retry() = load(showLoading = true)

    fun perform(action: ExchangeAction) {
        val content = _uiState.value as? ExchangeDetailUiState.Content ?: return
        if (content.isBusy) return
        _uiState.value = content.copy(isBusy = true)

        viewModelScope.launch {
            try {
                val request = content.request
                when (action) {
                    ExchangeAction.ACCEPT -> {
                        exchangeRepository.acceptRequest(request)
                        val chatId = ensureChat(request)
                        notificationSender.send(NotificationTemplates.exchangeDecision(request, accepted = true))
                        _messages.tryEmit(
                            if (chatId != null) "Accepted. A chat with ${request.senderName} is ready."
                            else "Accepted, but the chat couldn't be created yet.",
                        )
                    }
                    ExchangeAction.DECLINE -> {
                        exchangeRepository.declineRequest(request.id)
                        notificationSender.send(NotificationTemplates.exchangeDecision(request, accepted = false))
                        _messages.tryEmit("Request declined.")
                    }
                    ExchangeAction.CANCEL -> {
                        exchangeRepository.cancelRequest(request.id)
                        _messages.tryEmit("Request cancelled.")
                    }
                    ExchangeAction.COMPLETE -> {
                        exchangeRepository.completeRequest(request)
                        _messages.tryEmit("Exchange marked as completed.")
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _messages.tryEmit(mapError(e, "Couldn't update the request."))
            } finally {
                // Show what is really stored now, whether the action worked or not.
                load(showLoading = false)
            }
        }
    }

    private fun load(showLoading: Boolean) {
        if (showLoading) _uiState.value = ExchangeDetailUiState.Loading
        viewModelScope.launch {
            try {
                val request = exchangeRepository.getRequest(requestId)
                if (request == null) {
                    _uiState.value = ExchangeDetailUiState.Error("This exchange request no longer exists.")
                    return@launch
                }
                val chatId = if (request.status in CHAT_STATUSES) ensureChat(request) else null
                _uiState.value = ExchangeDetailUiState.Content(
                    request = request,
                    offered = listingRepository.getListing(request.offeredListingId),
                    requested = listingRepository.getListing(request.requestedListingId),
                    actions = exchangeActionsFor(request, myId),
                    isSender = myId == request.senderId,
                    chatId = chatId,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (showLoading || _uiState.value !is ExchangeDetailUiState.Content) {
                    _uiState.value = ExchangeDetailUiState.Error(mapError(e, "Couldn't load this exchange."))
                }
            }
        }
    }

    /** Creates the chat thread tied to this request, or finds it if it exists, and returns its id (null if that failed). Safe to repeat. */
    private suspend fun ensureChat(request: ExchangeRequest): String? = try {
        chatRepository.getOrCreateChat(
            contextType = EntityType.EXCHANGE_REQUEST,
            contextId = request.id,
            contextTitle = "${request.offeredTitle} ⇄ ${request.requestedTitle}",
            participantNames = mapOf(request.senderId to request.senderName, request.receiverId to request.receiverName),
            contextImage = request.requestedImageUrl,
        ).id
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    private companion object {
        val CHAT_STATUSES = setOf(ExchangeStatus.ACCEPTED, ExchangeStatus.COMPLETED)
    }
}
