package com.homesajja.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homesajja.app.data.model.Notification
import com.homesajja.app.repository.AuthRepository
import com.homesajja.app.repository.NotificationRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

sealed interface NotificationsUiState {
    data object Loading : NotificationsUiState
    data class Error(val message: String) : NotificationsUiState
    data class Content(val notifications: List<Notification>) : NotificationsUiState
}

enum class NotificationFilter(val label: String) {
    ALL("All"),
    UNREAD("Unread"),
}

fun NotificationFilter.apply(list: List<Notification>): List<Notification> = when (this) {
    NotificationFilter.ALL -> list
    NotificationFilter.UNREAD -> list.filter { !it.seen }
}

/** The signed-in person's notifications, live, newest first. */
class NotificationsViewModel(
    private val authRepository: AuthRepository,
    private val notificationRepository: NotificationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<NotificationsUiState>(NotificationsUiState.Loading)
    val uiState: StateFlow<NotificationsUiState> = _uiState

    var filter by mutableStateOf(NotificationFilter.ALL)
        private set

    init {
        observe()
    }

    fun selectFilter(value: NotificationFilter) {
        filter = value
    }

    fun retry() {
        _uiState.value = NotificationsUiState.Loading
        observe()
    }

    /** Opening one marks it as read; the live list then drops its unread dot. */
    fun markRead(notification: Notification) {
        if (notification.seen) return
        viewModelScope.launch { runCatching { notificationRepository.markAsRead(notification.id) } }
    }

    fun markAllRead() {
        val content = _uiState.value as? NotificationsUiState.Content ?: return
        val unread = content.notifications.filter { !it.seen }.map { it.id }
        viewModelScope.launch { runCatching { notificationRepository.markAllAsRead(unread) } }
    }

    private fun observe() {
        val uid = authRepository.currentUserId
        if (uid == null) {
            _uiState.value = NotificationsUiState.Error("Please log in to see your notifications.")
            return
        }
        viewModelScope.launch {
            try {
                notificationRepository.observeNotifications(uid)
                    .catch { e -> _uiState.value = NotificationsUiState.Error(mapError(e, "Couldn't load your notifications.")) }
                    .collect { _uiState.value = NotificationsUiState.Content(it) }
            } catch (e: CancellationException) {
                throw e
            }
        }
    }
}
