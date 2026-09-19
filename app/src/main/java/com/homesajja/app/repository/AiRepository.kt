package com.homesajja.app.repository

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.homesajja.app.data.model.ChatbotReply
import com.homesajja.app.data.model.FurnitureAssessment
import com.homesajja.app.data.model.FurnitureCategory
import com.homesajja.app.data.model.FurnitureListing
import com.homesajja.app.data.model.Recommendation
import com.homesajja.app.data.model.RecycleCondition
import com.homesajja.app.data.model.RecycleMaterial
import com.homesajja.app.data.model.RepairProblemType
import kotlinx.coroutines.CancellationException

/** Why an AI request failed, in terms a person can act on. */
enum class AiFailure { NOT_AVAILABLE, BUSY, NO_CONNECTION, BAD_ANSWER, OTHER }

class AiException(val failure: AiFailure, cause: Throwable? = null) : Exception(failure.name, cause)

/** One line of the conversation so far. */
data class ChatTurn(val fromUser: Boolean, val text: String)

/** The real listings the chatbot may point to, plus where the person is. */
data class ChatbotContext(val city: String, val listings: List<FurnitureListing>)

interface AiRepository {
    /** Looks at a photo and the person's notes and recommends what to do with the furniture. */
    suspend fun assessFurniture(photo: Uri, notes: String, ageYears: Int?, city: String): FurnitureAssessment

    /** Answers the newest message of a conversation, aware of the person's city and the listings around them. */
    suspend fun chat(history: List<ChatTurn>, message: String, context: ChatbotContext): ChatbotReply
}

private const val TAG = "HomeSajjaAi"
private const val PHOTO_EDGE_PX = 1024
private const val CONTEXT_LISTINGS = 25

/**
 * Talks to Gemini through Firebase AI Logic. The Gemini key never reaches the app: Firebase holds it and only
 * lets this app call the model. Every failure is turned into an [AiException], so screens can carry on without AI.
 */
class GeminiAiRepository(
    private val contentResolver: ContentResolver,
    private val modelName: String,
) : AiRepository {

    private val backend get() = Firebase.ai(backend = GenerativeBackend.googleAI())

    override suspend fun assessFurniture(photo: Uri, notes: String, ageYears: Int?, city: String): FurnitureAssessment =
        guarded {
            val bitmap = decodeScaledBitmap(contentResolver, photo, PHOTO_EDGE_PX) ?: throw AiException(AiFailure.OTHER)
            val model = backend.generativeModel(
                modelName = modelName,
                generationConfig = generationConfig {
                    responseMimeType = "application/json"
                    responseSchema = ASSESSMENT_SCHEMA
                },
                systemInstruction = content { text(ASSESSMENT_INSTRUCTION) },
            )
            val response = model.generateContent(
                content {
                    image(bitmap)
                    text(
                        buildString {
                            append("City: $city.\n")
                            ageYears?.let { append("Age: about $it years.\n") }
                            if (notes.isNotBlank()) append("Owner's notes: ${notes.trim()}\n")
                            append("What should the owner do with this furniture?")
                        },
                    )
                },
            )
            AiParsing.parseAssessment(response.text.orEmpty())
        }

    override suspend fun chat(history: List<ChatTurn>, message: String, context: ChatbotContext): ChatbotReply =
        guarded {
            val shown = context.listings.take(CONTEXT_LISTINGS)
            val model = backend.generativeModel(
                modelName = modelName,
                generationConfig = generationConfig {
                    responseMimeType = "application/json"
                    responseSchema = CHAT_SCHEMA
                },
                systemInstruction = content { text(chatInstruction(context.city, shown)) },
            )
            val chat = model.startChat(
                history = history.map { turn -> content(if (turn.fromUser) "user" else "model") { text(turn.text) } },
            )
            val response = chat.sendMessage(message)
            AiParsing.parseChatbotReply(response.text.orEmpty(), shown.map { it.id }.toSet())
        }

    /** Runs [block], turning anything that goes wrong into an [AiException]. */
    private suspend fun <T> guarded(block: suspend () -> T): T = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: AiException) {
        throw e
    } catch (e: AiResponseException) {
        throw AiException(AiFailure.BAD_ANSWER, e)
    } catch (e: Exception) {
        // Kept for diagnosing setup problems (e.g. AI Logic not enabled in the Firebase console); shows nothing to the person.
        Log.w(TAG, "Gemini request failed: ${e::class.java.simpleName}: ${e.message}")
        throw AiException(classify(e), e)
    }

    /** Firebase AI's exception classes are matched by name so a rename can't break the build. */
    private fun classify(e: Throwable): AiFailure {
        val name = e::class.java.simpleName
        val text = (e.message.orEmpty() + " " + generateSequence(e.cause) { it.cause }.joinToString(" ") { it.message.orEmpty() }).lowercase()
        return when {
            "Quota" in name || "quota" in text || "429" in text || "rate limit" in text -> AiFailure.BUSY
            "ServiceDisabled" in name || "ApiNotEnabled" in name || "PermissionDenied" in name ||
                "not enabled" in text || "has not been used" in text || "403" in text || "api key" in text -> AiFailure.NOT_AVAILABLE
            "UnknownHost" in name || "Timeout" in name || "unable to resolve host" in text || "network" in text -> AiFailure.NO_CONNECTION
            else -> AiFailure.OTHER
        }
    }

    private companion object {
        val ASSESSMENT_SCHEMA = Schema.obj(
            mapOf(
                "recommendation" to Schema.enumeration(Recommendation.entries.map { it.name }),
                "reasoning" to Schema.string(),
                "category" to Schema.enumeration(FurnitureCategory.entries.map { it.name }),
                "material" to Schema.enumeration(RecycleMaterial.entries.map { it.name }),
                "recycleCondition" to Schema.enumeration(RecycleCondition.entries.map { it.name }),
                "problemType" to Schema.enumeration(RepairProblemType.entries.map { it.name }),
                "priceMin" to Schema.integer(),
                "priceMax" to Schema.integer(),
            ),
            optionalProperties = listOf("category", "material", "recycleCondition", "problemType", "priceMin", "priceMax"),
        )

        val CHAT_SCHEMA = Schema.obj(
            mapOf(
                "reply" to Schema.string(),
                "listingIds" to Schema.array(Schema.string()),
            ),
            optionalProperties = listOf("listingIds"),
        )

        val ASSESSMENT_INSTRUCTION = """
            You are HomeSajja's furniture advisor for people in India. Look at the photo and the owner's notes,
            judge the condition, and recommend exactly one action: SELL (fine to sell as it is), REPAIR (worth fixing first),
            EXCHANGE (better swapped for something else) or RECYCLE (too damaged to sell or fix).
            Give one or two short, friendly sentences of reasoning in "reasoning".
            Fill "category" and "material" with the closest match. For REPAIR fill "problemType" with the main problem.
            For RECYCLE fill "recycleCondition" (REPAIRABLE, REUSABLE or BEYOND_REPAIR).
            For SELL or EXCHANGE give a realistic second-hand price range in Indian rupees as whole numbers in "priceMin" and "priceMax".
            Leave price fields out for REPAIR and RECYCLE. If the photo doesn't show furniture, still answer with your best guess and say so in "reasoning".
        """.trimIndent()

        fun chatInstruction(city: String, listings: List<FurnitureListing>): String = buildString {
            appendLine("You are Sajja, HomeSajja's friendly furniture assistant. HomeSajja lets people buy, sell, exchange, repair and recycle furniture.")
            appendLine("Answer briefly (at most four sentences) and plainly. Prices are in Indian rupees. The person lives in $city.")
            appendLine("Give practical advice about choosing, pricing, repairing or recycling furniture and about matching furniture to a room.")
            appendLine("If they want to find furniture, look ONLY at the listings below. Never invent a listing.")
            appendLine("Put the ids of the listings that match into \"listingIds\" (at most 4), otherwise leave it empty. Put your answer in \"reply\".")
            appendLine("Listings currently for sale or exchange in $city:")
            if (listings.isEmpty()) appendLine("(none right now)")
            listings.forEach {
                appendLine("- id=${it.id} | ${it.title} | ${it.category.displayName} | ${it.condition.displayName} | ${if (it.price > 0) "₹${it.price}" else "for exchange"}")
            }
        }
    }
}
