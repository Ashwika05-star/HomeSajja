package com.homesajja.app.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.homesajja.app.data.model.UserProfile
import kotlinx.coroutines.tasks.await

private const val COLLECTION = "users"

/** `users/{uid}` — private to the account owner, so there is no list query. */
class UserRepository(firestore: FirebaseFirestore) {

    private val users = firestore.collection(COLLECTION)

    suspend fun createUserProfile(profile: UserProfile) {
        users.document(profile.uid).set(profile).await()
    }

    suspend fun getUserProfile(uid: String): UserProfile? = users.document(uid).getAs()

    suspend fun updateUserProfile(profile: UserProfile) {
        users.document(profile.uid).set(profile).await()
    }

    suspend fun deleteUserProfile(uid: String) {
        users.document(uid).delete().await()
    }
}
