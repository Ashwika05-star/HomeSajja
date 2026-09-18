package com.homesajja.app.data.model

/** Stored at `favourites/{userId_listingId}`. */
data class Favourite(
    val id: String = "",
    val userId: String = "",
    val listingId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)
