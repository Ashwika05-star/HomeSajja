package com.homesajja.app.data.model

enum class ListingActionType(val displayName: String) {
    SELL("For sale"),
    EXCHANGE("For exchange"),
}

/** ACTIVE -> RESERVED -> SOLD / EXCHANGED; the owner can also set REMOVED. */
enum class ListingStatus(val displayName: String) {
    ACTIVE("Active"),
    RESERVED("Reserved"),
    SOLD("Sold"),
    EXCHANGED("Exchanged"),
    REMOVED("Removed"),
}

/** Who is selling: a person, or a vendor account. There is no verification step
 * yet, so every vendor account counts as verified. */
enum class SellerType(val displayName: String) {
    INDIVIDUAL("Individual"),
    VENDOR("Verified Vendor"),
}

/** Centimetres; any side the seller didn't fill in stays null. */
data class FurnitureDimensions(
    val lengthCm: Int? = null,
    val widthCm: Int? = null,
    val heightCm: Int? = null,
)

/** Filled by the Gemini Cloud Function in a later phase; null until analysed. */
data class AiAnalysis(
    val detectedCategory: FurnitureCategory? = null,
    val estimatedCondition: FurnitureCondition? = null,
    val suggestedPriceMin: Long? = null,
    val suggestedPriceMax: Long? = null,
    val repairSuggestions: List<String> = emptyList(),
    val summary: String = "",
    val analyzedAt: Long = 0L,
)

/** Stored at `listings/{id}`. Prices are whole rupees; [city] drives discovery.
 * For [ListingActionType.EXCHANGE] listings [price] is an optional estimated value (0 = not given). */
data class FurnitureListing(
    val id: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val title: String = "",
    val description: String = "",
    val images: List<String> = emptyList(),
    val price: Long = 0L,
    val condition: FurnitureCondition = FurnitureCondition.GOOD,
    val refurbished: Boolean = false,
    val category: FurnitureCategory = FurnitureCategory.OTHER,
    val material: MaterialType = MaterialType.OTHER,
    val ageYears: Int = 0,
    val dimensions: FurnitureDimensions = FurnitureDimensions(),
    val sellerType: SellerType = SellerType.INDIVIDUAL,
    val actionType: ListingActionType = ListingActionType.SELL,
    val city: String = "",
    val status: ListingStatus = ListingStatus.ACTIVE,
    val aiAnalysis: AiAnalysis? = null,
    /** The accepted exchange this listing is part of, if any. It is what lets the other party
     * of that exchange update this listing's status (see firestore.rules). */
    val exchangeRequestId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
