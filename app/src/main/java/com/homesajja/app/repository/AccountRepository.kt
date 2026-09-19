package com.homesajja.app.repository

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/** Why an account couldn't be deleted, in terms the person can act on. */
enum class DeleteAccountFailure { NOT_SIGNED_IN, WRONG_PASSWORD, LOGIN_AGAIN }

class DeleteAccountException(val failure: DeleteAccountFailure, cause: Throwable? = null) : Exception(failure.name, cause)

private const val RECENT_LOGIN_MILLIS = 4 * 60 * 1000L
private const val CLEANUP_BATCH = 100L

/**
 * Deletes a person's HomeSajja account: their sign-in, their profile and the things that are theirs alone (listings,
 * saved items, blocks, notifications, material requests, this phone's push token). Records shared with another person
 * (requests, chats, reviews) stay, because the other side still needs them; the privacy policy says so.
 */
class AccountRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val deviceTokenRepository: DeviceTokenRepository,
    private val sessionRepository: SessionRepository,
) {
    /** True for email-and-password accounts, which confirm deletion with the password; Google accounts sign in again instead. */
    val usesPassword: Boolean
        get() = auth.currentUser?.providerData?.any { it.providerId == EmailAuthProvider.PROVIDER_ID } == true

    /**
     * [password] is required for email accounts. Everything is checked before anything is deleted, so a wrong password
     * or an old login leaves the account exactly as it was.
     */
    suspend fun deleteAccount(password: String?) {
        val user = auth.currentUser ?: throw DeleteAccountException(DeleteAccountFailure.NOT_SIGNED_IN)
        confirmItIsThem(user, password)

        val uid = user.uid
        deleteWhere("listings", "ownerId", uid)
        deleteWhere("favourites", "userId", uid)
        deleteWhere("blocks", "blockerId", uid)
        deleteWhere("notifications", "recipientId", uid)
        deleteWhere("materialRequests", "vendorId", uid)
        deleteWhere("deviceTokens", "userId", uid)
        runCatching { deviceTokenRepository.unregisterCurrentToken() }
        // Only one of these exists (a person is a user or a vendor, never both).
        runCatching { firestore.collection("users").document(uid).delete().await() }
        runCatching { firestore.collection("vendors").document(uid).delete().await() }

        user.delete().await()
        sessionRepository.clearSession()
    }

    private suspend fun confirmItIsThem(user: FirebaseUser, password: String?) {
        if (usesPassword) {
            val email = user.email
            if (password.isNullOrEmpty() || email == null) throw DeleteAccountException(DeleteAccountFailure.WRONG_PASSWORD)
            try {
                user.reauthenticate(EmailAuthProvider.getCredential(email, password)).await()
            } catch (e: Exception) {
                throw DeleteAccountException(DeleteAccountFailure.WRONG_PASSWORD, e)
            }
        } else {
            // Google sign-in can't be re-checked here, so it must have been done a moment ago.
            val signedInAt = user.metadata?.lastSignInTimestamp ?: 0L
            if (System.currentTimeMillis() - signedInAt > RECENT_LOGIN_MILLIS) throw DeleteAccountException(DeleteAccountFailure.LOGIN_AGAIN)
        }
    }

    /** Deletes every document in [collection] whose [field] equals [value], a batch at a time. Best effort per batch. */
    private suspend fun deleteWhere(collection: String, field: String, value: String) {
        try {
            while (true) {
                val batch = firestore.collection(collection).whereEqualTo(field, value).limit(CLEANUP_BATCH).get().await()
                if (batch.isEmpty) return
                val writes = firestore.batch()
                batch.documents.forEach { writes.delete(it.reference) }
                writes.commit().await()
                if (batch.size() < CLEANUP_BATCH) return
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            // Something in this group couldn't be removed; carry on with the rest.
        }
    }
}
