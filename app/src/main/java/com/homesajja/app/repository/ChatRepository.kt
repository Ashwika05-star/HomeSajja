package com.homesajja.app.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.homesajja.app.data.model.Chat
import com.homesajja.app.data.model.EntityType
import com.homesajja.app.data.model.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

private const val CHATS = "chats"
private const val MESSAGES = "messages"
private const val RECENT_MESSAGE_LIMIT = 100

/** Enquiry chats at `chats/{id}` with messages in `chats/{id}/messages`.
 * Shared by every system: a chat only records which record it is about. */
class ChatRepository(private val firestore: FirebaseFirestore) {

    private val chats = firestore.collection(CHATS)

    /** Returns the existing chat for this context and pair, or creates it.
     * [participantNames] maps each of the two uids to a display name. */
    suspend fun getOrCreateChat(
        contextType: EntityType,
        contextId: String,
        contextTitle: String,
        participantNames: Map<String, String>,
        contextImage: String? = null,
    ): Chat {
        require(participantNames.size == 2) { "A chat has exactly two participants." }
        val (userA, userB) = participantNames.keys.toList()
        val ref = chats.document(FirestoreIds.chatId(contextType, contextId, userA, userB))

        ref.getAs<Chat>()?.let { return it }

        val chat = Chat(
            id = ref.id,
            participantIds = participantNames.keys.toList(),
            participantNames = participantNames,
            contextType = contextType,
            contextId = contextId,
            contextTitle = contextTitle,
            contextImage = contextImage,
        )
        ref.set(chat).await()
        return chat
    }

    suspend fun getChat(id: String): Chat? = chats.document(id).getAs()

    /** The user's inbox, most recently active first. Emits on every change. */
    fun observeChats(userId: String): Flow<List<Chat>> =
        chats.whereArrayContains("participantIds", userId)
            .orderBy("lastMessageAt", Query.Direction.DESCENDING)
            .observeAs()

    /** Records that [userId] has seen everything in the chat up to now (clears its unread mark). */
    suspend fun markRead(chatId: String, userId: String) {
        chats.document(chatId).update("readAt.$userId", System.currentTimeMillis()).await()
    }

    /** The most recent messages in a chat, oldest first. Emits on every new message. */
    fun observeMessages(chatId: String): Flow<List<Message>> =
        chats.document(chatId).collection(MESSAGES)
            .orderBy("sentAt")
            .limitToLast(RECENT_MESSAGE_LIMIT.toLong())
            .observeAs()

    /** Writes the message and updates the chat's last-message preview atomically. */
    suspend fun sendMessage(chatId: String, senderId: String, text: String, imageUrl: String? = null): Message {
        val chatRef = chats.document(chatId)
        val messageRef = chatRef.collection(MESSAGES).document()
        val message = Message(id = messageRef.id, chatId = chatId, senderId = senderId, text = text, imageUrl = imageUrl)

        firestore.batch()
            .set(messageRef, message)
            .update(
                chatRef,
                mapOf(
                    "lastMessage" to text.ifBlank { "Photo" },
                    "lastMessageAt" to message.sentAt,
                    "lastMessageSenderId" to senderId,
                ),
            )
            .commit()
            .await()
        return message
    }

    /** Senders can remove their own messages; whole chats are never deleted. */
    suspend fun deleteMessage(chatId: String, messageId: String) {
        chats.document(chatId).collection(MESSAGES).document(messageId).delete().await()
    }
}
