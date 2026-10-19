package com.homesajja.app.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.homesajja.app.HomeSajjaApp
import com.homesajja.app.data.model.EntityType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Receives FCM. Pushes are data-only messages (`notificationId`, `title`, `body`, `relatedType`, `relatedId`),
 * so this service builds the alert itself, in the background as well as the foreground.
 */
class HomeSajjaMessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** FCM rotated this phone's token: file the new one under whoever is signed in. */
    override fun onNewToken(token: String) {
        val container = (application as HomeSajjaApp).container
        val uid = container.authRepository.currentUserId ?: return
        scope.launch { runCatching { container.deviceTokenRepository.save(uid, token) } }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val container = (application as? HomeSajjaApp)?.container
        val data = message.data
        val title = data["title"] ?: message.notification?.title ?: return
        val body = data["body"] ?: message.notification?.body.orEmpty()
        val type = EntityType.entries.firstOrNull { it.name == data["relatedType"] } ?: EntityType.CHAT
        val relatedId = data["relatedId"].orEmpty()

        // If the user is currently viewing this exact chat, suppress the alert
        if (type == EntityType.CHAT && container?.activeChatId?.value == relatedId) {
            return
        }

        NotificationAlerts.show(
            context = applicationContext,
            notificationId = data["notificationId"] ?: message.messageId.orEmpty(),
            title = title,
            body = body,
            relatedType = type,
            relatedId = relatedId,
        )
    }
}
