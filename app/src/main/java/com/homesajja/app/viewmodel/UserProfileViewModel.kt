package com.homesajja.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homesajja.app.data.model.Review
import com.homesajja.app.data.model.UserProfile
import com.homesajja.app.repository.AuthRepository
import com.homesajja.app.repository.ReviewRepository
import com.homesajja.app.repository.UserRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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
) : ViewModel() {

    private val myId: String? = authRepository.currentUserId
    private val userId: String? = savedStateHandle.get<String>("userId") ?: myId
    private val nameArg: String? = savedStateHandle.get<String>("name")

    private val _uiState = MutableStateFlow<UserProfileUiState>(UserProfileUiState.Loading)
    val uiState: StateFlow<UserProfileUiState> = _uiState

    private val refreshGate = RefreshGate()

    init {
        load(showLoading = true)
    }

    fun retry() = load(showLoading = true)

    fun refreshIfStale() {
        if (refreshGate.isStale()) load(showLoading = false)
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
