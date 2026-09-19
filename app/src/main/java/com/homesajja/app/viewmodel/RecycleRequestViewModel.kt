package com.homesajja.app.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homesajja.app.data.model.RecycleCondition
import com.homesajja.app.data.model.RecycleMaterial
import com.homesajja.app.data.model.RecycleMethod
import com.homesajja.app.data.model.RecyclingRequest
import com.homesajja.app.data.model.VendorBusinessType
import com.homesajja.app.data.model.VendorProfile
import com.homesajja.app.repository.AuthRepository
import com.homesajja.app.repository.ImageRepository
import com.homesajja.app.repository.NotificationSender
import com.homesajja.app.repository.ReviewRepository
import com.homesajja.app.data.model.FlowPrefill
import com.homesajja.app.data.model.Recommendation
import kotlinx.coroutines.flow.MutableStateFlow
import com.homesajja.app.data.model.RatingSummary
import com.homesajja.app.repository.NotificationTemplates
import com.homesajja.app.repository.RecyclingRepository
import com.homesajja.app.repository.UserRepository
import com.homesajja.app.repository.VendorRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** What the flow shows before/around the step content. */
sealed interface RecycleScreenState {
    data object Loading : RecycleScreenState
    data class Error(val message: String) : RecycleScreenState
    data object Ready : RecycleScreenState
}

sealed interface RecycleSendState {
    data object Idle : RecycleSendState
    data class Sending(val progress: String) : RecycleSendState
    data class Sent(val requestId: String) : RecycleSendState
    data class Failed(val message: String) : RecycleSendState
}

/** Drives the recycle flow: photos -> condition -> material -> method -> destination -> confirm. */
class RecycleRequestViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val vendorRepository: VendorRepository,
    private val recyclingRepository: RecyclingRepository,
    private val imageRepository: ImageRepository,
    private val notificationSender: NotificationSender,
    private val reviewRepository: ReviewRepository,
    private val prefillHolder: MutableStateFlow<FlowPrefill?>,
) : ViewModel() {

    var screenState by mutableStateOf<RecycleScreenState>(RecycleScreenState.Loading)
        private set
    var form by mutableStateOf(RecycleForm())
        private set
    var step by mutableStateOf(RecycleStep.PHOTOS)
        private set
    var stepError by mutableStateOf<String?>(null)
        private set
    var recyclers by mutableStateOf<LoadState<VendorProfile>>(LoadState.Loading)
        private set
    /** Each provider's rating, by vendor id; a provider with no reviews is missing. Loaded after the list, and optional. */
    var ratings by mutableStateOf<Map<String, RatingSummary>>(emptyMap())
        private set
    var sendState by mutableStateOf<RecycleSendState>(RecycleSendState.Idle)
        private set

    init {
        load()
    }

    fun load() {
        screenState = RecycleScreenState.Loading
        viewModelScope.launch {
            try {
                val uid = authRepository.currentUserId
                if (uid == null) {
                    screenState = RecycleScreenState.Error("Please log in to recycle furniture.")
                    return@launch
                }
                form = form.copy(city = userRepository.getUserProfile(uid)?.city.orEmpty())
                prefillHolder.value?.takeIf { it.target == Recommendation.RECYCLE }?.let { prefill ->
                    prefillHolder.value = null
                    form = form.withPrefill(prefill)
                    // Land on the first step that still needs an answer.
                    step = RecycleStep.entries.dropLast(1).firstOrNull { form.validate(it) != null } ?: RecycleStep.entries.last()
                }
                screenState = RecycleScreenState.Ready
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                screenState = RecycleScreenState.Error(mapError(e, "Couldn't open the recycle flow."))
            }
        }
    }

    fun retryRecyclers() = loadRecyclers()

    private fun loadRecyclers() {
        recyclers = LoadState.Loading
        viewModelScope.launch {
            recyclers = try {
                LoadState.Loaded(vendorRepository.getVendorsByTypes(form.city, VendorBusinessType.RECYCLERS).also { found ->
                    ratings = runCatching { reviewRepository.getRatingSummaries(found.map { it.uid }) }.getOrDefault(emptyMap())
                })
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LoadState.Error(mapError(e, "Couldn't load recyclers."))
            }
        }
    }

    fun addPhotos(uris: List<Uri>) {
        val room = MAX_PHOTOS - form.photos.size
        form = form.copy(photos = form.photos + uris.take(room).map { SellPhoto.Local(it) })
        stepError = if (uris.size > room) "Only $MAX_PHOTOS photos are allowed — extra ones were skipped." else null
    }

    fun removePhoto(index: Int) {
        form = form.copy(photos = form.photos.filterIndexed { i, _ -> i != index })
        stepError = null
    }

    fun selectCondition(condition: RecycleCondition) = update { it.copy(condition = condition) }

    fun selectMaterial(material: RecycleMaterial) = update { it.copy(material = material) }

    /** Switching method drops any chosen recycler, since only a drop-off has one. */
    fun selectMethod(method: RecycleMethod) = update { it.copy(method = method, recycler = null) }

    fun selectRecycler(recycler: VendorProfile) = update { it.copy(recycler = recycler) }

    private fun update(transform: (RecycleForm) -> RecycleForm) {
        form = transform(form)
        stepError = null
    }

    fun next() {
        val problem = form.validate(step)
        if (problem != null) {
            stepError = problem
            return
        }
        stepError = null
        val steps = RecycleStep.entries
        if (step == steps.last()) {
            send()
        } else {
            step = steps[step.ordinal + 1]
            if (step == RecycleStep.DESTINATION && form.method == RecycleMethod.DROP_OFF) loadRecyclers()
        }
    }

    fun back() {
        stepError = null
        if (step != RecycleStep.PHOTOS) step = RecycleStep.entries[step.ordinal - 1]
    }

    fun dismissSendError() {
        sendState = RecycleSendState.Idle
    }

    private fun send() {
        val uid = authRepository.currentUserId ?: return
        val condition = form.condition ?: return
        val material = form.material ?: return
        val method = form.method ?: return
        val recycler = form.recycler.takeIf { method == RecycleMethod.DROP_OFF }
        viewModelScope.launch {
            try {
                val requestId = recyclingRepository.newRequestId()
                val localCount = form.photos.count { it is SellPhoto.Local }
                var done = 0
                val imageUrls = form.photos.map { photo ->
                    when (photo) {
                        is SellPhoto.Remote -> photo.url
                        is SellPhoto.Local -> {
                            sendState = RecycleSendState.Sending("Uploading photo ${done + 1} of $localCount…")
                            imageRepository.uploadRecyclingImage(uid, requestId, photo.uri).also { done++ }
                        }
                    }
                }

                sendState = RecycleSendState.Sending("Sending your request…")
                val saved = recyclingRepository.createRequest(
                    RecyclingRequest(
                        id = requestId,
                        userId = uid,
                        userName = authRepository.currentUserDisplayName ?: authRepository.currentUserEmail ?: "Customer",
                        vendorId = recycler?.uid,
                        vendorName = recycler?.let { it.businessName.ifBlank { it.name } },
                        condition = condition,
                        material = material,
                        method = method,
                        images = imageUrls,
                        city = form.city,
                    ),
                )
                notificationSender.send(NotificationTemplates.recyclingRequested(saved))
                sendState = RecycleSendState.Sent(requestId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                sendState = RecycleSendState.Failed(mapError(e, "Couldn't send your recycling request. Please try again."))
            }
        }
    }
}
