package com.homesajja.app.data.model

/** Stored at `vendors/{uid}`. [businessType] stores [VendorBusinessType.name] so
 * display strings can change later without touching stored data. */
data class VendorProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val businessName: String = "",
    val businessType: String = "",
    val city: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)
