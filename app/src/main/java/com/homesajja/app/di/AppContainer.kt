package com.homesajja.app.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage

/**
 * Manual dependency container for HomeSajja.
 *
 * We use manual DI instead of Hilt: the dependency graph is small (a handful of
 * Firebase services plus repositories built on top of them), so wiring it by hand
 * keeps every dependency explicit and easy to trace during a viva walkthrough,
 * without Hilt's annotation-processing setup.
 */
class AppContainer {
    val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }
    val functions: FirebaseFunctions by lazy { FirebaseFunctions.getInstance() }
}
