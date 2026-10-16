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
import com.homesajja.app.ui.i18n.AppLanguage
import kotlinx.coroutines.launch

/**
 * Lets the vendor toggle between English and Hindi (हिन्दी).
 * Persisted in DataStore so the choice is remembered across restarts.
 */
@Composable
fun LanguageCard(modifier: Modifier = Modifier) {
    val preference = LocalAppContainer.current.languagePreference
    val currentLanguage by preference.languageFlow.collectAsState(initial = AppLanguage.ENGLISH)
    val scope = rememberCoroutineScope()
    val isHindi = currentLanguage.isHindi

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                if (isHindi) "भाषा (Language)" else "Language (भाषा)",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            SegmentedControl(
                options = AppLanguage.entries,
                selected = currentLanguage,
                label = { it.nativeName },
                onSelect = { picked -> scope.launch { preference.setLanguage(picked) } },
            )
            Text(
                if (isHindi) "विक्रेता अनुभाग की भाषा हिन्दी चुनी गई है।"
                else "Choose English or Hindi for the vendor experience.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
