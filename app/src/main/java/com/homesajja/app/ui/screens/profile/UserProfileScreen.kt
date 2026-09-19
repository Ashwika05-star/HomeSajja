package com.homesajja.app.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.homesajja.app.di.LocalAppContainer
import com.homesajja.app.di.ViewModelFactory
import com.homesajja.app.ui.components.ErrorState
import com.homesajja.app.ui.components.LoadingState
import com.homesajja.app.ui.components.OutlinedButton
import com.homesajja.app.ui.components.ReviewsSection
import com.homesajja.app.viewmodel.UserProfileUiState
import com.homesajja.app.viewmodel.UserProfileViewModel

/**
 * A person's profile with the reviews written about them. On your own profile it also links to your saved
 * furniture and blocked people, and has Log out. On someone else's, that's just their name and reviews
 * (their account details are private).
 */
@Composable
fun UserProfileScreen(
    modifier: Modifier = Modifier,
    onOpenSaved: () -> Unit = {},
    onOpenBlocked: () -> Unit = {},
    onLogout: () -> Unit = {},
) {
    val viewModel: UserProfileViewModel = viewModel(factory = ViewModelFactory(LocalAppContainer.current))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleResumeEffect(viewModel) {
        viewModel.refreshIfStale()
        onPauseOrDispose {}
    }

    when (val current = state) {
        UserProfileUiState.Loading -> LoadingState(modifier)
        is UserProfileUiState.Error -> ErrorState(message = current.message, onRetry = viewModel::retry, modifier = modifier)
        is UserProfileUiState.Content -> Column(
            modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(current.name, style = MaterialTheme.typography.headlineMedium)
                current.profile?.let {
                    Text(it.city, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(it.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (current.isOwn) {
                OutlinedButton(text = "Saved furniture", onClick = onOpenSaved, modifier = Modifier.fillMaxWidth())
                OutlinedButton(text = "Blocked people", onClick = onOpenBlocked, modifier = Modifier.fillMaxWidth())
            }
            ReviewsSection(current.reviews)
            if (current.isOwn) OutlinedButton(text = "Log out", onClick = onLogout, modifier = Modifier.fillMaxWidth())
        }
    }
}
