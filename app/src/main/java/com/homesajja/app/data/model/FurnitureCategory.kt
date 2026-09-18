package com.homesajja.app.data.model

/** Shared by listings and by repair/recycling requests, so all four systems
 * describe furniture with the same vocabulary. */
enum class FurnitureCategory(val displayName: String) {
    SOFA("Sofa"),
    BED("Bed"),
    TABLE("Table"),
    CHAIR("Chair"),
    WARDROBE("Wardrobe"),
    STORAGE("Storage"),
    DESK("Desk"),
    OTHER("Other"),
}
