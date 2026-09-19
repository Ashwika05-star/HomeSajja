package com.homesajja.app.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.homesajja.app.data.model.DeviceToken
import kotlinx.coroutines.tasks.await

private const val COLLECTION = "deviceTokens"

/** Remembers this phone's FCM token against the signed-in person at `deviceTokens/{token}`, so a server can push to them. */
class DeviceTokenRepository(firestore: FirebaseFirestore, private val messaging: FirebaseMessaging) {

    private val tokens = firestore.collection(COLLECTION)

    /** Fetches the current token and stores it for [userId]. Logging in on a shared phone hands the token to the new person. */
    suspend fun registerCurrentToken(userId: String) {
        save(userId, messaging.token.await())
    }

    /** Called when FCM rotates the token. */
    suspend fun save(userId: String, token: String) {
        tokens.document(token).set(DeviceToken(token = token, userId = userId)).await()
    }

    /** Best effort on logout: the phone stops being reachable as this person. */
    suspend fun unregisterCurrentToken() {
        val token = messaging.token.await()
        tokens.document(token).delete().await()
    }
}
