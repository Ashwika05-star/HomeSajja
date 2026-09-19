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
        if (NotificationAlerts.isAppInForeground()) return
        val data = message.data
        val title = data["title"] ?: return
        val type = EntityType.entries.firstOrNull { it.name == data["relatedType"] } ?: EntityType.CHAT
        NotificationAlerts.show(
            context = applicationContext,
            notificationId = data["notificationId"] ?: message.messageId.orEmpty(),
            title = title,
            body = data["body"].orEmpty(),
            relatedType = type,
            relatedId = data["relatedId"].orEmpty(),
        )
    }
}
