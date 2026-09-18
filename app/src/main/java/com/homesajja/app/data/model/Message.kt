package com.homesajja.app.data.model

/** Stored at `chats/{chatId}/messages/{id}`. */
data class Message(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val text: String = "",
    val imageUrl: String? = null,
    val sentAt: Long = System.currentTimeMillis(),
)
