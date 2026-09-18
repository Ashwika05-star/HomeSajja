package com.homesajja.app.repository

import com.homesajja.app.data.model.EntityType

/** Deterministic document ids. Using the same key twice hits the same document,
 * which gives us "one favourite per listing", "one review per transaction" and
 * "one chat per pair per context" without any extra lookup. firestore.rules
 * checks the favourite and review formats, so keep them in sync. */
object FirestoreIds {

    fun favouriteId(userId: String, listingId: String): String = "${userId}_$listingId"

    fun reviewId(reviewerId: String, contextType: EntityType, contextId: String): String =
        "${reviewerId}_${contextType.name}_$contextId"

    fun chatId(contextType: EntityType, contextId: String, userA: String, userB: String): String {
        val (first, second) = listOf(userA, userB).sorted()
        return "${contextType.name}_${contextId}_${first}_$second"
    }
}
