package com.homesajja.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homesajja.app.data.model.Chat
import com.homesajja.app.data.model.Message
import com.homesajja.app.repository.AuthRepository
import com.homesajja.app.repository.ChatRepository
import com.homesajja.app.repository.NotificationSender
import com.homesajja.app.repository.NotificationTemplates
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

sealed interface ChatThreadUiState {
    data object Loading : ChatThreadUiState
    data class Error(val message: String) : ChatThreadUiState

    /** [messages] is oldest first and updates live; an empty list means nobody has said anything yet. */
    data class Content(val chat: Chat, val myId: String, val messages: List<Message>) : ChatThreadUiState
}

const val MAX_MESSAGE_LENGTH = 2000

/** One conversation: loads the chat, listens to its messages live, sends new ones and keeps the chat marked as read. */
class ChatThreadViewModel(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository,
    private val notificationSender: NotificationSender,
) : ViewModel() {

    private val chatId: String = checkNotNull(savedStateHandle["chatId"])
    private val myId: String? = authRepository.currentUserId

    private val _uiState = MutableStateFlow<ChatThreadUiState>(ChatThreadUiState.Loading)
    val uiState: StateFlow<ChatThreadUiState> = _uiState

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages

    var draft by mutableStateOf("")
        private set

    init {
        load()
    }

    fun retry() {
        _uiState.value = ChatThreadUiState.Loading
        load()
    }

    fun onDraftChange(value: String) {
        draft = value.take(MAX_MESSAGE_LENGTH)
    }

    fun send() {
        val content = _uiState.value as? ChatThreadUiState.Content ?: return
        val text = draft.trim()
        if (text.isEmpty()) return
        draft = ""
        viewModelScope.launch {
            try {
                chatRepository.sendMessage(chatId, content.myId, text)
                notificationSender.send(NotificationTemplates.newMessage(content.chat, content.myId, text))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                draft = text
                _messages.tryEmit(mapError(e, "Couldn't send your message."))
            }
        }
    }

    private fun load() {
        val uid = myId
        if (uid == null) {
            _uiState.value = ChatThreadUiState.Error("Please log in to see this chat.")
            return
        }
        viewModelScope.launch {
            try {
                val chat = chatRepository.getChat(chatId)
                if (chat == null || uid !in chat.participantIds) {
                    _uiState.value = ChatThreadUiState.Error("This chat isn't available.")
                    return@launch
                }
                chatRepository.observeMessages(chatId)
                    .catch { e -> _uiState.value = ChatThreadUiState.Error(mapError(e, "Couldn't load the messages.")) }
                    .collect { messages ->
                        _uiState.value = ChatThreadUiState.Content(chat, uid, messages)
                        markRead(uid)
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = ChatThreadUiState.Error(mapError(e, "Couldn't open this chat."))
            }
        }
    }

    /** Whenever the thread is open and messages arrive, they count as read. Failing to record it is harmless. */
    private suspend fun markRead(uid: String) {
        try {
            chatRepository.markRead(chatId, uid)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // The unread dot may linger until the next time the chat is opened.
        }
    }
}
