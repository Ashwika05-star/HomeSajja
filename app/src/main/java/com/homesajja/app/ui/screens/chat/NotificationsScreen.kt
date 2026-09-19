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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.homesajja.app.data.model.Notification
import com.homesajja.app.data.model.NotificationType
import com.homesajja.app.di.LocalAppContainer
import com.homesajja.app.di.ViewModelFactory
import com.homesajja.app.notification.NotificationRouting
import com.homesajja.app.ui.components.AppTopBar
import com.homesajja.app.ui.components.CategoryChip
import com.homesajja.app.ui.components.EmptyState
import com.homesajja.app.ui.components.ErrorState
import com.homesajja.app.ui.components.LoadingState
import com.homesajja.app.ui.components.NoResultsState
import com.homesajja.app.ui.util.formatTimeAgo
import com.homesajja.app.viewmodel.NotificationFilter
import com.homesajja.app.viewmodel.NotificationsUiState
import com.homesajja.app.viewmodel.NotificationsViewModel
import com.homesajja.app.viewmodel.apply

private fun NotificationType.icon(): ImageVector = when (this) {
    NotificationType.NEW_MESSAGE -> Icons.AutoMirrored.Filled.Chat
    NotificationType.NEW_OFFER -> Icons.Filled.LocalOffer
    NotificationType.EXCHANGE_REQUEST, NotificationType.EXCHANGE_ACCEPTED, NotificationType.EXCHANGE_DECLINED -> Icons.Filled.SwapHoriz
    NotificationType.REPAIR_UPDATE -> Icons.Filled.Build
    NotificationType.RECYCLING_UPDATE -> Icons.Filled.Recycling
    NotificationType.PURCHASE_UPDATE -> Icons.AutoMirrored.Filled.ReceiptLong
    NotificationType.LISTING_PUBLISHED -> Icons.Filled.AddPhotoAlternate
    NotificationType.MATERIAL_MATCH -> Icons.Filled.Inventory2
    NotificationType.VENDOR_RESPONSE -> Icons.Filled.Storefront
    NotificationType.SALE_COMPLETED -> Icons.Filled.CheckCircle
}

/** The person's notifications, newest first, with an unread dot. Tapping one marks it read and opens what it is about. */
@Composable
fun NotificationsScreen(onBackClick: () -> Unit, onOpenRoute: (String) -> Unit) {
    val viewModel: NotificationsViewModel = viewModel(factory = ViewModelFactory(LocalAppContainer.current))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val hasUnread = (state as? NotificationsUiState.Content)?.notifications?.any { !it.seen } == true

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Notifications",
                onBackClick = onBackClick,
                actions = { if (hasUnread) TextButton(onClick = viewModel::markAllRead) { Text("Mark all read") } },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val current = state) {
                NotificationsUiState.Loading -> LoadingState()
                is NotificationsUiState.Error -> ErrorState(message = current.message, onRetry = viewModel::retry)
                is NotificationsUiState.Content -> {
                    if (current.notifications.isEmpty()) {
                        EmptyState(
                            icon = Icons.Filled.Notifications,
                            title = "No notifications yet",
                            subtitle = "Messages, requests and updates about your items will show up here.",
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            NotificationFilter.entries.forEach { option ->
                                CategoryChip(label = option.label, selected = viewModel.filter == option, onClick = { viewModel.selectFilter(option) })
                            }
                        }
                        val visible = viewModel.filter.apply(current.notifications)
                        if (visible.isEmpty()) {
                            NoResultsState(
                                icon = Icons.Filled.Notifications,
                                title = "No unread notifications",
                                subtitle = "You're all caught up.",
                                actionLabel = "Show all",
                                onActionClick = { viewModel.selectFilter(NotificationFilter.ALL) },
                            )
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(visible, key = { it.id }) { notification ->
                                    NotificationRow(notification, onClick = {
                                        viewModel.markRead(notification)
                                        NotificationRouting.routeFor(notification.relatedType, notification.relatedId)?.let(onOpenRoute)
                                    })
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
private fun NotificationRow(notification: Notification, onClick: () -> Unit) {
    val unread = !notification.seen
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (unread) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.background)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(notification.type.icon(), contentDescription = notification.type.label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 2.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(notification.title, style = MaterialTheme.typography.titleSmall, fontWeight = if (unread) FontWeight.Bold else FontWeight.Normal)
            Text(notification.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatTimeAgo(notification.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (unread) Box(modifier = Modifier.padding(top = 6.dp).size(10.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
    }
}
