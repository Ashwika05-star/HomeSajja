package com.homesajja.app.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.homesajja.app.data.model.UserProfile
import kotlinx.coroutines.tasks.await

private const val COLLECTION = "users"

class UserRepository(private val firestore: FirebaseFirestore) {

    suspend fun createUserProfile(profile: UserProfile) {
        firestore.collection(COLLECTION).document(profile.uid).set(profile).await()
    }

    suspend fun getUserProfile(uid: String): UserProfile? {
        val snapshot = firestore.collection(COLLECTION).document(uid).get().await()
        return snapshot.toObject(UserProfile::class.java)
    }
}
