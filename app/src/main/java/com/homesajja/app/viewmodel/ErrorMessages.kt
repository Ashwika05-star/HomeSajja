package com.homesajja.app.viewmodel

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.homesajja.app.repository.AiException
import com.homesajja.app.repository.AiFailure
import com.homesajja.app.repository.ImageUploadException
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
    throwable is AiException -> when (throwable.failure) {
        AiFailure.NOT_ENABLED -> "HomeSajja's AI isn't switched on yet. It needs to be enabled for this app in Firebase (AI Logic)."
        AiFailure.QUOTA_EXCEEDED -> "The free AI limit has been reached for now. Please try again in a little while."
        AiFailure.NO_NETWORK -> "No internet connection. Check your network and try again."
        AiFailure.MODEL_UNAVAILABLE -> "The AI model isn't available for this project right now."
        AiFailure.BUSY -> "The AI is very busy right now. Please try again in a moment."
        AiFailure.BLOCKED -> "The AI couldn't answer that one. Try rewording it."
        AiFailure.BAD_ANSWER, AiFailure.OTHER -> fallback
    }
    throwable is ImageUploadException -> throwable.message ?: "Upload failed. Please try again."
    else -> fallback
}
