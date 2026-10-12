package com.homesajja.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.homesajja.app.ui.theme.AmberDot

/**
 * The app's bottom navigation, used by both spaces. [NavigationBar] pads itself for the system navigation bar or gesture area,
 * so it is never overlapped. [selected] is null on hub pages that aren't one of the tabs. The selected destination has a
 * primary-coloured icon and label and a small amber dot under the icon.
 */
@Composable
fun <T> BottomTabBar(
    tabs: List<T>,
    selected: T?,
    label: (T) -> String,
    icon: (T) -> ImageVector,
    onSelect: (T) -> Unit,
) {
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        NavigationBar(containerColor = MaterialTheme.colorScheme.background, tonalElevation = 0.dp) {
            tabs.forEach { tab ->
                val isSelected = tab == selected
                NavigationBarItem(
                    selected = isSelected,
                    onClick = { onSelect(tab) },
                    icon = {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(icon(tab), contentDescription = null)
                            if (isSelected) {
                                Box(
                                    Modifier
                                        .align(Alignment.BottomCenter)
                                        .offset(y = 7.dp)
                                        .size(5.dp)
                                        .background(AmberDot, CircleShape),
                                )
                            }
                        }
                    },
                    label = { Text(label(tab)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = Color.Transparent,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}
