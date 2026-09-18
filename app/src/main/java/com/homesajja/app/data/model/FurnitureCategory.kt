package com.homesajja.app.data.model

/** Shared by listings and by repair/recycling requests, so all four systems
 * describe furniture with the same vocabulary. Declaration order is the order
 * shown in the UI. */
enum class FurnitureCategory(val displayName: String) {
    SOFA("Sofa"),
    BED("Bed"),
    TABLE("Table"),
    CHAIR("Chair"),
    DESK("Desk"),
    WARDROBE("Wardrobe"),
    STORAGE("Storage"),
    BOOKSHELF("Bookshelf"),
    TV_UNIT("TV Unit"),
    COFFEE_TABLE("Coffee Table"),
    RECLINER("Recliner"),
    OTHER("Other"),
}
