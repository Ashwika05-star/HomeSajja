package com.homesajja.app.data.model

/** Stored at `chats/{id}`; messages live in the `messages` subcollection.
 * One chat per (context, pair of people) — see FirestoreIds.chatId.
 * [contextImage] is a thumbnail of what the chat is about. [readAt] maps each participant's uid to the time
 * they last opened the chat, which is how "unread" is worked out. */
data class Chat(
    val id: String = "",
    val participantIds: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val contextType: EntityType = EntityType.LISTING,
    val contextId: String = "",
    val contextTitle: String = "",
    val contextImage: String? = null,
    val lastMessage: String = "",
    val lastMessageAt: Long = 0L,
    val lastMessageSenderId: String = "",
    val readAt: Map<String, Long> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis(),
) {
    /** The other person in the chat, from [userId]'s point of view. */
    fun otherParticipantId(userId: String): String? = participantIds.firstOrNull { it != userId }

    fun otherParticipantName(userId: String): String =
        otherParticipantId(userId)?.let { participantNames[it] }.orEmpty().ifBlank { "Someone" }

    /** A message from the other person that [userId] hasn't opened the chat since. */
    fun isUnreadFor(userId: String): Boolean =
        lastMessageAt > 0 && lastMessageSenderId.isNotEmpty() && lastMessageSenderId != userId &&
            lastMessageAt > (readAt[userId] ?: 0L)
}
