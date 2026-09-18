package com.homesajja.app.di

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.createSavedStateHandle
import com.homesajja.app.viewmodel.ExploreViewModel
import com.homesajja.app.viewmodel.HomeViewModel
import com.homesajja.app.viewmodel.ListingDetailViewModel
import com.homesajja.app.viewmodel.LoginViewModel
import com.homesajja.app.viewmodel.MyListingsViewModel
import com.homesajja.app.viewmodel.MyRequestsViewModel
import com.homesajja.app.viewmodel.SellViewModel
import com.homesajja.app.viewmodel.SignupViewModel
import com.homesajja.app.viewmodel.SplashViewModel

/** Manual ViewModel factory matching [AppContainer]'s manual DI — see its
 * doc comment for why this project doesn't use Hilt. */
class ViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {

    // The CreationExtras overload is what Compose's viewModel() calls; it gives
    // us the SavedStateHandle, which holds navigation arguments like the listing id.
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        return build(modelClass, extras.createSavedStateHandle()) as T
    }

    private fun build(modelClass: Class<*>, handle: SavedStateHandle): ViewModel {
        return when (modelClass) {
            SplashViewModel::class.java -> SplashViewModel(
                container.authRepository,
                container.sessionRepository,
            )
            SignupViewModel::class.java -> SignupViewModel(
                container.authRepository,
                container.userRepository,
                container.vendorRepository,
                container.sessionRepository,
            )
            LoginViewModel::class.java -> LoginViewModel(
                container.authRepository,
                container.userRepository,
                container.vendorRepository,
                container.sessionRepository,
            )
            HomeViewModel::class.java -> HomeViewModel(
                container.authRepository,
                container.sessionRepository,
            )
            ExploreViewModel::class.java -> ExploreViewModel(
                container.authRepository,
                container.userRepository,
                container.listingRepository,
            )
            ListingDetailViewModel::class.java -> ListingDetailViewModel(
                handle,
                container.authRepository,
                container.listingRepository,
                container.favouriteRepository,
                container.chatRepository,
                container.purchaseRequestRepository,
            )
            SellViewModel::class.java -> SellViewModel(
                handle,
                container.authRepository,
                container.userRepository,
                container.sessionRepository,
                container.listingRepository,
                container.storageRepository,
            )
            MyListingsViewModel::class.java -> MyListingsViewModel(
                container.authRepository,
                container.listingRepository,
                container.storageRepository,
            )
            MyRequestsViewModel::class.java -> MyRequestsViewModel(
                container.authRepository,
                container.purchaseRequestRepository,
            )
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
