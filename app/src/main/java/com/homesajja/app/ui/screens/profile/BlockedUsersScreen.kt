package com.homesajja.app.ui.screens.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.homesajja.app.di.LocalAppContainer
import com.homesajja.app.di.ViewModelFactory
import com.homesajja.app.ui.components.AppTopBar
import com.homesajja.app.ui.components.EmptyState
import com.homesajja.app.ui.components.ErrorState
import com.homesajja.app.ui.components.LoadingState
import com.homesajja.app.viewmodel.BlockedUiState
import com.homesajja.app.viewmodel.BlockedUsersViewModel

/** The people you've blocked, each with an Unblock button. */
@Composable
fun BlockedUsersScreen(onBackClick: () -> Unit) {
    val viewModel: BlockedUsersViewModel = viewModel(factory = ViewModelFactory(LocalAppContainer.current))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = { AppTopBar(title = "Blocked people", onBackClick = onBackClick) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val current = state) {
                BlockedUiState.Loading -> LoadingState()
                is BlockedUiState.Error -> ErrorState(message = current.message, onRetry = viewModel::retry)
                is BlockedUiState.Content -> {
                    if (current.blocked.isEmpty()) {
                        EmptyState(
                            icon = Icons.Filled.Block,
                            title = "You haven't blocked anyone",
                            subtitle = "If someone bothers you, use the menu on their profile or chat to block them.",
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(current.blocked, key = { it.id }) { block ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(block.blockedName.ifBlank { "Someone" }, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                    TextButton(onClick = { viewModel.unblock(block) }) { Text("Unblock") }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
