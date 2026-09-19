package com.homesajja.app.di

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import com.homesajja.app.BuildConfig
import com.homesajja.app.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.messaging.FirebaseMessaging
import com.homesajja.app.notification.SessionServices
import com.homesajja.app.data.model.FlowPrefill
import com.homesajja.app.repository.AiRepository
import com.homesajja.app.repository.AuthRepository
import com.homesajja.app.repository.BlockRepository
import com.homesajja.app.repository.GeminiAiRepository
import com.homesajja.app.repository.ReportRepository
import com.homesajja.app.repository.DeviceTokenRepository
import com.homesajja.app.repository.NotificationSender
import com.homesajja.app.repository.ChatRepository
import com.homesajja.app.repository.ExchangeRepository
import com.homesajja.app.repository.FavouriteRepository
import com.homesajja.app.repository.GoogleSignInManager
import com.homesajja.app.repository.ImageRepository
import com.homesajja.app.repository.ListingRepository
import com.homesajja.app.repository.MaterialRequestRepository
import com.homesajja.app.repository.NotificationRepository
import com.homesajja.app.repository.PurchaseRequestRepository
import com.homesajja.app.repository.RecyclingRepository
import com.homesajja.app.repository.RepairRepository
import com.homesajja.app.repository.ReviewRepository
import com.homesajja.app.repository.SessionRepository
import com.homesajja.app.repository.UserRepository
import com.homesajja.app.repository.VendorInboxRepository
import com.homesajja.app.repository.VendorRepository

/**
 * Manual dependency container for HomeSajja.
 *
 * We use manual DI instead of Hilt: the dependency graph is small (a handful of
 * Firebase services plus repositories built on top of them), so wiring it by hand
 * keeps every dependency explicit and easy to trace during a viva walkthrough,
 * without Hilt's annotation-processing setup.
 */
class AppContainer(private val appContext: Context) {

    // Debug-only: -PuseEmulator=true points Auth and Firestore at the
    // local emulators (10.0.2.2 is the host machine as seen from the Android emulator).
    private val useEmulator = BuildConfig.DEBUG && BuildConfig.USE_FIREBASE_EMULATOR

    val firebaseAuth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance().also { if (useEmulator) it.useEmulator(EMULATOR_HOST, 9099) }
    }
    val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance().also { if (useEmulator) it.useEmulator(EMULATOR_HOST, 8080) }
    }
    val functions: FirebaseFunctions by lazy { FirebaseFunctions.getInstance() }

    val sessionRepository: SessionRepository by lazy { SessionRepository(appContext) }
    val authRepository: AuthRepository by lazy { AuthRepository(firebaseAuth) }
    val userRepository: UserRepository by lazy { UserRepository(firestore) }
    val vendorRepository: VendorRepository by lazy { VendorRepository(firestore) }
    val listingRepository: ListingRepository by lazy { ListingRepository(firestore) }
    val imageRepository: ImageRepository by lazy {
        ImageRepository(
            appContext.contentResolver,
            appContext.getString(R.string.cloudinary_cloud_name),
            appContext.getString(R.string.cloudinary_upload_preset),
        )
    }
    val purchaseRequestRepository: PurchaseRequestRepository by lazy { PurchaseRequestRepository(firestore) }
    val exchangeRepository: ExchangeRepository by lazy { ExchangeRepository(firestore) }
    val repairRepository: RepairRepository by lazy { RepairRepository(firestore) }
    val recyclingRepository: RecyclingRepository by lazy { RecyclingRepository(firestore) }
    val materialRequestRepository: MaterialRequestRepository by lazy { MaterialRequestRepository(firestore) }
    val chatRepository: ChatRepository by lazy { ChatRepository(firestore) }
    val notificationRepository: NotificationRepository by lazy { NotificationRepository(firestore) }
    val deviceTokenRepository: DeviceTokenRepository by lazy { DeviceTokenRepository(firestore, FirebaseMessaging.getInstance()) }

    /** Lives as long as the app process, so notifications still get written after the screen that caused them closes. */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val notificationSender: NotificationSender by lazy { NotificationSender(notificationRepository, appScope) }
    val sessionServices: SessionServices by lazy {
        SessionServices(appContext, appScope, authRepository, notificationRepository, deviceTokenRepository)
    }

    val reportRepository: ReportRepository by lazy { ReportRepository(firestore) }
    val blockRepository: BlockRepository by lazy { BlockRepository(firestore) }
    val aiRepository: AiRepository by lazy {
        GeminiAiRepository(appContext.contentResolver, appContext.getString(R.string.gemini_model))
    }

    /** What the Smart Decision screen hands to the Sell, Repair or Recycle flow; the flow takes it once and clears it. */
    val flowPrefill = MutableStateFlow<FlowPrefill?>(null)

    /** A screen to open once the app is signed in and showing, set when a system notification is tapped. */
    val pendingRoute = MutableStateFlow<String?>(null)
    val reviewRepository: ReviewRepository by lazy { ReviewRepository(firestore) }
    val vendorInboxRepository: VendorInboxRepository by lazy {
        VendorInboxRepository(purchaseRequestRepository, exchangeRepository, repairRepository, recyclingRepository)
    }
    val favouriteRepository: FavouriteRepository by lazy { FavouriteRepository(firestore) }

    fun googleSignInManager(context: Context): GoogleSignInManager {
        val webClientId = context.getString(R.string.default_web_client_id)
        return GoogleSignInManager(context, webClientId)
    }

    private companion object {
        const val EMULATOR_HOST = "10.0.2.2"
    }
}
