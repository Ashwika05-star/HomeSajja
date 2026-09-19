package com.homesajja.app.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.homesajja.app.data.model.VendorBusinessType
import com.homesajja.app.data.model.VendorProfile
import kotlinx.coroutines.tasks.await

private const val COLLECTION = "vendors"

/** `vendors/{uid}` — readable by any signed-in user so vendors can be discovered. */
class VendorRepository(firestore: FirebaseFirestore) {

    private val vendors = firestore.collection(COLLECTION)

    suspend fun createVendorProfile(profile: VendorProfile) {
        vendors.document(profile.uid).set(profile).await()
    }

    suspend fun getVendorProfile(uid: String): VendorProfile? = vendors.document(uid).getAs()

    /** City-scoped discovery, optionally narrowed to one business type. */
    suspend fun getVendors(
        city: String,
        businessType: VendorBusinessType? = null,
        limit: Int = DEFAULT_PAGE_SIZE,
    ): List<VendorProfile> {
        var query: Query = vendors.whereEqualTo("city", city)
        businessType?.let { query = query.whereEqualTo("businessType", it.name) }
        return query.limit(limit.toLong()).getAllAs()
    }

    /** City-scoped vendors of any of [types]. */
    suspend fun getVendorsByTypes(
        city: String,
        types: List<VendorBusinessType>,
        limit: Int = DEFAULT_PAGE_SIZE,
    ): List<VendorProfile> =
        vendors.whereEqualTo("city", city)
            .whereIn("businessType", types.map { it.name })
            .limit(limit.toLong())
            .getAllAs()

    suspend fun updateVendorProfile(profile: VendorProfile) {
        vendors.document(profile.uid).set(profile).await()
    }

    suspend fun deleteVendorProfile(uid: String) {
        vendors.document(uid).delete().await()
    }
}
