package com.homesajja.app.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.homesajja.app.data.model.Notification
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

private const val COLLECTION = "notifications"

/** In-app notifications at `notifications/{id}`, visible only to the recipient.
 * They are written from the client for now; a later Cloud Functions phase can
 * move creation server-side. Recipients can only flip `seen`. */
class NotificationRepository(firestore: FirebaseFirestore) {

    private val notifications = firestore.collection(COLLECTION)

    suspend fun createNotification(notification: Notification): Notification {
        val ref = notifications.document()
        val saved = notification.copy(id = ref.id)
        ref.set(saved).await()
        return saved
    }

    suspend fun getNotification(id: String): Notification? = notifications.document(id).getAs()

    /** The user's notifications, newest first. Emits on every change. */
    fun observeNotifications(recipientId: String, limit: Int = DEFAULT_PAGE_SIZE): Flow<List<Notification>> =
        notifications.whereEqualTo("recipientId", recipientId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .observeAs()

    suspend fun markAsRead(id: String) {
        notifications.document(id).update("seen", true).await()
    }

    suspend fun deleteNotification(id: String) {
        notifications.document(id).delete().await()
    }
}
