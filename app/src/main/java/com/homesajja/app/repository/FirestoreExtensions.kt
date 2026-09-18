package com.homesajja.app.repository

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

internal const val DEFAULT_PAGE_SIZE = 50

/** Reads one document as [T], or null if it doesn't exist. */
internal suspend inline fun <reified T : Any> DocumentReference.getAs(): T? =
    get().await().toObject(T::class.java)

/** Runs a one-shot query and maps every document to [T]. */
internal suspend inline fun <reified T : Any> Query.getAllAs(): List<T> =
    get().await().toObjects(T::class.java)

/** Real-time query results as a Flow; the listener is removed when collection stops. */
internal inline fun <reified T : Any> Query.observeAs(): Flow<List<T>> = callbackFlow {
    val registration = addSnapshotListener { snapshot, error ->
        if (error != null) {
            close(error)
        } else if (snapshot != null) {
            trySend(snapshot.toObjects(T::class.java))
        }
    }
    awaitClose { registration.remove() }
}
