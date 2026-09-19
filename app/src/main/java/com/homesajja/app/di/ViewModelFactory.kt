package com.homesajja.app.di

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.createSavedStateHandle
import com.homesajja.app.viewmodel.ChatListViewModel
import com.homesajja.app.viewmodel.ChatThreadViewModel
import com.homesajja.app.viewmodel.ExchangeBrowseViewModel
import com.homesajja.app.viewmodel.InboxBadgeViewModel
import com.homesajja.app.viewmodel.NotificationsViewModel
import com.homesajja.app.viewmodel.ExchangeDetailViewModel
import com.homesajja.app.viewmodel.ExchangeProposalViewModel
import com.homesajja.app.viewmodel.ExchangeRequestsViewModel
import com.homesajja.app.viewmodel.ExploreViewModel
import com.homesajja.app.viewmodel.HomeViewModel
import com.homesajja.app.viewmodel.ListingDetailViewModel
import com.homesajja.app.viewmodel.LoginViewModel
import com.homesajja.app.viewmodel.MyListingsViewModel
import com.homesajja.app.viewmodel.MyRequestsViewModel
import com.homesajja.app.viewmodel.MaterialBoardViewModel
import com.homesajja.app.viewmodel.MaterialRequestDetailViewModel
import com.homesajja.app.viewmodel.MaterialRequestFormViewModel
import com.homesajja.app.viewmodel.MyRecyclingViewModel
import com.homesajja.app.viewmodel.OpenPickupsViewModel
import com.homesajja.app.viewmodel.MyRepairsViewModel
import com.homesajja.app.viewmodel.RecycleDetailViewModel
import com.homesajja.app.viewmodel.RecycleRequestViewModel
import com.homesajja.app.viewmodel.RepairDetailViewModel
import com.homesajja.app.viewmodel.RepairRequestViewModel
import com.homesajja.app.viewmodel.ReviewParamsKey
import com.homesajja.app.viewmodel.ReviewViewModel
import com.homesajja.app.viewmodel.BlockedUsersViewModel
import com.homesajja.app.viewmodel.SavedListingsViewModel
import com.homesajja.app.viewmodel.SajjaChatViewModel
import com.homesajja.app.viewmodel.SellViewModel
import com.homesajja.app.viewmodel.SmartDecisionViewModel
import com.homesajja.app.viewmodel.TrustActionsViewModel
import com.homesajja.app.viewmodel.UserProfileViewModel
import com.homesajja.app.viewmodel.VendorDashboardViewModel
import com.homesajja.app.viewmodel.VendorMaterialsViewModel
import com.homesajja.app.viewmodel.VendorProfileEditViewModel
import com.homesajja.app.viewmodel.VendorPublicProfileViewModel
import com.homesajja.app.viewmodel.VendorRecyclingViewModel
import com.homesajja.app.viewmodel.VendorRequestsViewModel
import com.homesajja.app.viewmodel.VendorRepairsViewModel
import com.homesajja.app.viewmodel.SignupViewModel
import com.homesajja.app.viewmodel.SplashViewModel

/** Manual ViewModel factory matching [AppContainer]'s manual DI — see its
 * doc comment for why this project doesn't use Hilt. */
class ViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {

    // The CreationExtras overload is what Compose's viewModel() calls; it gives
    // us the SavedStateHandle, which holds navigation arguments like the listing id.
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        return build(modelClass, extras.createSavedStateHandle(), extras) as T
    }

    private fun build(modelClass: Class<*>, handle: SavedStateHandle, extras: CreationExtras): ViewModel {
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
                container.deviceTokenRepository,
            )
            ExploreViewModel::class.java -> ExploreViewModel(
                container.authRepository,
                container.userRepository,
                container.listingRepository,
                container.favouriteRepository,
            )
            ListingDetailViewModel::class.java -> ListingDetailViewModel(
                handle,
                container.authRepository,
                container.listingRepository,
                container.favouriteRepository,
                container.chatRepository,
                container.purchaseRequestRepository,
                container.notificationSender,
                container.vendorRepository,
                container.reviewRepository,
                container.blockRepository,
            )
            SellViewModel::class.java -> SellViewModel(
                handle,
                container.authRepository,
                container.userRepository,
                container.vendorRepository,
                container.sessionRepository,
                container.listingRepository,
                container.imageRepository,
                container.notificationSender,
                container.flowPrefill,
            )
            MyListingsViewModel::class.java -> MyListingsViewModel(
                container.authRepository,
                container.listingRepository,
            )
            MyRequestsViewModel::class.java -> MyRequestsViewModel(
                container.authRepository,
                container.purchaseRequestRepository,
                container.notificationSender,
            )
            ExchangeBrowseViewModel::class.java -> ExchangeBrowseViewModel(
                container.authRepository,
                container.userRepository,
                container.listingRepository,
                container.favouriteRepository,
            )
            ExchangeProposalViewModel::class.java -> ExchangeProposalViewModel(
                handle,
                container.authRepository,
                container.listingRepository,
                container.exchangeRepository,
                container.notificationSender,
            )
            ExchangeRequestsViewModel::class.java -> ExchangeRequestsViewModel(
                container.authRepository,
                container.exchangeRepository,
            )
            ExchangeDetailViewModel::class.java -> ExchangeDetailViewModel(
                handle,
                container.authRepository,
                container.exchangeRepository,
                container.listingRepository,
                container.chatRepository,
                container.notificationSender,
            )
            RepairRequestViewModel::class.java -> RepairRequestViewModel(
                container.authRepository,
                container.userRepository,
                container.listingRepository,
                container.vendorRepository,
                container.repairRepository,
                container.imageRepository,
                container.notificationSender,
                container.reviewRepository,
                container.flowPrefill,
            )
            MyRepairsViewModel::class.java -> MyRepairsViewModel(
                container.authRepository,
                container.repairRepository,
            )
            VendorRepairsViewModel::class.java -> VendorRepairsViewModel(
                container.authRepository,
                container.repairRepository,
            )
            RepairDetailViewModel::class.java -> RepairDetailViewModel(
                handle,
                container.authRepository,
                container.repairRepository,
                container.chatRepository,
                container.notificationSender,
            )
            RecycleRequestViewModel::class.java -> RecycleRequestViewModel(
                container.authRepository,
                container.userRepository,
                container.vendorRepository,
                container.recyclingRepository,
                container.imageRepository,
                container.notificationSender,
                container.reviewRepository,
                container.flowPrefill,
            )
            MyRecyclingViewModel::class.java -> MyRecyclingViewModel(
                container.authRepository,
                container.recyclingRepository,
            )
            VendorRecyclingViewModel::class.java -> VendorRecyclingViewModel(
                container.authRepository,
                container.recyclingRepository,
            )
            RecycleDetailViewModel::class.java -> RecycleDetailViewModel(
                handle,
                container.authRepository,
                container.recyclingRepository,
                container.notificationSender,
            )
            VendorDashboardViewModel::class.java -> VendorDashboardViewModel(
                container.authRepository,
                container.vendorRepository,
                container.listingRepository,
                container.vendorInboxRepository,
            )
            VendorRequestsViewModel::class.java -> VendorRequestsViewModel(
                container.authRepository,
                container.vendorRepository,
            )
            OpenPickupsViewModel::class.java -> OpenPickupsViewModel(
                container.authRepository,
                container.vendorRepository,
                container.recyclingRepository,
                container.notificationSender,
            )
            VendorProfileEditViewModel::class.java -> VendorProfileEditViewModel(
                container.authRepository,
                container.vendorRepository,
                container.imageRepository,
            )
            VendorPublicProfileViewModel::class.java -> VendorPublicProfileViewModel(
                handle,
                container.authRepository,
                container.vendorRepository,
                container.listingRepository,
                container.reviewRepository,
                container.favouriteRepository,
            )
            MaterialBoardViewModel::class.java -> MaterialBoardViewModel(
                container.authRepository,
                container.userRepository,
                container.materialRequestRepository,
            )
            VendorMaterialsViewModel::class.java -> VendorMaterialsViewModel(
                container.authRepository,
                container.userRepository,
                container.materialRequestRepository,
            )
            MaterialRequestFormViewModel::class.java -> MaterialRequestFormViewModel(
                handle,
                container.authRepository,
                container.vendorRepository,
                container.materialRequestRepository,
            )
            MaterialRequestDetailViewModel::class.java -> MaterialRequestDetailViewModel(
                handle,
                container.authRepository,
                container.materialRequestRepository,
                container.listingRepository,
                container.notificationSender,
            )
            ChatListViewModel::class.java -> ChatListViewModel(
                container.authRepository,
                container.chatRepository,
            )
            ChatThreadViewModel::class.java -> ChatThreadViewModel(
                handle,
                container.authRepository,
                container.chatRepository,
                container.notificationSender,
            )
            NotificationsViewModel::class.java -> NotificationsViewModel(
                container.authRepository,
                container.notificationRepository,
            )
            InboxBadgeViewModel::class.java -> InboxBadgeViewModel(
                container.authRepository,
                container.chatRepository,
                container.notificationRepository,
            )
            SavedListingsViewModel::class.java -> SavedListingsViewModel(
                container.authRepository,
                container.favouriteRepository,
                container.listingRepository,
            )
            TrustActionsViewModel::class.java -> TrustActionsViewModel(
                container.authRepository,
                container.reportRepository,
                container.blockRepository,
            )
            BlockedUsersViewModel::class.java -> BlockedUsersViewModel(
                container.authRepository,
                container.blockRepository,
            )
            UserProfileViewModel::class.java -> UserProfileViewModel(
                handle,
                container.authRepository,
                container.userRepository,
                container.reviewRepository,
            )
            SmartDecisionViewModel::class.java -> SmartDecisionViewModel(
                container.authRepository,
                container.userRepository,
                container.vendorRepository,
                container.aiRepository,
                container.flowPrefill,
            )
            SajjaChatViewModel::class.java -> SajjaChatViewModel(
                container.authRepository,
                container.userRepository,
                container.listingRepository,
                container.aiRepository,
            )
            ReviewViewModel::class.java -> ReviewViewModel(
                checkNotNull(extras[ReviewParamsKey]) { "ReviewViewModel needs ReviewParams" },
                container.authRepository,
                container.reviewRepository,
            )
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
