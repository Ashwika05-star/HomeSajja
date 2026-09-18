package com.homesajja.app.data.model

/** Stored at `chats/{id}`; messages live in the `messages` subcollection.
 * One chat per (context, pair of people) — see FirestoreIds.chatId. */
data class Chat(
    val id: String = "",
    val participantIds: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val contextType: EntityType = EntityType.LISTING,
    val contextId: String = "",
    val contextTitle: String = "",
    val lastMessage: String = "",
    val lastMessageAt: Long = 0L,
    val lastMessageSenderId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)
