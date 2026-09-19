package com.homesajja.app.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homesajja.app.data.model.FurnitureDimensions
import com.homesajja.app.data.model.FurnitureListing
import com.homesajja.app.data.model.SellerType
import com.homesajja.app.data.model.UserRole
import com.homesajja.app.repository.AuthRepository
import com.homesajja.app.repository.ListingRepository
import com.homesajja.app.repository.SessionRepository
import com.homesajja.app.repository.ImageRepository
import com.homesajja.app.repository.UserRepository
import com.homesajja.app.repository.VendorRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** What the sell screen shows before/around the step content. */
sealed interface SellScreenState {
    data object Loading : SellScreenState
    data class Error(val message: String) : SellScreenState
    data object Ready : SellScreenState
}

sealed interface PublishState {
    data object Idle : PublishState
    data class Publishing(val progress: String) : PublishState
    data class Published(val listingId: String) : PublishState
    data class Failed(val message: String) : PublishState
}

/**
 * Drives the multi-step sell flow. With a `listingId` argument it edits that
 * listing instead: the form starts pre-filled and publishing updates it.
 */
class SellViewModel(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val vendorRepository: VendorRepository,
    private val sessionRepository: SessionRepository,
    private val listingRepository: ListingRepository,
    private val imageRepository: ImageRepository,
) : ViewModel() {

    private val editingListingId: String? = savedStateHandle["listingId"]
    val isEditing: Boolean = editingListingId != null

    var screenState by mutableStateOf<SellScreenState>(SellScreenState.Loading)
        private set
    var form by mutableStateOf(SellForm())
        private set
    var step by mutableStateOf(SellStep.CATEGORY)
        private set
    var stepError by mutableStateOf<String?>(null)
        private set
    var publishState by mutableStateOf<PublishState>(PublishState.Idle)
        private set

    private var editingListing: FurnitureListing? = null

    /** Shown as the seller name on a vendor's listings; null for individuals. */
    private var vendorShopName: String? = null

    init {
        load()
    }

    fun load() {
        screenState = SellScreenState.Loading
        viewModelScope.launch {
            try {
                val uid = authRepository.currentUserId
                if (uid == null) {
                    screenState = SellScreenState.Error("Please log in to sell an item.")
                    return@launch
                }
                if (editingListingId == null) {
                    // A person's city comes from their user profile; a vendor's from their shop profile.
                    val user = userRepository.getUserProfile(uid)
                    val vendor = if (user == null) vendorRepository.getVendorProfile(uid) else null
                    vendorShopName = vendor?.businessName?.ifBlank { vendor.name }
                    form = form.copy(city = user?.city ?: vendor?.city.orEmpty())
                } else {
                    val listing = listingRepository.getListing(editingListingId)
                    if (listing == null || listing.ownerId != uid) {
                        screenState = SellScreenState.Error("This listing can't be edited.")
                        return@launch
                    }
                    editingListing = listing
                    form = listing.toForm()
                }
                screenState = SellScreenState.Ready
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                screenState = SellScreenState.Error(mapError(e, "Couldn't open the sell flow."))
            }
        }
    }

    fun update(transform: (SellForm) -> SellForm) {
        form = transform(form)
        stepError = null
    }

    /** Adds picked photos, keeping the total at [MAX_PHOTOS]. */
    fun addPhotos(uris: List<Uri>) {
        val room = MAX_PHOTOS - form.photos.size
        form = form.copy(photos = form.photos + uris.take(room).map { SellPhoto.Local(it) })
        stepError = if (uris.size > room) "Only $MAX_PHOTOS photos are allowed — extra ones were skipped." else null
    }

    fun removePhoto(index: Int) {
        form = form.copy(photos = form.photos.filterIndexed { i, _ -> i != index })
        stepError = null
    }

    fun next() {
        val problem = form.validate(step)
        if (problem != null) {
            stepError = problem
            return
        }
        stepError = null
        val steps = SellStep.entries
        if (step == steps.last()) publish() else step = steps[step.ordinal + 1]
    }

    fun back() {
        stepError = null
        if (step != SellStep.CATEGORY) step = SellStep.entries[step.ordinal - 1]
    }

    fun dismissPublishError() {
        publishState = PublishState.Idle
    }

    private fun publish() {
        val uid = authRepository.currentUserId ?: return
        viewModelScope.launch {
            try {
                val editing = editingListing
                val listingId = editing?.id ?: listingRepository.newListingId()

                val localCount = form.photos.count { it is SellPhoto.Local }
                var done = 0
                val imageUrls = form.photos.map { photo ->
                    when (photo) {
                        is SellPhoto.Remote -> photo.url
                        is SellPhoto.Local -> {
                            publishState = PublishState.Publishing("Uploading photo ${done + 1} of $localCount…")
                            imageRepository.uploadListingImage(uid, listingId, photo.uri).also { done++ }
                        }
                    }
                }

                publishState = PublishState.Publishing("Saving your listing…")
                val role = sessionRepository.roleFlow.first()
                val listing = buildListing(listingId, uid, imageUrls, editing, role)
                if (editing == null) listingRepository.createListing(listing) else listingRepository.updateListing(listing)

                publishState = PublishState.Published(listingId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                publishState = PublishState.Failed(mapError(e, "Couldn't publish your listing. Please try again."))
            }
        }
    }

    private fun buildListing(
        id: String,
        ownerId: String,
        imageUrls: List<String>,
        editing: FurnitureListing?,
        role: UserRole?,
    ): FurnitureListing {
        val base = editing ?: FurnitureListing(
            ownerName = vendorShopName ?: authRepository.currentUserDisplayName ?: authRepository.currentUserEmail ?: "Seller",
            sellerType = if (role == UserRole.VENDOR) SellerType.VENDOR else SellerType.INDIVIDUAL,
        )
        return base.copy(
            id = id,
            ownerId = ownerId,
            title = form.title.trim(),
            description = form.description.trim(),
            images = imageUrls,
            price = form.price.trim().toLongOrNull() ?: 0L,
            actionType = form.actionType,
            condition = checkNotNull(form.condition),
            refurbished = form.refurbished,
            category = checkNotNull(form.category),
            material = checkNotNull(form.material),
            ageYears = form.ageYears.trim().toInt(),
            dimensions = FurnitureDimensions(
                lengthCm = form.lengthCm.trim().toIntOrNull(),
                widthCm = form.widthCm.trim().toIntOrNull(),
                heightCm = form.heightCm.trim().toIntOrNull(),
            ),
            city = form.city,
        )
    }

    private fun FurnitureListing.toForm() = SellForm(
        category = category,
        photos = images.map { SellPhoto.Remote(it) },
        title = title,
        description = description,
        material = material,
        ageYears = ageYears.toString(),
        lengthCm = dimensions.lengthCm?.toString().orEmpty(),
        widthCm = dimensions.widthCm?.toString().orEmpty(),
        heightCm = dimensions.heightCm?.toString().orEmpty(),
        condition = condition,
        refurbished = refurbished,
        price = if (price == 0L) "" else price.toString(),
        actionType = actionType,
        city = city,
    )
}
