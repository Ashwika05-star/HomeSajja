package com.homesajja.app.repository

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.CancellationException
import java.security.MessageDigest

private const val TAG = "HomeSajjaGoogle"

/** Why Google sign-in didn't produce a token, in terms a person (or a teammate setting up) can act on. */
enum class GoogleSignInFailure { CANCELLED, NO_ACCOUNT, PLAY_SERVICES, NETWORK, OTHER }

class GoogleSignInException(val failure: GoogleSignInFailure, cause: Throwable? = null) : Exception(failure.name, cause)

/** Turns whatever Credential Manager threw into one of our failure kinds. Pure, so it can be unit-tested. */
fun classifyGoogleFailure(error: Throwable): GoogleSignInFailure = when (error) {
    is GetCredentialCancellationException -> GoogleSignInFailure.CANCELLED
    is NoCredentialException -> GoogleSignInFailure.NO_ACCOUNT
    is GetCredentialProviderConfigurationException, is GetCredentialUnsupportedException -> GoogleSignInFailure.PLAY_SERVICES
    is GetCredentialInterruptedException -> GoogleSignInFailure.NETWORK
    else -> GoogleSignInFailure.OTHER
}

/** The sentence shown on screen. Every failure has one, so the button can never fail silently. */
fun googleFailureMessage(failure: GoogleSignInFailure): String = when (failure) {
    GoogleSignInFailure.CANCELLED -> "Google sign-in was cancelled. Tap the button to try again."
    GoogleSignInFailure.NO_ACCOUNT ->
        "No Google account could be used. Add a Google account to this phone and try again. " +
            "If you already have one, this app build's signing key may not be registered in Firebase yet."
    GoogleSignInFailure.PLAY_SERVICES ->
        "Google Play services is missing or out of date on this phone, so Google sign-in can't open. Update it from the Play Store, or use email instead."
    GoogleSignInFailure.NETWORK -> "Google sign-in was interrupted. Check your connection and try again."
    GoogleSignInFailure.OTHER ->
        "Google sign-in couldn't start. Please try again, or use email instead. If it keeps happening, this app build's signing key may not be registered in Firebase."
}

/**
 * Wraps Credential Manager's "Sign in with Google" flow. It uses [GetSignInWithGoogleOption], Google's button flow, which
 * lets the person pick or add a Google account; every failure is logged in full and returned as a [GoogleSignInException].
 */
class GoogleSignInManager(private val context: Context, private val webClientId: String) {

    suspend fun requestIdToken(): Result<String> {
        val activity = context.findActivity()
            ?: return Result.failure(GoogleSignInException(GoogleSignInFailure.OTHER, IllegalStateException("Not an Activity context")))
        return try {
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(GetSignInWithGoogleOption.Builder(webClientId).build())
                .build()
            val credential = CredentialManager.create(activity).getCredential(activity, request).credential

            if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                throw IllegalStateException("Unexpected credential type: ${credential.type}")
            }
            Result.success(GoogleIdTokenCredential.createFrom(credential.data).idToken)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val failure = classifyGoogleFailure(if (e is GoogleIdTokenParsingException) IllegalStateException(e) else e)
            // The real error and the fingerprint to register in Firebase, for whoever is debugging.
            Log.e(TAG, "Google sign-in failed ($failure). Web client id: $webClientId. This build's signing SHA-1: ${signingSha1(context)}", e)
            Result.failure(GoogleSignInException(failure, e))
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** The SHA-1 of the key this build is signed with, as Firebase shows it (AA:BB:...), or null if it can't be read. */
@Suppress("DEPRECATION")
private fun signingSha1(context: Context): String? = runCatching {
    val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            .signingInfo?.apkContentsSigners
    } else {
        context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES).signatures
    }
    info?.firstOrNull()?.toByteArray()?.let { bytes ->
        MessageDigest.getInstance("SHA-1").digest(bytes).joinToString(":") { "%02X".format(it) }
    }
}.getOrNull()
