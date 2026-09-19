package com.homesajja.app.data.model

/** Stored at `vendors/{uid}`. [businessType] stores [VendorBusinessType.name] so
 * display strings can change later without touching stored data. The shop
 * location is filled in later via Google Maps/Places, so it starts empty. */
data class VendorProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val businessName: String = "",
    val businessType: String = "",
    val city: String = "",
    val shopAddress: String = "",
    val shopLatitude: Double? = null,
    val shopLongitude: Double? = null,
    /** Optional repair details shown on provider cards: [RepairProblemType] names and a cost range in rupees. */
    val repairServices: List<String> = emptyList(),
    val repairCostMin: Long? = null,
    val repairCostMax: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
