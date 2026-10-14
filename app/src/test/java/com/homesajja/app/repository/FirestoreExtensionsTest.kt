package com.homesajja.app.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FakeDocumentSnapshot
import com.homesajja.app.data.model.Notification
import com.homesajja.app.data.model.NotificationType
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit test for resilient Firestore document deserialization.
 * Verifies that a list of document snapshots containing an invalid enum value (or corrupt schema)
 * does not crash or throw, but instead logs a warning and returns the valid documents.
 */
class FirestoreExtensionsTest {

    @Test
    fun snapshotWithOneInvalidEnumValueAndTwoValidDocuments_returnsTwoValidOnesAndDoesNotThrow() {
        val validNotif1 = Notification(
            id = "notif_1",
            title = "Offer received",
            type = NotificationType.NEW_OFFER,
        )
        val validNotif2 = Notification(
            id = "notif_3",
            title = "Repair ready",
            type = NotificationType.REPAIR_UPDATE,
        )

        val doc1 = FakeDocumentSnapshot.create("notif_1", validNotif1)
        val doc2 = FakeDocumentSnapshot.create(
            docId = "notif_2",
            value = null,
            failure = RuntimeException(
                "Could not deserialize object. Could not find enum value of com.homesajja.app.data.model.NotificationType for value \"PURCHASE_REQUEST_RECEIVED\" (found in field 'type')",
            ),
        )
        val doc3 = FakeDocumentSnapshot.create("notif_3", validNotif2)

        val documents: List<DocumentSnapshot> = listOf(doc1, doc2, doc3)

        // Must not throw, skips doc2 with warning, returns doc1 and doc3
        val result = documents.mapDocumentsSafe<Notification>()

        assertEquals(2, result.size)
        assertEquals("notif_1", result[0].id)
        assertEquals(NotificationType.NEW_OFFER, result[0].type)
        assertEquals("notif_3", result[1].id)
        assertEquals(NotificationType.REPAIR_UPDATE, result[1].type)
    }
}
