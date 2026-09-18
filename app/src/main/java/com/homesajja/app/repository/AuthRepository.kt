package com.homesajja.app.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.tasks.await

/** Thin wrapper around Firebase Authentication. Returns [Result] instead of
 * throwing so ViewModels can map failures to user-facing messages in one place. */
class AuthRepository(private val firebaseAuth: FirebaseAuth) {

    val currentUserId: String?
        get() = firebaseAuth.currentUser?.uid

    val currentUserDisplayName: String?
        get() = firebaseAuth.currentUser?.displayName

    val currentUserEmail: String?
        get() = firebaseAuth.currentUser?.email

    fun isSignedIn(): Boolean = firebaseAuth.currentUser != null

    suspend fun signUpWithEmail(name: String, email: String, password: String): Result<String> = runCatching {
        val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        val user = result.user ?: error("Signup succeeded but no user was returned.")
        user.updateProfile(userProfileChangeRequest { displayName = name }).await()
        user.uid
    }

    suspend fun signInWithEmail(email: String, password: String): Result<String> = runCatching {
        val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
        result.user?.uid ?: error("Login succeeded but no user was returned.")
    }

    suspend fun signInWithGoogle(idToken: String): Result<String> = runCatching {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = firebaseAuth.signInWithCredential(credential).await()
        result.user?.uid ?: error("Google sign-in succeeded but no user was returned.")
    }

    fun signOut() {
        firebaseAuth.signOut()
    }
}
