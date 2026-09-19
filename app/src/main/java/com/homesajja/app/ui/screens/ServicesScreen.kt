package com.homesajja.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.homesajja.app.ui.screens.recycle.MyRecyclingScreen
import com.homesajja.app.ui.screens.repair.MyRepairsScreen

/** The two service sections a user can switch between. Repair and Recycle stay separate systems; this only groups their lists. */
enum class ServicesSection(val label: String) {
    REPAIR("Repair"),
    RECYCLE("Recycle"),
}

/** The Services tab: the user's repair requests and recycling requests, one section at a time. */
@Composable
fun ServicesScreen(
    section: ServicesSection,
    onSectionChange: (ServicesSection) -> Unit,
    onOpenRepair: (String) -> Unit,
    onRequestRepair: () -> Unit,
    onOpenRecycling: (String) -> Unit,
    onRecycle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = section.ordinal, containerColor = MaterialTheme.colorScheme.background) {
            ServicesSection.entries.forEach { entry ->
                Tab(selected = section == entry, onClick = { onSectionChange(entry) }, text = { Text(entry.label) })
            }
        }
        when (section) {
            ServicesSection.REPAIR -> MyRepairsScreen(onOpenRequest = onOpenRepair, onRequestRepair = onRequestRepair)
            ServicesSection.RECYCLE -> MyRecyclingScreen(onOpenRequest = onOpenRecycling, onRecycle = onRecycle)
        }
    }
}
