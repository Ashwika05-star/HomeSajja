package com.homesajja.app.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homesajja.app.data.model.Cities
import com.homesajja.app.data.model.VendorBusinessType
import com.homesajja.app.data.model.VendorProfile
import com.homesajja.app.repository.AuthRepository
import com.homesajja.app.repository.ImageRepository
import com.homesajja.app.repository.VendorRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

sealed interface VendorEditScreenState {
    data object Loading : VendorEditScreenState
    data class Error(val message: String) : VendorEditScreenState
    data object Ready : VendorEditScreenState
}

sealed interface VendorSaveState {
    data object Idle : VendorSaveState
    data class Saving(val progress: String) : VendorSaveState
    data object Saved : VendorSaveState
    data class Failed(val message: String) : VendorSaveState
}

/** What the vendor types and picks on the profile screen. */
data class VendorProfileForm(
    val businessName: String = "",
    val businessType: VendorBusinessType? = null,
    val description: String = "",
    val shopAddress: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val brochure: List<SellPhoto> = emptyList(),
) {
    fun validate(): String? = when {
        businessName.isBlank() -> "Enter your business name."
        businessType == null -> "Pick your business type."
        else -> null
    }
}

/** Sets up or edits the vendor's business profile, including the shop pin and brochure photos. */
class VendorProfileEditViewModel(
    private val authRepository: AuthRepository,
    private val vendorRepository: VendorRepository,
    private val imageRepository: ImageRepository,
) : ViewModel() {

    var screenState by mutableStateOf<VendorEditScreenState>(VendorEditScreenState.Loading)
        private set
    var form by mutableStateOf(VendorProfileForm())
        private set
    var formError by mutableStateOf<String?>(null)
        private set
    var saveState by mutableStateOf<VendorSaveState>(VendorSaveState.Idle)
        private set

    /** Where the map's pin is right now; it only becomes the shop location when the vendor taps "Set location". */
    var pinLatitude by mutableStateOf(0.0)
        private set
    var pinLongitude by mutableStateOf(0.0)
        private set

    /** The saved profile, kept so unchanged fields (name, phone, verified flag...) are written back as they were. */
    private var original: VendorProfile? = null

    init {
        load()
    }

    fun load() {
        screenState = VendorEditScreenState.Loading
        viewModelScope.launch {
            try {
                val uid = authRepository.currentUserId
                val profile = uid?.let { vendorRepository.getVendorProfile(it) }
                if (profile == null) {
                    screenState = VendorEditScreenState.Error("Couldn't find your vendor profile.")
                    return@launch
                }
                original = profile
                form = VendorProfileForm(
                    businessName = profile.businessName,
                    businessType = VendorBusinessType.fromNameOrNull(profile.businessType),
                    description = profile.description,
                    shopAddress = profile.shopAddress,
                    latitude = profile.shopLatitude,
                    longitude = profile.shopLongitude,
                    brochure = profile.brochureImages.map { SellPhoto.Remote(it) },
                )
                val (cityLat, cityLng) = Cities.centreOf(profile.city)
                pinLatitude = profile.shopLatitude ?: cityLat
                pinLongitude = profile.shopLongitude ?: cityLng
                screenState = VendorEditScreenState.Ready
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                screenState = VendorEditScreenState.Error(mapError(e, "Couldn't open your profile."))
            }
        }
    }

    /** The city the map opens on, and shows next to the address. */
    val city: String get() = original?.city.orEmpty()

    fun update(transform: (VendorProfileForm) -> VendorProfileForm) {
        form = transform(form)
        formError = null
    }

    fun onPinMoved(latitude: Double, longitude: Double) {
        pinLatitude = latitude
        pinLongitude = longitude
    }

    fun setLocationToPin() = update { it.copy(latitude = pinLatitude, longitude = pinLongitude) }

    fun addBrochure(uris: List<Uri>) {
        val room = MAX_PHOTOS - form.brochure.size
        form = form.copy(brochure = form.brochure + uris.take(room).map { SellPhoto.Local(it) })
        formError = if (uris.size > room) "Only $MAX_PHOTOS photos are allowed — extra ones were skipped." else null
    }

    fun removeBrochure(index: Int) = update { it.copy(brochure = it.brochure.filterIndexed { i, _ -> i != index }) }

    fun dismissSaveError() {
        saveState = VendorSaveState.Idle
    }

    fun save() {
        val profile = original ?: return
        val problem = form.validate()
        if (problem != null) {
            formError = problem
            return
        }
        viewModelScope.launch {
            try {
                val localCount = form.brochure.count { it is SellPhoto.Local }
                var done = 0
                val urls = form.brochure.map { photo ->
                    when (photo) {
                        is SellPhoto.Remote -> photo.url
                        is SellPhoto.Local -> {
                            saveState = VendorSaveState.Saving("Uploading photo ${done + 1} of $localCount…")
                            imageRepository.uploadVendorImage(profile.uid, photo.uri).also { done++ }
                        }
                    }
                }
                saveState = VendorSaveState.Saving("Saving your profile…")
                val updated = profile.copy(
                    businessName = form.businessName.trim(),
                    businessType = form.businessType?.name.orEmpty(),
                    description = form.description.trim(),
                    shopAddress = form.shopAddress.trim(),
                    shopLatitude = form.latitude,
                    shopLongitude = form.longitude,
                    brochureImages = urls,
                )
                vendorRepository.updateVendorProfile(updated)
                original = updated
                saveState = VendorSaveState.Saved
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                saveState = VendorSaveState.Failed(mapError(e, "Couldn't save your profile. Please try again."))
            }
        }
    }
}
