package com.homesajja.app.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.homesajja.app.data.model.Block
import kotlinx.coroutines.tasks.await

private const val COLLECTION = "blocks"

/** Blocked people at `blocks/{blockerId_blockedId}`. firestore.rules uses these documents to refuse chat and requests between the two. */
class BlockRepository(firestore: FirebaseFirestore) {

    private val blocks = firestore.collection(COLLECTION)

    suspend fun block(blockerId: String, blockedId: String, blockedName: String) {
        val id = idOf(blockerId, blockedId)
        blocks.document(id).set(Block(id = id, blockerId = blockerId, blockedId = blockedId, blockedName = blockedName)).await()
    }

    suspend fun unblock(blockerId: String, blockedId: String) {
        blocks.document(idOf(blockerId, blockedId)).delete().await()
    }

    /** Whether [blockerId] has blocked [blockedId]. Only the blocker can read this, so the other direction can't be asked. */
    suspend fun isBlocked(blockerId: String, blockedId: String): Boolean =
        blocks.document(idOf(blockerId, blockedId)).get().await().exists()

    /** Everyone [blockerId] has blocked, most recent first. */
    suspend fun getBlocked(blockerId: String): List<Block> =
        blocks.whereEqualTo("blockerId", blockerId).getAllAs<Block>().sortedByDescending { it.createdAt }

    private fun idOf(blockerId: String, blockedId: String) = "${blockerId}_$blockedId"
}
