package com.homesajja.app.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homesajja.app.data.model.FlowPrefill
import com.homesajja.app.data.model.FurnitureAssessment
import com.homesajja.app.data.model.Recommendation
import com.homesajja.app.repository.AiRepository
import com.homesajja.app.repository.AuthRepository
import com.homesajja.app.repository.UserRepository
import com.homesajja.app.repository.VendorRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Where the "Not sure what to do?" screen is. Whatever happens, the person can still choose a flow themselves. */
sealed interface SmartDecisionPhase {
    data object Input : SmartDecisionPhase
    data object Thinking : SmartDecisionPhase
    data class Suggestion(val assessment: FurnitureAssessment) : SmartDecisionPhase
    data class Failed(val message: String) : SmartDecisionPhase
}

/**
 * "Not sure what to do? Ask HomeSajja": sends a photo and a few notes to Gemini and shows what it recommends.
 * The suggestion is only advice: [proceed] can start any of the four flows, with or without it.
 */
class SmartDecisionViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val vendorRepository: VendorRepository,
    private val aiRepository: AiRepository,
    private val prefillHolder: MutableStateFlow<FlowPrefill?>,
) : ViewModel() {

    var photo by mutableStateOf<Uri?>(null)
        private set
    var notes by mutableStateOf("")
        private set
    var ageText by mutableStateOf("")
        private set
    var phase by mutableStateOf<SmartDecisionPhase>(SmartDecisionPhase.Input)
        private set

    /** Emits the flow to open after [proceed] has stored the prefill. */
    private val _openFlow = MutableSharedFlow<Recommendation>(extraBufferCapacity = 1)
    val openFlow: SharedFlow<Recommendation> = _openFlow

    fun pickPhoto(uri: Uri) {
        photo = uri
        if (phase is SmartDecisionPhase.Failed || phase is SmartDecisionPhase.Suggestion) phase = SmartDecisionPhase.Input
    }

    fun onNotesChange(value: String) {
        notes = value.take(300)
    }

    fun onAgeChange(value: String) {
        ageText = value.filter(Char::isDigit).take(3)
    }

    fun tryAgain() {
        phase = SmartDecisionPhase.Input
    }

    fun ask() {
        val chosen = photo ?: return
        if (phase == SmartDecisionPhase.Thinking) return
        phase = SmartDecisionPhase.Thinking
        viewModelScope.launch {
            phase = try {
                val city = cityOfSignedInPerson()
                SmartDecisionPhase.Suggestion(aiRepository.assessFurniture(chosen, notes, ageText.toIntOrNull(), city))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Never a dead end: the manual choices stay on screen under this message.
                SmartDecisionPhase.Failed(mapError(e, "Couldn't get a suggestion this time."))
            }
        }
    }

    /** Opens [target] pre-filled with what is known (the photo, and the suggestion if there is one). */
    fun proceed(target: Recommendation) {
        val suggestion = (phase as? SmartDecisionPhase.Suggestion)?.assessment
        prefillHolder.value = if (photo != null || suggestion != null) FlowPrefill(target, photo, suggestion) else null
        _openFlow.tryEmit(target)
    }

    private suspend fun cityOfSignedInPerson(): String {
        val uid = authRepository.currentUserId ?: return ""
        return runCatching { userRepository.getUserProfile(uid)?.city ?: vendorRepository.getVendorProfile(uid)?.city }.getOrNull().orEmpty()
    }
}
