package com.homesajja.app.repository

import com.homesajja.app.data.model.EntityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class FirestoreIdsTest {

    @Test
    fun chatId_isSameRegardlessOfParticipantOrder() {
        val ab = FirestoreIds.chatId(EntityType.LISTING, "l1", "alice", "bob")
        val ba = FirestoreIds.chatId(EntityType.LISTING, "l1", "bob", "alice")
        assertEquals(ab, ba)
    }

    @Test
    fun chatId_differsPerContext() {
        val listing = FirestoreIds.chatId(EntityType.LISTING, "x1", "alice", "bob")
        val repair = FirestoreIds.chatId(EntityType.REPAIR_REQUEST, "x1", "alice", "bob")
        assertNotEquals(listing, repair)
    }

    // These formats are also enforced by firestore.rules — change both together.
    @Test
    fun favouriteId_matchesRulesFormat() {
        assertEquals("alice_l1", FirestoreIds.favouriteId("alice", "l1"))
    }

    @Test
    fun reviewId_matchesRulesFormat() {
        assertEquals("alice_REPAIR_REQUEST_r1", FirestoreIds.reviewId("alice", EntityType.REPAIR_REQUEST, "r1"))
    }
}
