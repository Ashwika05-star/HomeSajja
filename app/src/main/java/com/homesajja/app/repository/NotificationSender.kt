package com.homesajja.app.repository

import com.homesajja.app.data.model.Notification
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Creates notification documents for other people, from the app. It is fire-and-forget on purpose: telling
 * someone about an action must never slow down or fail the action itself (a message, a status change...),
 * and it keeps working if the phone is offline (the write is queued and sent later).
 */
class NotificationSender(
    private val repository: NotificationRepository,
    private val scope: CoroutineScope,
) {
    fun send(notification: Notification?) {
        if (notification == null || notification.recipientId.isBlank()) return
        scope.launch {
            try {
                repository.createNotification(notification)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Best effort: the action already succeeded, so a missed notification is not worth an error.
            }
        }
    }
}
