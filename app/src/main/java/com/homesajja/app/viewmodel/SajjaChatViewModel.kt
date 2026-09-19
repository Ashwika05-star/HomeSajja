package com.homesajja.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homesajja.app.data.model.FurnitureListing
import com.homesajja.app.repository.AiRepository
import com.homesajja.app.repository.AuthRepository
import com.homesajja.app.repository.ChatTurn
import com.homesajja.app.repository.ChatbotContext
import com.homesajja.app.repository.ListingRepository
import com.homesajja.app.repository.UserRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** One bubble in the Sajja conversation. [listings] are real listings the assistant pointed to. */
data class SajjaMessage(
    val fromUser: Boolean,
    val text: String,
    val listings: List<FurnitureListing> = emptyList(),
    val isError: Boolean = false,
)

const val MAX_CHAT_INPUT = 400
private const val HISTORY_TURNS = 10
private const val CONTEXT_FETCH = 25

val SAJJA_SUGGESTIONS = listOf(
    "Find a sofa under ₹15,000",
    "Should I repair or recycle this table?",
    "What furniture matches my room?",
)

/**
 * The Sajja assistant. It is told the person's city and the listings around them, so when they ask to find
 * something it can point to real listings, which are shown under its answer.
 */
class SajjaChatViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val listingRepository: ListingRepository,
    private val aiRepository: AiRepository,
) : ViewModel() {

    val messages = mutableListOf<SajjaMessage>().toMutableStateList()

    var draft by mutableStateOf("")
        private set
    var thinking by mutableStateOf(false)
        private set

    /** Loaded once, on the first question: the city and listings the assistant may talk about. */
    private var context: ChatbotContext? = null
    private var lastQuestion: String? = null

    fun onDraftChange(value: String) {
        draft = value.take(MAX_CHAT_INPUT)
    }

    fun send() = ask(draft.trim().also { draft = "" })

    fun sendSuggestion(text: String) = ask(text)

    /** Asks the last question again after a failure. */
    fun retry() {
        val question = lastQuestion ?: return
        if (messages.lastOrNull()?.isError == true) messages.removeAt(messages.lastIndex)
        ask(question, addUserBubble = false)
    }

    private fun ask(question: String, addUserBubble: Boolean = true) {
        if (question.isEmpty() || thinking) return
        lastQuestion = question
        // History is what was said before this question, without failed attempts.
        val history = messages.filter { !it.isError }.takeLast(HISTORY_TURNS).map { ChatTurn(it.fromUser, it.text) }
        if (addUserBubble) messages.add(SajjaMessage(fromUser = true, text = question))
        thinking = true
        viewModelScope.launch {
            try {
                val chatContext = context ?: loadContext().also { context = it }
                val reply = aiRepository.chat(history, question, chatContext)
                val byId = chatContext.listings.associateBy { it.id }
                messages.add(SajjaMessage(fromUser = false, text = reply.text, listings = reply.listingIds.mapNotNull { byId[it] }))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                messages.add(SajjaMessage(fromUser = false, text = mapError(e, "Sorry, I couldn't answer that right now."), isError = true))
            } finally {
                thinking = false
            }
        }
    }

    private suspend fun loadContext(): ChatbotContext {
        val uid = authRepository.currentUserId
        val city = uid?.let { runCatching { userRepository.getUserProfile(it)?.city }.getOrNull() }.orEmpty()
        val listings = if (city.isBlank()) emptyList() else runCatching { listingRepository.getListings(city, limit = CONTEXT_FETCH) }.getOrDefault(emptyList())
        return ChatbotContext(city.ifBlank { "India" }, listings.filter { it.ownerId != uid })
    }
}
