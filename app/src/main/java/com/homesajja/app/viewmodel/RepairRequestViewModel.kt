package com.homesajja.app.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homesajja.app.data.model.FurnitureListing
import com.homesajja.app.data.model.RepairProblemType
import com.homesajja.app.data.model.RepairRequest
import com.homesajja.app.data.model.VendorBusinessType
import com.homesajja.app.data.model.VendorProfile
import com.homesajja.app.repository.AuthRepository
import com.homesajja.app.repository.ImageRepository
import com.homesajja.app.repository.ListingRepository
import com.homesajja.app.repository.NotificationSender
import com.homesajja.app.repository.ReviewRepository
import com.homesajja.app.data.model.FlowPrefill
import com.homesajja.app.data.model.Recommendation
import kotlinx.coroutines.flow.MutableStateFlow
import com.homesajja.app.data.model.RatingSummary
import com.homesajja.app.repository.NotificationTemplates
import com.homesajja.app.repository.RepairRepository
import com.homesajja.app.repository.UserRepository
import com.homesajja.app.repository.VendorRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** What the flow shows before/around the step content. */
sealed interface RepairScreenState {
    data object Loading : RepairScreenState
    data class Error(val message: String) : RepairScreenState
    data object Ready : RepairScreenState
}

/** A list the flow loads by itself (own listings, providers): each has its own loading/empty/error. */
sealed interface LoadState<out T> {
    data object Loading : LoadState<Nothing>
    data class Error(val message: String) : LoadState<Nothing>
    data class Loaded<T>(val items: List<T>) : LoadState<T>
}

sealed interface RepairSendState {
    data object Idle : RepairSendState
    data class Sending(val progress: String) : RepairSendState
    data class Sent(val requestId: String) : RepairSendState
    data class Failed(val message: String) : RepairSendState
}

/** Drives the repair request flow: furniture -> photos -> problem -> description -> location -> provider. */
class RepairRequestViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val listingRepository: ListingRepository,
    private val vendorRepository: VendorRepository,
    private val repairRepository: RepairRepository,
    private val imageRepository: ImageRepository,
    private val notificationSender: NotificationSender,
    private val reviewRepository: ReviewRepository,
    private val prefillHolder: MutableStateFlow<FlowPrefill?>,
) : ViewModel() {

    var screenState by mutableStateOf<RepairScreenState>(RepairScreenState.Loading)
        private set
    var form by mutableStateOf(RepairForm())
        private set
    var step by mutableStateOf(RepairStep.FURNITURE)
        private set
    var stepError by mutableStateOf<String?>(null)
        private set
    var listings by mutableStateOf<LoadState<FurnitureListing>>(LoadState.Loading)
        private set
    var providers by mutableStateOf<LoadState<VendorProfile>>(LoadState.Loading)
        private set
    /** Each provider's rating, by vendor id; a provider with no reviews is missing. Loaded after the list, and optional. */
    var ratings by mutableStateOf<Map<String, RatingSummary>>(emptyMap())
        private set
    var sendState by mutableStateOf<RepairSendState>(RepairSendState.Idle)
        private set

    init {
        load()
    }

    fun load() {
        screenState = RepairScreenState.Loading
        listings = LoadState.Loading
        viewModelScope.launch {
            try {
                val uid = authRepository.currentUserId
                if (uid == null) {
                    screenState = RepairScreenState.Error("Please log in to request a repair.")
                    return@launch
                }
                form = form.copy(city = userRepository.getUserProfile(uid)?.city.orEmpty())
                prefillHolder.value?.takeIf { it.target == Recommendation.REPAIR }?.let { prefill ->
                    prefillHolder.value = null
                    form = form.withPrefill(prefill)
                    // Land on the first step that still needs an answer.
                    step = RepairStep.entries.dropLast(1).firstOrNull { form.validate(it) != null } ?: RepairStep.entries.last()
                }
                screenState = RepairScreenState.Ready
                loadListings(uid)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                screenState = RepairScreenState.Error(mapError(e, "Couldn't open the repair flow."))
            }
        }
    }

    fun retryListings() {
        val uid = authRepository.currentUserId ?: return
        listings = LoadState.Loading
        viewModelScope.launch { loadListings(uid) }
    }

    private suspend fun loadListings(uid: String) {
        listings = try {
            LoadState.Loaded(listingRepository.getListingsByOwner(uid))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LoadState.Error(mapError(e, "Couldn't load your listings."))
        }
    }

    fun retryProviders() = loadProviders()

    private fun loadProviders() {
        providers = LoadState.Loading
        viewModelScope.launch {
            providers = try {
                LoadState.Loaded(vendorRepository.getVendorsByTypes(form.city, VendorBusinessType.REPAIR_PROVIDERS).also { found ->
                    ratings = runCatching { reviewRepository.getRatingSummaries(found.map { it.uid }) }.getOrDefault(emptyMap())
                })
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LoadState.Error(mapError(e, "Couldn't load repair providers."))
            }
        }
    }

    /** Picks one of the user's listings; its photos become the starting photos. */
    fun chooseListing(listing: FurnitureListing) {
        form = form.copy(
            listing = listing,
            notListed = false,
            category = listing.category,
            photos = listing.images.take(MAX_PHOTOS).map { SellPhoto.Remote(it) },
        )
        stepError = null
    }

    fun chooseNotListed() {
        if (form.notListed) return
        form = form.copy(listing = null, notListed = true, photos = emptyList())
        stepError = null
    }

    fun update(transform: (RepairForm) -> RepairForm) {
        form = transform(form)
        stepError = null
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

    fun selectProblem(problem: RepairProblemType) = update { it.copy(problem = problem) }

    fun selectProvider(provider: VendorProfile) = update { it.copy(provider = provider) }

    fun next() {
        val problem = form.validate(step)
        if (problem != null) {
            stepError = problem
            return
        }
        stepError = null
        val steps = RepairStep.entries
        if (step == steps.last()) {
            send()
        } else {
            step = steps[step.ordinal + 1]
            if (step == RepairStep.PROVIDER) loadProviders()
        }
    }

    fun back() {
        stepError = null
        if (step != RepairStep.FURNITURE) step = RepairStep.entries[step.ordinal - 1]
    }

    fun dismissSendError() {
        sendState = RepairSendState.Idle
    }

    private fun send() {
        val uid = authRepository.currentUserId ?: return
        val provider = form.provider ?: return
        val problem = form.problem ?: return
        viewModelScope.launch {
            try {
                val requestId = repairRepository.newRequestId()
                val localCount = form.photos.count { it is SellPhoto.Local }
                var done = 0
                val imageUrls = form.photos.map { photo ->
                    when (photo) {
                        is SellPhoto.Remote -> photo.url
                        is SellPhoto.Local -> {
                            sendState = RepairSendState.Sending("Uploading photo ${done + 1} of $localCount…")
                            imageRepository.uploadRepairImage(uid, requestId, photo.uri).also { done++ }
                        }
                    }
                }

                sendState = RepairSendState.Sending("Sending your request…")
                val saved = repairRepository.createRequest(
                    RepairRequest(
                        id = requestId,
                        userId = uid,
                        userName = authRepository.currentUserDisplayName ?: authRepository.currentUserEmail ?: "Customer",
                        vendorId = provider.uid,
                        vendorName = provider.businessName.ifBlank { provider.name },
                        listingId = form.listing?.id,
                        furnitureTitle = form.furnitureTitle,
                        furnitureCategory = form.furnitureCategory,
                        problemType = problem,
                        issueDescription = form.description.trim(),
                        images = imageUrls,
                        city = form.city,
                    ),
                )
                notificationSender.send(NotificationTemplates.repairRequested(saved))
                sendState = RepairSendState.Sent(requestId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                sendState = RepairSendState.Failed(mapError(e, "Couldn't send your repair request. Please try again."))
            }
        }
    }
}
