package com.homesajja.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homesajja.app.data.model.Chat
import com.homesajja.app.repository.AuthRepository
import com.homesajja.app.repository.ChatRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

sealed interface ChatListUiState {
    data object Loading : ChatListUiState
    data class Error(val message: String) : ChatListUiState

    /** [chats] is newest activity first; [myId] lets the screen work out who the "other person" is and what is unread. */
    data class Content(val chats: List<Chat>, val myId: String) : ChatListUiState
}

/** Which chats the inbox shows. */
enum class ChatFilter(val label: String) {
    ALL("All"),
    UNREAD("Unread"),
}

/** The chat inbox: every thread the signed-in person is in, kept live by a Firestore snapshot listener. */
class ChatListViewModel(
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatListUiState>(ChatListUiState.Loading)
    val uiState: StateFlow<ChatListUiState> = _uiState

    var filter by mutableStateOf(ChatFilter.ALL)
        private set

    init {
        observe()
    }

    fun selectFilter(value: ChatFilter) {
        filter = value
    }

    fun retry() {
        _uiState.value = ChatListUiState.Loading
        observe()
    }

    private fun observe() {
        val uid = authRepository.currentUserId
        if (uid == null) {
            _uiState.value = ChatListUiState.Error("Please log in to see your chats.")
            return
        }
        viewModelScope.launch {
            try {
                chatRepository.observeChats(uid)
                    .catch { e -> _uiState.value = ChatListUiState.Error(mapError(e, "Couldn't load your chats.")) }
                    .collect { chats -> _uiState.value = ChatListUiState.Content(chats.filter { it.lastMessageAt > 0 }, uid) }
            } catch (e: CancellationException) {
                throw e
            }
        }
    }
}

/** Chats that already have a message; a thread that was created but never used stays out of the inbox. */
fun ChatFilter.apply(chats: List<Chat>, myId: String): List<Chat> = when (this) {
    ChatFilter.ALL -> chats
    ChatFilter.UNREAD -> chats.filter { it.isUnreadFor(myId) }
}
