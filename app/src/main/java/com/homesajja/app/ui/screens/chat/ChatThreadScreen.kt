package com.homesajja.app.ui.screens.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.homesajja.app.data.model.Chat
import com.homesajja.app.data.model.Message
import com.homesajja.app.di.LocalAppContainer
import com.homesajja.app.di.ViewModelFactory
import com.homesajja.app.notification.NotificationRouting
import com.homesajja.app.ui.components.AppTopBar
import com.homesajja.app.ui.components.ErrorState
import com.homesajja.app.ui.components.ItemThumbnail
import com.homesajja.app.ui.components.LoadingState
import com.homesajja.app.ui.components.TrustMenu
import com.homesajja.app.viewmodel.TrustActionsViewModel
import com.homesajja.app.ui.util.formatMessageTime
import com.homesajja.app.viewmodel.ChatThreadUiState
import com.homesajja.app.viewmodel.ChatThreadViewModel

/** One conversation: the record it is about pinned at the top, live message bubbles, and the message box. */
@Composable
fun ChatThreadScreen(onBackClick: () -> Unit, onOpenRoute: (String) -> Unit) {
    val viewModel: ChatThreadViewModel = viewModel(factory = ViewModelFactory(LocalAppContainer.current))
    // The same instance the menu in the top bar uses, so blocking there updates this screen straight away.
    val trust: TrustActionsViewModel = viewModel(factory = ViewModelFactory(LocalAppContainer.current))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    val loaded = state as? ChatThreadUiState.Content
    val title = loaded?.let { it.chat.otherParticipantName(it.myId) } ?: "Chat"
    val otherId = loaded?.let { it.chat.otherParticipantId(it.myId) }
    Scaffold(
        topBar = {
            AppTopBar(
                title = title,
                onBackClick = onBackClick,
                actions = { if (otherId != null) TrustMenu(userId = otherId, userName = title) },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.imePadding(),
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val current = state) {
                ChatThreadUiState.Loading -> LoadingState()
                is ChatThreadUiState.Error -> ErrorState(message = current.message, onRetry = viewModel::retry)
                is ChatThreadUiState.Content -> Thread(
                    content = current,
                    iBlockedThem = otherId != null && otherId in trust.blockedIds,
                    draft = viewModel.draft,
                    onDraftChange = viewModel::onDraftChange,
                    onSend = viewModel::send,
                    onOpenContext = {
                        NotificationRouting.routeFor(current.chat.contextType, current.chat.contextId)?.let(onOpenRoute)
                    },
                )
            }
        }
    }
}

@Composable
private fun Thread(
    content: ChatThreadUiState.Content,
    iBlockedThem: Boolean,
    draft: String,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onOpenContext: () -> Unit,
) {
    val listState = rememberLazyListState()
    val messages = content.messages

    // Stay at the newest message when the thread opens and whenever one arrives.
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ContextCard(content.chat, onClick = onOpenContext)

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (messages.isEmpty()) {
                Text(
                    "No messages yet. Say hello!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(messages, key = { it.id }) { message -> Bubble(message, mine = message.senderId == content.myId) }
                }
            }
        }

        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 6.dp) {
            if (iBlockedThem) {
                Text(
                    "You've blocked ${content.chat.otherParticipantName(content.myId)}. Unblock them from the menu to send messages.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                )
            } else Row(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    placeholder = { Text("Message") },
                    maxLines = 4,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onSend, enabled = draft.isNotBlank()) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

/** What this chat is about; tapping it goes to that listing or request. */
@Composable
private fun ContextCard(chat: Chat, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ItemThumbnail(
                imageUrl = chat.contextImage,
                description = chat.contextTitle,
                modifier = Modifier.size(width = 64.dp, height = 48.dp).clip(RoundedCornerShape(8.dp)),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(chat.contextType.chatContextLabel(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(chat.contextTitle, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text("View", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun Bubble(message: Message, mine: Boolean) {
    val scheme = MaterialTheme.colorScheme
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
        Surface(
            color = if (mine) scheme.primary else scheme.surface,
            contentColor = if (mine) scheme.onPrimary else scheme.onSurface,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = if (mine) 16.dp else 4.dp, bottomEnd = if (mine) 4.dp else 16.dp),
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(message.text, style = MaterialTheme.typography.bodyMedium)
                Text(
                    formatMessageTime(message.sentAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = (if (mine) scheme.onPrimary else scheme.onSurfaceVariant).copy(alpha = 0.75f),
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
    }
}
