package com.homesajja.app.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * The app's bottom navigation, used by both spaces. [NavigationBar] pads itself for the system navigation bar or gesture area,
 * so it is never overlapped. [selected] is null on hub pages that aren't one of the tabs.
 */
@Composable
fun <T> BottomTabBar(
    tabs: List<T>,
    selected: T?,
    label: (T) -> String,
    icon: (T) -> ImageVector,
    onSelect: (T) -> Unit,
) {
    NavigationBar {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                icon = { Icon(icon(tab), contentDescription = null) },
                label = { Text(label(tab)) },
            )
        }
    }
}
