package com.homesajja.app.ui.screens

import com.homesajja.app.ui.components.PillTabs
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

/** The service sections a user can switch between: Repair and Recycle. */
enum class ServicesSection(val label: String) {
    REPAIR("Repair"),
    RECYCLE("Recycle"),
}

/** The Services tab: the user's repair and recycling requests. */
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
        PillTabs(
            labels = ServicesSection.entries.map { it.label },
            selectedIndex = section.ordinal,
            onSelect = { onSectionChange(ServicesSection.entries[it]) },
        )
        when (section) {
            ServicesSection.REPAIR -> MyRepairsScreen(onOpenRequest = onOpenRepair, onRequestRepair = onRequestRepair)
            ServicesSection.RECYCLE -> MyRecyclingScreen(onOpenRequest = onOpenRecycling, onRecycle = onRecycle)
        }
    }
}
