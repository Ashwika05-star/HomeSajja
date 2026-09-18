package com.homesajja.app.data.model

/** The "new / used / refurbished" facet. Derived from a listing, not stored. */
enum class ItemState(val displayName: String) {
    NEW("New"),
    USED("Used"),
    REFURBISHED("Refurbished"),
}

fun FurnitureListing.itemState(): ItemState = when {
    refurbished -> ItemState.REFURBISHED
    condition == FurnitureCondition.NEW -> ItemState.NEW
    else -> ItemState.USED
}

/**
 * Filters the Explore screen applies on the device. Firestore can only combine so
 * many range/array filters with its ordering, so only city, category and status
 * are queried server-side; everything here narrows the fetched listings.
 * A null bound or an empty set means "no restriction".
 */
data class ListingFilters(
    val minPrice: Long? = null,
    val maxPrice: Long? = null,
    val conditions: Set<FurnitureCondition> = emptySet(),
    val materials: Set<MaterialType> = emptySet(),
    val itemState: ItemState? = null,
) {
    val isActive: Boolean
        get() = minPrice != null || maxPrice != null || conditions.isNotEmpty() ||
            materials.isNotEmpty() || itemState != null

    fun matches(listing: FurnitureListing): Boolean =
        (minPrice == null || listing.price >= minPrice) &&
            (maxPrice == null || listing.price <= maxPrice) &&
            (conditions.isEmpty() || listing.condition in conditions) &&
            (materials.isEmpty() || listing.material in materials) &&
            (itemState == null || listing.itemState() == itemState)

    companion object {
        /** Upper end of the price slider; a maxPrice at this value means "no upper limit". */
        const val PRICE_SLIDER_MAX = 100_000L
    }
}

/** Case-insensitive match on title, description or category name. Blank matches everything. */
fun FurnitureListing.matchesSearch(query: String): Boolean {
    val needle = query.trim()
    if (needle.isEmpty()) return true
    return title.contains(needle, ignoreCase = true) ||
        description.contains(needle, ignoreCase = true) ||
        category.displayName.contains(needle, ignoreCase = true)
}
