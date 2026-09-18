package com.homesajja.app.data.model

/** Stored at `users/{uid}`. Kept separate from [com.homesajja.app.data.model.VendorProfile] —
 * see the Phase 2 data-model note: role is permanent and the two roles share
 * almost no fields, so a flag-on-shared-doc approach would just add nullable
 * fields instead of removing complexity. */
data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val city: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)
