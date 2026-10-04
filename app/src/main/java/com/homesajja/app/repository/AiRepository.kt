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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Why an AI request failed, in terms a person (or whoever sets the project up) can act on. */
enum class AiFailure {
    /** AI Logic isn't switched on for the Firebase project (or the key isn't allowed to use it). */
    NOT_ENABLED,

    /** The free quota or rate limit is used up for now. */
    QUOTA_EXCEEDED,

    NO_NETWORK,

    /** The configured Gemini model name doesn't exist or isn't available to this project. */
    MODEL_UNAVAILABLE,

    /** Gemini is temporarily overloaded ("high demand", 503); trying again or another model usually works. */
    BUSY,

    /** Gemini refused the request or the answer (safety filters). */
    BLOCKED,

    /** Gemini answered, but not in a usable shape. */
    BAD_ANSWER,

    OTHER,
}

/**
 * Sorts whatever the Firebase AI SDK (or the network) threw into an [AiFailure]. It looks at the exception's class name and its
 * message chain rather than importing every SDK exception type, so a rename in the SDK can't break the build. Pure, so it is unit-tested.
 */
fun classifyAiFailure(error: Throwable): AiFailure {
    val chain = generateSequence(error) { it.cause }.take(6).toList()
    val names = chain.map { it::class.java.simpleName }
    val text = chain.joinToString(" ") { it.message.orEmpty() }.lowercase()
    fun hasName(vararg parts: String) = names.any { name -> parts.any { it in name } }
    return when {
        hasName("QuotaExceeded") || "quota" in text || "429" in text || "resource_exhausted" in text || "rate limit" in text -> AiFailure.QUOTA_EXCEEDED
        "high demand" in text || "overloaded" in text || "503" in text || "service unavailable" in text -> AiFailure.BUSY
        hasName("UnknownHost", "ConnectException", "SocketTimeout", "RequestTimeout", "SSLException") ||
            "unable to resolve host" in text || "failed to connect" in text || "timeout" in text || "network is unreachable" in text -> AiFailure.NO_NETWORK
        hasName("APINotConfigured", "ServiceDisabled", "InvalidAPIKey", "PermissionMissing", "UnsupportedUserLocation") ||
            "genai config not found" in text || "has not been used" in text || "is disabled" in text || "not enabled" in text ||
            "api key not valid" in text || "permission_denied" in text || "403" in text -> AiFailure.NOT_ENABLED
        "is not found for api version" in text || "no longer available" in text || "models/" in text && "not found" in text || "404" in text -> AiFailure.MODEL_UNAVAILABLE
        hasName("ContentBlocked", "PromptBlocked", "ResponseStopped") -> AiFailure.BLOCKED
        else -> AiFailure.OTHER
    }
}

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
    /** Tried in order; the next one is only used when the previous model doesn't exist for this project. */
    private val modelNames: List<String>,
) : AiRepository {

    private val backend get() = Firebase.ai(backend = GenerativeBackend.googleAI())

    override suspend fun assessFurniture(photo: Uri, notes: String, ageYears: Int?, city: String): FurnitureAssessment =
        guarded { modelName ->
            // Decoding a photo is disk and CPU work, so it runs off the main thread.
            val bitmap = withContext(Dispatchers.IO) { decodeScaledBitmap(contentResolver, photo, PHOTO_EDGE_PX) } ?: throw AiException(AiFailure.OTHER)
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
        guarded { modelName ->
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

    /**
     * Runs [block] with each model name in turn, turning anything that goes wrong into an [AiException]. Only a "model not
     * available" or "busy" failure moves on to the next name; every other failure (AI not enabled, no network, quota...) stops straight away.
     */
    private suspend fun <T> guarded(block: suspend (modelName: String) -> T): T {
        var last: AiException? = null
        for (modelName in modelNames) {
            try {
                return block(modelName)
            } catch (e: CancellationException) {
                throw e
            } catch (e: AiException) {
                throw e
            } catch (e: AiResponseException) {
                throw AiException(AiFailure.BAD_ANSWER, e)
            } catch (e: Exception) {
                val failure = classifyAiFailure(e)
                // The real error, for whoever is debugging (e.g. AI Logic not switched on in the Firebase console).
                Log.e(TAG, "Gemini request failed ($failure) with model $modelName: ${e::class.java.simpleName}: ${e.message}", e)
                last = AiException(failure, e)
                if (failure != AiFailure.MODEL_UNAVAILABLE && failure != AiFailure.BUSY) throw last
            }
        }
        throw last ?: AiException(AiFailure.OTHER)
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
