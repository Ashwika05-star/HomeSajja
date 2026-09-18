package com.homesajja.app.data.model

enum class VendorBusinessType(val displayName: String) {
    SHOP("Shop"),
    CARPENTER("Carpenter"),
    REPAIR_PROFESSIONAL("Repair Professional"),
    REFURBISHER("Refurbisher"),
    RECYCLER("Recycler");

    companion object {
        fun fromNameOrNull(name: String): VendorBusinessType? =
            entries.firstOrNull { it.name == name }
    }
}
