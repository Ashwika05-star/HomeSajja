package com.homesajja.app.data.model

enum class VendorBusinessType(val displayName: String) {
    SHOP("Shop"),
    CARPENTER("Carpenter"),
    REPAIR_PROFESSIONAL("Repair Professional"),
    REFURBISHER("Refurbisher"),
    RECYCLER("Recycler");

    companion object {
        /** Vendors who can take a repair job: repair professionals, and carpenters who also repair. */
        val REPAIR_PROVIDERS = listOf(REPAIR_PROFESSIONAL, CARPENTER)

        fun fromNameOrNull(name: String): VendorBusinessType? =
            entries.firstOrNull { it.name == name }
    }
}
