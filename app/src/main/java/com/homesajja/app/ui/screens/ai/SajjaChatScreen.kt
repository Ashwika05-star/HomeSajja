package com.homesajja.app.ui.screens.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.homesajja.app.di.LocalAppContainer
import com.homesajja.app.di.ViewModelFactory
import com.homesajja.app.ui.components.AppTopBar
import com.homesajja.app.ui.components.CategoryChip
import com.homesajja.app.ui.components.FurnitureCard
import com.homesajja.app.ui.util.priceLabel
import com.homesajja.app.viewmodel.SAJJA_SUGGESTIONS
import com.homesajja.app.viewmodel.SajjaChatViewModel
import com.homesajja.app.viewmodel.SajjaMessage

/** Ask Sajja anything about furniture. When it points to listings, they appear under its answer. */
@Composable
fun SajjaChatScreen(onBackClick: () -> Unit, onOpenListing: (String) -> Unit) {
    val viewModel: SajjaChatViewModel = viewModel(factory = ViewModelFactory(LocalAppContainer.current))
    val listState = rememberLazyListState()
    val messages = viewModel.messages

    // Stay at the newest message, or the "thinking" line, as the conversation grows.
    LaunchedEffect(messages.size, viewModel.thinking) {
        val last = messages.size + (if (viewModel.thinking) 1 else 0) - 1
        if (last >= 0) listState.animateScrollToItem(last)
    }

    Scaffold(
        topBar = { AppTopBar(title = "Sajja AI", onBackClick = onBackClick) },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.imePadding(),
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (messages.isEmpty()) {
                    Welcome(onSuggestion = viewModel::sendSuggestion)
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(messages.size) { index ->
                            MessageBlock(messages[index], onOpenListing, onRetry = viewModel::retry, isLast = index == messages.lastIndex)
                        }
                        if (viewModel.thinking) item { Bubble(SajjaMessage(fromUser = false, text = "Thinking…")) }
                    }
                }
            }
            Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 6.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = viewModel.draft,
                        onValueChange = viewModel::onDraftChange,
                        placeholder = { Text("Ask about furniture…") },
                        maxLines = 4,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = viewModel::send, enabled = viewModel.draft.isNotBlank() && !viewModel.thinking) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Welcome(onSuggestion: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text("Hi, I'm Sajja", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
        Text(
            "Ask me to find furniture, price something, or decide between repairing and recycling.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SAJJA_SUGGESTIONS.forEach { suggestion -> CategoryChip(label = suggestion, selected = false, onClick = { onSuggestion(suggestion) }) }
        }
    }
}

@Composable
private fun MessageBlock(message: SajjaMessage, onOpenListing: (String) -> Unit, onRetry: () -> Unit, isLast: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Bubble(message)
        if (message.isError && isLast) TextButton(onClick = onRetry) { Text("Try again") }
        message.listings.forEach { listing ->
            FurnitureCard(
                title = listing.title,
                price = listing.priceLabel(),
                imageUrl = listing.images.firstOrNull(),
                subtitle = "${listing.category.displayName} · ${listing.city}",
                onClick = { onOpenListing(listing.id) },
                modifier = Modifier.widthIn(max = 260.dp),
            )
        }
    }
}

@Composable
private fun Bubble(message: SajjaMessage) {
    val scheme = MaterialTheme.colorScheme
    val mine = message.fromUser
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
        Surface(
            color = when {
                mine -> scheme.primary
                message.isError -> scheme.errorContainer
                else -> scheme.surface
            },
            contentColor = when {
                mine -> scheme.onPrimary
                message.isError -> scheme.onErrorContainer
                else -> scheme.onSurface
            },
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = if (mine) 16.dp else 4.dp, bottomEnd = if (mine) 4.dp else 16.dp),
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            Text(message.text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
        }
    }
}
