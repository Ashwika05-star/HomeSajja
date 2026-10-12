package com.homesajja.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.homesajja.app.di.LocalAppContainer
import com.homesajja.app.ui.theme.ThemeMode
import kotlinx.coroutines.launch

/**
 * The Light / Dark / System choice. The pick is saved on the phone and applied at once; System follows the phone's own
 * light or dark setting.
 */
@Composable
fun AppearanceCard(modifier: Modifier = Modifier) {
    val preference = LocalAppContainer.current.themePreference
    val mode by preference.modeFlow.collectAsState(initial = ThemeMode.SYSTEM)
    val scope = rememberCoroutineScope()

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Appearance", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            SegmentedControl(
                options = ThemeMode.entries,
                selected = mode,
                label = { it.displayName },
                onSelect = { picked -> scope.launch { preference.setMode(picked) } },
            )
            Text(
                "System follows your phone's light or dark setting.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
