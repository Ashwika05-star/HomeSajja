package com.homesajja.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.homesajja.app.di.LocalAppContainer
import com.homesajja.app.repository.GoogleSignInException
import com.homesajja.app.repository.GoogleSignInFailure
import com.homesajja.app.repository.googleFailureMessage
import kotlinx.coroutines.launch

/**
 * The "Continue with Google" button for Login and Signup. It shows a spinner label while Google's sheet is open, and puts any
 * failure (including from the Firebase step, via [error]) in a banner directly under the button, so it can never fail silently.
 */
@Composable
fun GoogleSignInButton(
    text: String,
    enabled: Boolean,
    error: String?,
    onStarted: () -> Unit,
    onIdToken: (String) -> Unit,
    onFailed: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var opening by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            text = if (opening) "Opening Google…" else text,
            enabled = enabled && !opening,
            onClick = {
                onStarted()
                opening = true
                scope.launch {
                    try {
                        container.googleSignInManager(context).requestIdToken().fold(
                            onSuccess = onIdToken,
                            onFailure = { e ->
                                val failure = (e as? GoogleSignInException)?.failure ?: GoogleSignInFailure.OTHER
                                onFailed(googleFailureMessage(failure))
                            },
                        )
                    } finally {
                        opening = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        if (error != null) InlineErrorBanner(message = error)
    }
}
