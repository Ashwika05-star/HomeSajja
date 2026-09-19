package com.homesajja.app.data.model

/** The four things HomeSajja can suggest for a piece of furniture. */
enum class Recommendation(val displayName: String, val verb: String) {
    SELL("Sell", "Sell it"),
    REPAIR("Repair", "Get it repaired"),
    EXCHANGE("Exchange", "Exchange it"),
    RECYCLE("Recycle", "Recycle it"),
}

/** What Gemini thinks of a piece of furniture. Everything except [recommendation] and [reasoning] is optional. */
data class FurnitureAssessment(
    val recommendation: Recommendation,
    val reasoning: String,
    val category: FurnitureCategory? = null,
    val material: RecycleMaterial? = null,
    val recycleCondition: RecycleCondition? = null,
    val problemType: RepairProblemType? = null,
    /** Suggested price range in rupees, given for Sell and Exchange. */
    val priceMin: Long? = null,
    val priceMax: Long? = null,
)

/** A reply from the Sajja chatbot. [listingIds] are real listings it points to; only ids it was actually shown are kept. */
data class ChatbotReply(val text: String, val listingIds: List<String> = emptyList())

/**
 * What the Smart Decision screen hands to the Sell, Repair or Recycle flow so it can start pre-filled.
 * [photo] is the picture the person already chose, so they don't have to pick it again.
 */
data class FlowPrefill(
    val target: Recommendation,
    val photo: android.net.Uri?,
    /** Null when the person chose a flow themselves without (or after failing) an AI suggestion. */
    val assessment: FurnitureAssessment?,
)
