package com.homesajja.app.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.homesajja.app.di.LocalAppContainer
import com.homesajja.app.di.ViewModelFactory
import com.homesajja.app.viewmodel.InboxBadgeViewModel

/** The Chats and Notifications buttons for a home top bar, each with a live count of what is waiting. */
@Composable
fun RowScope.InboxActions(onOpenChats: () -> Unit, onOpenNotifications: () -> Unit) {
    val viewModel: InboxBadgeViewModel = viewModel(factory = ViewModelFactory(LocalAppContainer.current))
    val counts by viewModel.counts.collectAsStateWithLifecycle()

    IconButton(onClick = onOpenChats, modifier = Modifier.semantics { contentDescription = "Chats, ${counts.unreadChats} unread" }) {
        BadgedBox(badge = { CountBadge(counts.unreadChats) }) {
            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null)
        }
    }
    IconButton(
        onClick = onOpenNotifications,
        modifier = Modifier.semantics { contentDescription = "Notifications, ${counts.unseenNotifications} unread" },
    ) {
        BadgedBox(badge = { CountBadge(counts.unseenNotifications) }) {
            Icon(Icons.Filled.Notifications, contentDescription = null)
        }
    }
}

@Composable
private fun CountBadge(count: Int) {
    if (count > 0) Badge { Text(if (count > 9) "9+" else "$count") }
}
