package com.homesajja.app.repository

import com.homesajja.app.data.model.ChatbotReply
import com.homesajja.app.data.model.FurnitureAssessment
import com.homesajja.app.data.model.FurnitureCategory
import com.homesajja.app.data.model.Recommendation
import com.homesajja.app.data.model.RecycleCondition
import com.homesajja.app.data.model.RecycleMaterial
import com.homesajja.app.data.model.RepairProblemType
import org.json.JSONException
import org.json.JSONObject

/** Thrown when Gemini answered but the answer can't be used. */
class AiResponseException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Turns Gemini's JSON answers into app types. Gemini is asked for JSON, but nothing is trusted: unknown words are
 * dropped (or the whole answer rejected if the essential parts are missing), so a strange answer can never crash a screen.
 */
object AiParsing {

    /** Reads the assessment JSON. Needs a known recommendation and some reasoning; everything else is optional. */
    fun parseAssessment(json: String): FurnitureAssessment {
        val obj = try {
            JSONObject(stripCodeFence(json))
        } catch (e: JSONException) {
            throw AiResponseException("The answer wasn't valid JSON", e)
        }
        val recommendation = enumOrNull<Recommendation>(obj.optString("recommendation"))
            ?: throw AiResponseException("No usable recommendation in the answer")
        val reasoning = obj.optString("reasoning").trim()
        if (reasoning.isEmpty()) throw AiResponseException("No reasoning in the answer")

        var priceMin = obj.optLongOrNull("priceMin")
        var priceMax = obj.optLongOrNull("priceMax")
        if (recommendation != Recommendation.SELL && recommendation != Recommendation.EXCHANGE) {
            priceMin = null
            priceMax = null
        } else if (priceMin != null && priceMax != null && priceMin > priceMax) {
            priceMin = priceMax.also { priceMax = priceMin }
        }
        return FurnitureAssessment(
            recommendation = recommendation,
            reasoning = reasoning,
            category = enumOrNull<FurnitureCategory>(obj.optString("category")),
            material = enumOrNull<RecycleMaterial>(obj.optString("material")),
            recycleCondition = enumOrNull<RecycleCondition>(obj.optString("recycleCondition")),
            problemType = enumOrNull<RepairProblemType>(obj.optString("problemType")),
            priceMin = priceMin?.takeIf { it >= 0 },
            priceMax = priceMax?.takeIf { it >= 0 },
        )
    }

    /** Reads a chatbot answer. Only ids in [knownListingIds] survive, at most [maxListings] of them. */
    fun parseChatbotReply(json: String, knownListingIds: Set<String>, maxListings: Int = 4): ChatbotReply {
        val obj = try {
            JSONObject(stripCodeFence(json))
        } catch (e: JSONException) {
            // Not JSON after all: treat the whole thing as the reply text.
            return ChatbotReply(json.trim().ifEmpty { throw AiResponseException("Empty answer") })
        }
        val text = obj.optString("reply").trim()
        if (text.isEmpty()) throw AiResponseException("Empty answer")
        val ids = obj.optJSONArray("listingIds")?.let { array -> (0 until array.length()).map { array.optString(it) } }.orEmpty()
        return ChatbotReply(text, ids.filter { it in knownListingIds }.distinct().take(maxListings))
    }

    /** Models sometimes wrap JSON in a ```json fence even when told not to. */
    fun stripCodeFence(text: String): String =
        text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

    private inline fun <reified T : Enum<T>> enumOrNull(name: String): T? =
        enumValues<T>().firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }

    private fun JSONObject.optLongOrNull(key: String): Long? =
        if (has(key) && !isNull(key)) optDouble(key, Double.NaN).takeIf { !it.isNaN() }?.toLong() else null
}
