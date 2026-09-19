package com.homesajja.app.data.model

/** Stored at `vendors/{uid}`. [businessType] stores [VendorBusinessType.name] so
 * display strings can change later without touching stored data. The shop
 * location is picked on a map in the profile editor, so it starts empty. */
data class VendorProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val businessName: String = "",
    val businessType: String = "",
    val city: String = "",
    val shopAddress: String = "",
    val description: String = "",
    /** Placeholder badge: an admin flips it in the Firebase console; vendors cannot set it themselves. */
    val verified: Boolean = false,
    /** Catalogue / brochure photo URLs (Cloudinary). */
    val brochureImages: List<String> = emptyList(),
    val shopLatitude: Double? = null,
    val shopLongitude: Double? = null,
    /** Optional repair details shown on provider cards: [RepairProblemType] names and a cost range in rupees. */
    val repairServices: List<String> = emptyList(),
    val repairCostMin: Long? = null,
    val repairCostMax: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
