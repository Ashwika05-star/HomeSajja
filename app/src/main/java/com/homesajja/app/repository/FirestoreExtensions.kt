package com.homesajja.app.repository

import android.util.Log
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

internal const val DEFAULT_PAGE_SIZE = 50
private const val TAG = "FirestoreExtensions"

/** Safely maps a list of [DocumentSnapshot]s to [T], logging a warning with the document id and error, and skipping any failing document. */
internal inline fun <reified T : Any> List<DocumentSnapshot>.mapDocumentsSafe(): List<T> =
    mapNotNull { doc ->
        runCatching { doc.toObject(T::class.java) }.getOrElse { e ->
            Log.w(TAG, "Failed to deserialize document ${doc.id} as ${T::class.java.simpleName}: ${e.message}", e)
            null
        }
    }

/** Reads one document as [T], or null if it doesn't exist. */
internal suspend inline fun <reified T : Any> DocumentReference.getAs(): T? =
    get().await().let { doc ->
        runCatching { doc.toObject(T::class.java) }.getOrElse { e ->
            Log.w(TAG, "Failed to deserialize document ${doc.id} as ${T::class.java.simpleName}: ${e.message}", e)
            null
        }
    }

/** Runs a one-shot query and maps every document to [T]. */
internal suspend inline fun <reified T : Any> Query.getAllAs(): List<T> =
    get().await().documents.mapDocumentsSafe()

/** Real-time query results as a Flow; the listener is removed when collection stops. */
internal inline fun <reified T : Any> Query.observeAs(): Flow<List<T>> = callbackFlow {
    val registration = addSnapshotListener { snapshot, error ->
        if (error != null) {
            close(error)
        } else if (snapshot != null) {
            trySend(snapshot.documents.mapDocumentsSafe())
        }
    }
    awaitClose { registration.remove() }
}
