package com.homesajja.app.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.homesajja.app.data.model.Chat
import com.homesajja.app.data.model.EntityType
import com.homesajja.app.di.LocalAppContainer
import com.homesajja.app.di.ViewModelFactory
import com.homesajja.app.ui.components.AppTopBar
import com.homesajja.app.ui.components.CategoryChip
import com.homesajja.app.ui.components.EmptyState
import com.homesajja.app.ui.components.ErrorState
import com.homesajja.app.ui.components.ItemThumbnail
import com.homesajja.app.ui.components.LoadingState
import com.homesajja.app.ui.components.NoResultsState
import com.homesajja.app.ui.util.formatTimeAgo
import com.homesajja.app.viewmodel.ChatFilter
import com.homesajja.app.viewmodel.ChatListUiState
import com.homesajja.app.viewmodel.ChatListViewModel
import com.homesajja.app.viewmodel.apply

/** Short label for what a chat is about. */
fun EntityType.chatContextLabel(): String = when (this) {
    EntityType.LISTING -> "Listing"
    EntityType.EXCHANGE_REQUEST -> "Exchange"
    EntityType.REPAIR_REQUEST -> "Repair"
    EntityType.PURCHASE_REQUEST -> "Purchase"
    EntityType.RECYCLING_REQUEST -> "Recycling"
    EntityType.MATERIAL_REQUEST -> "Material request"
    EntityType.CHAT, EntityType.REVIEW -> "Chat"
}

/** Every chat the signed-in person is in, across listings, exchanges and repairs, with an unread dot. */
@Composable
fun ChatListScreen(onBackClick: () -> Unit, onOpenChat: (String) -> Unit) {
    val viewModel: ChatListViewModel = viewModel(factory = ViewModelFactory(LocalAppContainer.current))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { AppTopBar(title = "Chats", onBackClick = onBackClick) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val current = state) {
                ChatListUiState.Loading -> LoadingState()
                is ChatListUiState.Error -> ErrorState(message = current.message, onRetry = viewModel::retry)
                is ChatListUiState.Content -> {
                    if (current.chats.isEmpty()) {
                        EmptyState(
                            icon = Icons.AutoMirrored.Filled.Chat,
                            title = "No chats yet",
                            subtitle = "Tap Chat on a listing, or open an accepted exchange or a repair, to start talking.",
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ChatFilter.entries.forEach { option ->
                                CategoryChip(label = option.label, selected = viewModel.filter == option, onClick = { viewModel.selectFilter(option) })
                            }
                        }
                        val visible = viewModel.filter.apply(current.chats, current.myId)
                        if (visible.isEmpty()) {
                            NoResultsState(
                                icon = Icons.AutoMirrored.Filled.Chat,
                                title = "No unread chats",
                                subtitle = "You're all caught up.",
                                actionLabel = "Show all",
                                onActionClick = { viewModel.selectFilter(ChatFilter.ALL) },
                            )
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(visible, key = { it.id }) { chat ->
                                    ChatRow(chat, current.myId, onClick = { onOpenChat(chat.id) })
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatRow(chat: Chat, myId: String, onClick: () -> Unit) {
    val unread = chat.isUnreadFor(myId)
    val weight = if (unread) FontWeight.Bold else FontWeight.Normal
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ItemThumbnail(
            imageUrl = chat.contextImage,
            description = chat.contextTitle,
            modifier = Modifier.size(width = 72.dp, height = 54.dp).clip(RoundedCornerShape(10.dp)),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                chat.otherParticipantName(myId),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = weight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${chat.contextType.chatContextLabel()} · ${chat.contextTitle}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                (if (chat.lastMessageSenderId == myId) "You: " else "") + chat.lastMessage,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = weight,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(formatTimeAgo(chat.lastMessageAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (unread) {
                Box(modifier = Modifier.size(10.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
            }
        }
    }
}
