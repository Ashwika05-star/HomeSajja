package com.homesajja.app.repository

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

/** Wraps Credential Manager's Google ID flow. Needs a real Firebase Web Client
 * ID (see strings.xml/default_web_client_id) to actually authenticate — until
 * then this will fail with a caught [GetCredentialException], surfaced to the
 * caller as a [Result.failure] rather than a crash. */
class GoogleSignInManager(private val context: Context, private val webClientId: String) {

    suspend fun requestIdToken(): Result<String> = runCatching {
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()

        val credential = CredentialManager.create(context)
            .getCredential(context, request)
            .credential

        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            error("Unexpected credential type returned by Credential Manager.")
        }

        try {
            GoogleIdTokenCredential.createFrom(credential.data).idToken
        } catch (e: GoogleIdTokenParsingException) {
            throw IllegalStateException("Could not parse the Google ID token.", e)
        }
    }
}
