package com.homesajja.app.repository

import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import com.homesajja.app.navigation.HomeTabState
import com.homesajja.app.navigation.UserTab
import com.homesajja.app.navigation.VendorTab
import java.net.UnknownHostException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleFailureTest {

    @Test
    fun eachCredentialManagerError_mapsToItsOwnKind() {
        assertEquals(GoogleSignInFailure.CANCELLED, classifyGoogleFailure(GetCredentialCancellationException("x")))
        assertEquals(GoogleSignInFailure.NO_ACCOUNT, classifyGoogleFailure(NoCredentialException("x")))
        assertEquals(GoogleSignInFailure.PLAY_SERVICES, classifyGoogleFailure(GetCredentialProviderConfigurationException("x")))
        assertEquals(GoogleSignInFailure.PLAY_SERVICES, classifyGoogleFailure(GetCredentialUnsupportedException("x")))
        assertEquals(GoogleSignInFailure.NETWORK, classifyGoogleFailure(GetCredentialInterruptedException("x")))
        assertEquals(GoogleSignInFailure.OTHER, classifyGoogleFailure(IllegalStateException("boom")))
    }

    @Test
    fun everyFailureHasAMessageSoTheButtonNeverFailsSilently() {
        GoogleSignInFailure.entries.forEach { assertTrue("$it has no message", googleFailureMessage(it).isNotBlank()) }
        // Setup problems point the person at the signing key, which was the real cause of the original bug.
        assertTrue(googleFailureMessage(GoogleSignInFailure.NO_ACCOUNT).contains("signing key"))
        assertTrue(googleFailureMessage(GoogleSignInFailure.OTHER).contains("signing key"))
    }
}

// Same simple names as the Firebase AI SDK's exception types, which is how classifyAiFailure recognises them.
private class QuotaExceededException(message: String) : Exception(message)
private class ServiceDisabledException(message: String) : Exception(message)
private class ServerException(message: String) : Exception(message)

class AiFailureTest {

    @Test
    fun notEnabled_isRecognisedFromTheRealErrors() {
        assertEquals(AiFailure.NOT_ENABLED, classifyAiFailure(ServerException("Firebase AI Logic genai config not found. Learn more: https://x")))
        assertEquals(AiFailure.NOT_ENABLED, classifyAiFailure(ServiceDisabledException("anything")))
        assertEquals(AiFailure.NOT_ENABLED, classifyAiFailure(ServerException("Firebase AI Logic API has not been used in project 123 before or it is disabled.")))
    }

    @Test
    fun quotaAndNetwork_areTheirOwnKinds() {
        assertEquals(AiFailure.QUOTA_EXCEEDED, classifyAiFailure(QuotaExceededException("x")))
        assertEquals(AiFailure.QUOTA_EXCEEDED, classifyAiFailure(ServerException("Quota exceeded for metric generate_content_requests")))
        assertEquals(AiFailure.NO_NETWORK, classifyAiFailure(UnknownHostException("Unable to resolve host")))
        assertEquals(AiFailure.NO_NETWORK, classifyAiFailure(RuntimeException("wrapper", UnknownHostException("x"))))
    }

    @Test
    fun aMissingModel_isNotConfusedWithAnUnenabledProject() {
        assertEquals(AiFailure.MODEL_UNAVAILABLE, classifyAiFailure(ServerException("models/gemini-9-flash is not found for API version v1beta")))
    }

    @Test
    fun aRetiredModel_isAModelProblem() {
        assertEquals(AiFailure.MODEL_UNAVAILABLE, classifyAiFailure(ServerException("This model models/gemini-2.5-flash is no longer available to new users.")))
    }

    @Test
    fun anOverloadedModel_isBusy_notAGenericError() {
        assertEquals(AiFailure.BUSY, classifyAiFailure(ServerException("This model is currently experiencing high demand. Spikes in demand are usually temporary.")))
    }

    @Test
    fun unknownErrors_fallBackToOther() {
        assertEquals(AiFailure.OTHER, classifyAiFailure(IllegalArgumentException("something odd")))
    }
}

class HomeTabStateTest {

    @Test
    fun reset_returnsBothSpacesToTheirFirstTab() {
        val tabs = HomeTabState()
        tabs.user.value = UserTab.SERVICES
        tabs.vendor.value = VendorTab.MATERIALS
        tabs.reset()
        assertEquals(UserTab.EXPLORE, tabs.user.value)
        assertEquals(VendorTab.DASHBOARD, tabs.vendor.value)
    }
}
