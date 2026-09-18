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

/** Stored at `listings/{id}`. Prices are whole rupees; [city] drives discovery. */
data class FurnitureListing(
    val id: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val title: String = "",
    val description: String = "",
    val images: List<String> = emptyList(),
    val price: Long = 0L,
    val condition: FurnitureCondition = FurnitureCondition.GOOD,
    val category: FurnitureCategory = FurnitureCategory.OTHER,
    val actionType: ListingActionType = ListingActionType.SELL,
    val city: String = "",
    val status: ListingStatus = ListingStatus.ACTIVE,
    val aiAnalysis: AiAnalysis? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
