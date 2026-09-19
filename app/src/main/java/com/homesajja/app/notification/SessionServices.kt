package com.homesajja.app.notification

import android.content.Context
import com.homesajja.app.repository.AuthRepository
import com.homesajja.app.repository.DeviceTokenRepository
import com.homesajja.app.repository.NotificationRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/**
 * Things that run for as long as someone is signed in, started once from the Application:
 * - files this phone's FCM token under them, so a server can push to it;
 * - while the app is in the background but still running, turns each new unseen notification into a system alert.
 *   (A push to a fully closed app needs the Cloud Function in `functions/`, which needs a paid plan.)
 */
class SessionServices(
    private val context: Context,
    private val scope: CoroutineScope,
    private val authRepository: AuthRepository,
    private val notificationRepository: NotificationRepository,
    private val deviceTokenRepository: DeviceTokenRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun start() {
        // Only notifications that arrive after login are alerted, not the backlog.
        scope.launch {
            authRepository.authState.collect { uid ->
                if (uid != null) registerToken(uid)
            }
        }
        scope.launch {
            var startedAt = System.currentTimeMillis()
            val alerted = mutableSetOf<String>()
            authRepository.authState
                .flatMapLatest { uid ->
                    startedAt = System.currentTimeMillis()
                    alerted.clear()
                    if (uid == null) flowOf(emptyList()) else notificationRepository.observeNotifications(uid, ALERT_WINDOW)
                }
                .catch { /* listener failed (offline or rules); alerts just pause */ }
                .collect { list ->
                    if (NotificationAlerts.isAppInForeground()) {
                        // In the foreground the bell badge is enough; remember them so they aren't alerted later.
                        list.forEach { alerted += it.id }
                        return@collect
                    }
                    list.filter { !it.seen && it.createdAt >= startedAt && alerted.add(it.id) }.forEach {
                        NotificationAlerts.show(context, it.id, it.title, it.body, it.relatedType, it.relatedId)
                    }
                }
        }
    }

    private suspend fun registerToken(uid: String) {
        try {
            deviceTokenRepository.registerCurrentToken(uid)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // FCM or the network isn't available right now; it is retried at the next login.
        }
    }

    private companion object {
        const val ALERT_WINDOW = 30
    }
}
