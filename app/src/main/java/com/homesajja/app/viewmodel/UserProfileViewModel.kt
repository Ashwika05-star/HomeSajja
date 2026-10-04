package com.homesajja.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homesajja.app.data.model.Review
import com.homesajja.app.data.model.UserProfile
import com.homesajja.app.payment.UpiPayment
import com.homesajja.app.repository.AuthRepository
import com.homesajja.app.repository.ReviewRepository
import com.homesajja.app.repository.StatsRepository
import com.homesajja.app.repository.UserRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** The two cards on your own profile: what you earned from selling, and the furniture you reused, repaired and recycled. */
data class ProfileSummary(
    /** Money received from your own sales: only payments you confirmed as received. */
    val totalEarned: Long,
    val earnedIsPartial: Boolean,
    val counts: UserTaskCounts,
) {
    val itemsSold: Int get() = counts.itemsSold
    val itemsReused: Int get() = counts.itemsReused
    val itemsRepaired: Int get() = counts.repaired
    val itemsRecycled: Int get() = counts.recycled

    /** True when there is nothing to show yet (the cards then explain how to get started). */
    val isEmpty: Boolean get() = totalEarned == 0L && itemsReused == 0 && itemsRepaired == 0 && itemsRecycled == 0
}

sealed interface SummaryState {
    data object Loading : SummaryState
    data class Error(val message: String) : SummaryState
    data class Loaded(val summary: ProfileSummary) : SummaryState
}

sealed interface UserProfileUiState {
    data object Loading : UserProfileUiState
    data class Error(val message: String) : UserProfileUiState

    /**
     * [profile] is the person's own details, set only on your own profile: other people's accounts are private,
     * so a public profile has just [name] (from wherever you found them) and their reviews.
     */
    data class Content(
        val userId: String,
        val name: String,
        val profile: UserProfile?,
        val reviews: List<Review>,
        val isOwn: Boolean,
    ) : UserProfileUiState
}

/**
 * A person's profile: your own (no arguments) or someone else's (`userId` and `name` arguments). Both show the
 * reviews written about them.
 */
class UserProfileViewModel(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val reviewRepository: ReviewRepository,
    private val statsRepository: StatsRepository,
) : ViewModel() {

    private val myId: String? = authRepository.currentUserId
    private val userId: String? = savedStateHandle.get<String>("userId") ?: myId
    private val nameArg: String? = savedStateHandle.get<String>("name")

    private val _uiState = MutableStateFlow<UserProfileUiState>(UserProfileUiState.Loading)
    val uiState: StateFlow<UserProfileUiState> = _uiState

    /** The UPI id being typed on your own profile, the id last saved, and how saving went. */
    var upiDraft by mutableStateOf("")
        private set
    var savedUpiId by mutableStateOf<String?>(null)
        private set
    var upiSaving by mutableStateOf(false)
        private set
    var upiMessage by mutableStateOf<String?>(null)
        private set
    private var upiLoaded = false

    val upiDraftInvalid: Boolean get() = upiDraft.isNotBlank() && !UpiPayment.isValidUpiId(upiDraft)

    fun onUpiDraftChange(value: String) {
        upiDraft = value
        upiMessage = null
    }

    /** Saves the typed UPI id on your own profile, or clears it when the field is empty. */
    fun saveUpiId() {
        val uid = myId ?: return
        if (upiSaving || upiDraftInvalid) return
        val value = upiDraft.trim().takeIf { it.isNotEmpty() }
        upiSaving = true
        viewModelScope.launch {
            try {
                userRepository.updateUpiId(uid, value)
                savedUpiId = value
                upiDraft = value.orEmpty()
                upiMessage = if (value == null) "UPI ID removed." else "UPI ID saved."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                upiMessage = mapError(e, "Couldn't save your UPI ID.")
            } finally {
                upiSaving = false
            }
        }
    }

    /** The earnings and sustainability numbers on your own profile (not loaded for other people's profiles). */
    var summary by mutableStateOf<SummaryState>(SummaryState.Loading)
        private set

    private val refreshGate = RefreshGate()

    init {
        load(showLoading = true)
        if (userId != null && userId == myId) loadSummary()
    }

    fun retrySummary() = loadSummary()

    /** Counts use aggregation queries and the money is read from your confirmed sales only, so this stays cheap (see [StatsRepository]). */
    private fun loadSummary() {
        val id = myId ?: return
        summary = SummaryState.Loading
        viewModelScope.launch {
            try {
                val (counts, earned) = coroutineScope {
                    val c = async { statsRepository.userTaskCounts(id) }
                    val e = async { statsRepository.userEarned(id) }
                    c.await() to e.await()
                }
                summary = SummaryState.Loaded(ProfileSummary(totalOf(earned.items), earned.isPartial, counts))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                summary = SummaryState.Error(mapError(e, "Couldn't load your summary."))
            }
        }
    }

    fun retry() = load(showLoading = true)

    fun refreshIfStale() {
        if (refreshGate.isStale()) {
            load(showLoading = false)
            if (userId != null && userId == myId && summary !is SummaryState.Loading) loadSummary()
        }
    }

    private fun load(showLoading: Boolean) {
        val id = userId
        if (id == null) {
            _uiState.value = UserProfileUiState.Error("Please log in to see this profile.")
            return
        }
        if (showLoading) _uiState.value = UserProfileUiState.Loading
        refreshGate.markLoaded()
        viewModelScope.launch {
            try {
                val isOwn = id == myId
                val (profile, reviews) = coroutineScope {
                    val own = async { if (isOwn) userRepository.getUserProfile(id) else null }
                    val received = async { reviewRepository.getReviewsForTarget(id) }
                    own.await() to received.await()
                }
                if (isOwn && profile != null && !upiLoaded) {
                    upiLoaded = true
                    savedUpiId = profile.upiId
                    upiDraft = profile.upiId.orEmpty()
                }
                _uiState.value = UserProfileUiState.Content(
                    userId = id,
                    name = profile?.name ?: nameArg ?: authRepository.currentUserDisplayName.takeIf { isOwn } ?: "This person",
                    profile = profile,
                    reviews = reviews,
                    isOwn = isOwn,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (showLoading || _uiState.value !is UserProfileUiState.Content) {
                    _uiState.value = UserProfileUiState.Error(mapError(e, "Couldn't load this profile."))
                }
            } finally {
                refreshGate.markLoaded()
            }
        }
    }
}
