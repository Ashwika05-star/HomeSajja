package com.homesajja.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.homesajja.app.data.model.EntityType
import com.homesajja.app.data.model.Review
import com.homesajja.app.repository.AuthRepository
import com.homesajja.app.repository.FirestoreIds
import com.homesajja.app.repository.ReviewRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

const val MAX_REVIEW_COMMENT = 500

/** Which completed transaction a review is about, and who is being reviewed. */
data class ReviewParams(
    val contextType: EntityType,
    val contextId: String,
    val targetUserId: String,
    val targetName: String,
)

/** Lets the review prompt be created for a specific transaction wherever it is shown. */
object ReviewParamsKey : CreationExtras.Key<ReviewParams>

sealed interface ReviewUiState {
    data object Loading : ReviewUiState
    data class Error(val message: String) : ReviewUiState

    /** [review] is the one already written, or null if the person hasn't reviewed yet. */
    data class Ready(val review: Review?) : ReviewUiState
}

/** The "Leave a review" prompt for one completed transaction: shows what was already written, and saves a new or edited review. */
class ReviewViewModel(
    private val params: ReviewParams,
    private val authRepository: AuthRepository,
    private val reviewRepository: ReviewRepository,
) : ViewModel() {

    var uiState by mutableStateOf<ReviewUiState>(ReviewUiState.Loading)
        private set

    var dialogOpen by mutableStateOf(false)
        private set
    var rating by mutableStateOf(0)
        private set
    var comment by mutableStateOf("")
        private set
    var saving by mutableStateOf(false)
        private set
    var dialogError by mutableStateOf<String?>(null)
        private set

    private val myId: String? = authRepository.currentUserId

    val targetName: String get() = params.targetName

    init {
        load()
    }

    fun load() {
        val uid = myId
        if (uid == null) {
            uiState = ReviewUiState.Error("Please log in to leave a review.")
            return
        }
        uiState = ReviewUiState.Loading
        viewModelScope.launch {
            uiState = try {
                ReviewUiState.Ready(reviewRepository.getReview(FirestoreIds.reviewId(uid, params.contextType, params.contextId)))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ReviewUiState.Error(mapError(e, "Couldn't check your review."))
            }
        }
    }

    fun openDialog() {
        val existing = (uiState as? ReviewUiState.Ready)?.review
        rating = existing?.rating ?: 0
        comment = existing?.comment.orEmpty()
        dialogError = null
        dialogOpen = true
    }

    fun closeDialog() {
        if (!saving) dialogOpen = false
    }

    fun chooseRating(value: Int) {
        rating = value.coerceIn(1, 5)
        dialogError = null
    }

    fun changeComment(value: String) {
        comment = value.take(MAX_REVIEW_COMMENT)
    }

    fun submit() {
        val uid = myId ?: return
        if (rating !in 1..5) {
            dialogError = "Tap a star to rate."
            return
        }
        saving = true
        viewModelScope.launch {
            try {
                val existing = (uiState as? ReviewUiState.Ready)?.review
                val saved = if (existing == null) {
                    reviewRepository.createReview(
                        Review(
                            reviewerId = uid,
                            reviewerName = authRepository.currentUserDisplayName ?: authRepository.currentUserEmail ?: "Someone",
                            targetUserId = params.targetUserId,
                            contextType = params.contextType,
                            contextId = params.contextId,
                            rating = rating,
                            comment = comment.trim(),
                        ),
                    )
                } else {
                    reviewRepository.updateReview(existing.id, rating, comment.trim())
                    existing.copy(rating = rating, comment = comment.trim())
                }
                uiState = ReviewUiState.Ready(saved)
                dialogOpen = false
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                dialogError = mapError(e, "Couldn't save your review. Please try again.")
            } finally {
                saving = false
            }
        }
    }
}
