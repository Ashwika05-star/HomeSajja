package com.homesajja.app.viewmodel

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

/** Maps the Firebase/Firestore exceptions this app can hit during auth into
 * short, user-facing messages instead of raw exception text. */
fun mapAuthError(throwable: Throwable): String = when (throwable) {
    is FirebaseAuthWeakPasswordException -> "Password is too weak. Use at least 6 characters."
    is FirebaseAuthInvalidCredentialsException -> "That email address or password looks invalid."
    is FirebaseAuthUserCollisionException -> "An account already exists with this email."
    is FirebaseAuthInvalidUserException -> "No account found for that email."
    is FirebaseNetworkException -> "Network error. Check your connection and try again."
    else -> throwable.message ?: "Something went wrong. Please try again."
}
