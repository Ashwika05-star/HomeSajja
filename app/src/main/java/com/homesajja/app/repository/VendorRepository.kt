package com.homesajja.app.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.homesajja.app.data.model.VendorProfile
import kotlinx.coroutines.tasks.await

private const val COLLECTION = "vendors"

class VendorRepository(private val firestore: FirebaseFirestore) {

    suspend fun createVendorProfile(profile: VendorProfile) {
        firestore.collection(COLLECTION).document(profile.uid).set(profile).await()
    }

    suspend fun getVendorProfile(uid: String): VendorProfile? {
        val snapshot = firestore.collection(COLLECTION).document(uid).get().await()
        return snapshot.toObject(VendorProfile::class.java)
    }
}
