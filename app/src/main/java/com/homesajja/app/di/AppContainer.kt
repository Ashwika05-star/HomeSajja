package com.homesajja.app.di

import android.content.Context
import com.homesajja.app.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import com.homesajja.app.repository.AuthRepository
import com.homesajja.app.repository.GoogleSignInManager
import com.homesajja.app.repository.SessionRepository
import com.homesajja.app.repository.UserRepository
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
    val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }
    val functions: FirebaseFunctions by lazy { FirebaseFunctions.getInstance() }

    val sessionRepository: SessionRepository by lazy { SessionRepository(appContext) }
    val authRepository: AuthRepository by lazy { AuthRepository(firebaseAuth) }
    val userRepository: UserRepository by lazy { UserRepository(firestore) }
    val vendorRepository: VendorRepository by lazy { VendorRepository(firestore) }

    fun googleSignInManager(context: Context): GoogleSignInManager {
        val webClientId = context.getString(R.string.default_web_client_id)
        return GoogleSignInManager(context, webClientId)
    }
}
