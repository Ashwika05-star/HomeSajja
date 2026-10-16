package com.homesajja.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.homesajja.app.di.LocalAppContainer
import com.homesajja.app.ui.i18n.AppLanguage
import com.homesajja.app.ui.i18n.LocalAppLanguage
import kotlinx.coroutines.launch

/**
 * A one-tap language toggle for the vendor top bar.
 * Tapping it switches between English and Hindi immediately.
 */
@Composable
fun LanguageToggleButton(modifier: Modifier = Modifier) {
    val preference = LocalAppContainer.current.languagePreference
    val language = LocalAppLanguage.current
    val scope = rememberCoroutineScope()
    val isHindi = language.isHindi

    OutlinedButton(
        onClick = {
            scope.launch {
                preference.setLanguage(if (isHindi) AppLanguage.ENGLISH else AppLanguage.HINDI)
            }
        },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
        modifier = modifier,
    ) {
        Text(
            text = if (isHindi) "EN" else "हिन्दी",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
