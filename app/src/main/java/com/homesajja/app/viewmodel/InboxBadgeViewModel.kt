package com.homesajja.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homesajja.app.repository.AuthRepository
import com.homesajja.app.repository.ChatRepository
import com.homesajja.app.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/** How many things are waiting: unread chats and unseen notifications, for the badges in the home top bars. */
data class InboxCounts(val unreadChats: Int = 0, val unseenNotifications: Int = 0)

class InboxBadgeViewModel(
    authRepository: AuthRepository,
    chatRepository: ChatRepository,
    notificationRepository: NotificationRepository,
) : ViewModel() {

    private val _counts = MutableStateFlow(InboxCounts())
    val counts: StateFlow<InboxCounts> = _counts

    init {
        val uid = authRepository.currentUserId
        if (uid != null) {
            viewModelScope.launch {
                chatRepository.observeChats(uid)
                    .catch { /* badge just stays as it was */ }
                    .collect { chats -> _counts.value = _counts.value.copy(unreadChats = chats.count { it.isUnreadFor(uid) }) }
            }
            viewModelScope.launch {
                notificationRepository.observeNotifications(uid)
                    .catch { /* badge just stays as it was */ }
                    .collect { list -> _counts.value = _counts.value.copy(unseenNotifications = list.count { !it.seen }) }
            }
        }
    }
}
