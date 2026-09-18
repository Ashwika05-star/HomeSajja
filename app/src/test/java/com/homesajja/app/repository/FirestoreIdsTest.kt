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

class StoragePathTest {

    @Test
    fun productionDownloadUrl_yieldsDecodedObjectPath() {
        val url = "https://firebasestorage.googleapis.com/v0/b/proj.appspot.com/o/" +
            "listings%2Fuid1%2Flisting1%2Fabc-123?alt=media&token=t0k3n"
        assertEquals("listings/uid1/listing1/abc-123", storagePathFromDownloadUrl(url))
    }

    @Test
    fun emulatorDownloadUrl_worksToo() {
        val url = "http://10.0.2.2:9199/v0/b/proj.appspot.com/o/listings%2Fu%2Fl%2Ff?alt=media&token=x"
        assertEquals("listings/u/l/f", storagePathFromDownloadUrl(url))
    }

    @Test
    fun urlWithoutAnObjectPath_isIgnored() {
        assertEquals(null, storagePathFromDownloadUrl("https://picsum.photos/seed/x/400/300"))
    }
}
