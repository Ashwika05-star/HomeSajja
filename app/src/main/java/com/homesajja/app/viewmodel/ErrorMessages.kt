package com.homesajja.app.viewmodel

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.storage.StorageException
import com.homesajja.app.repository.ItemUnavailableException

/** Turns Firebase failures into short messages for Error states and snackbars.
 * [fallback] describes what was being attempted, e.g. "Couldn't load listings". */
fun mapError(throwable: Throwable, fallback: String): String = when {
    throwable is ItemUnavailableException -> throwable.message ?: fallback
    throwable is FirebaseNetworkException -> "Network error. Check your connection and try again."
    throwable is FirebaseFirestoreException && throwable.code == FirebaseFirestoreException.Code.UNAVAILABLE ->
        "Network error. Check your connection and try again."
    throwable is FirebaseFirestoreException && throwable.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
        "You don't have permission to do that."
    throwable is StorageException && throwable.errorCode == StorageException.ERROR_RETRY_LIMIT_EXCEEDED ->
        "Upload failed. Check your connection and try again."
    else -> fallback
}
