package com.homesajja.app.ui.screens.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.homesajja.app.data.model.FurnitureCondition
import com.homesajja.app.data.model.ItemState
import com.homesajja.app.data.model.ListingFilters
import com.homesajja.app.data.model.MaterialType
import com.homesajja.app.ui.components.OutlinedButton
import com.homesajja.app.ui.components.PrimaryButton
import com.homesajja.app.ui.util.formatPrice

private const val PRICE_STEP = 1_000L

/** The slider is continuous; values are rounded to the nearest ₹1,000 when shown and applied. */
private fun snapToStep(value: Float): Long = (Math.round(value / PRICE_STEP) * PRICE_STEP)

/** Bottom sheet for price range, condition, material and new/used/refurbished.
 * Edits a draft; nothing changes on Explore until Apply. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterSheet(
    filters: ListingFilters,
    onApply: (ListingFilters) -> Unit,
    onDismiss: () -> Unit,
) {
    val max = ListingFilters.PRICE_SLIDER_MAX.toFloat()
    var draft by remember { mutableStateOf(filters) }
    var priceRange by remember {
        mutableStateOf((filters.minPrice ?: 0L).toFloat()..(filters.maxPrice ?: ListingFilters.PRICE_SLIDER_MAX).toFloat())
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Filters", style = MaterialTheme.typography.titleLarge)

            SectionTitle("Price range")
            val lower = snapToStep(priceRange.start)
            val upperValue = snapToStep(priceRange.endInclusive)
            val upper = if (upperValue >= ListingFilters.PRICE_SLIDER_MAX) "${formatPrice(upperValue)}+" else formatPrice(upperValue)
            Text(
                "${formatPrice(lower)} – $upper",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            RangeSlider(
                value = priceRange,
                onValueChange = { priceRange = it },
                valueRange = 0f..max,
            )

            SectionTitle("Item")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OptionChip("Any", selected = draft.itemState == null) { draft = draft.copy(itemState = null) }
                ItemState.entries.forEach { state ->
                    OptionChip(state.displayName, selected = draft.itemState == state) {
                        draft = draft.copy(itemState = state)
                    }
                }
            }

            SectionTitle("Condition")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FurnitureCondition.entries.forEach { condition ->
                    OptionChip(condition.displayName, selected = condition in draft.conditions) {
                        draft = draft.copy(conditions = draft.conditions.toggle(condition))
                    }
                }
            }

            SectionTitle("Material")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MaterialType.entries.forEach { material ->
                    OptionChip(material.displayName, selected = material in draft.materials) {
                        draft = draft.copy(materials = draft.materials.toggle(material))
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    text = "Reset",
                    onClick = {
                        draft = ListingFilters()
                        priceRange = 0f..max
                    },
                    modifier = Modifier.weight(1f),
                )
                PrimaryButton(
                    text = "Apply",
                    onClick = {
                        onApply(
                            draft.copy(
                                minPrice = snapToStep(priceRange.start).takeIf { it > 0 },
                                maxPrice = snapToStep(priceRange.endInclusive).takeIf { it < ListingFilters.PRICE_SLIDER_MAX },
                            ),
                        )
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun OptionChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    )
}

private fun <T> Set<T>.toggle(item: T): Set<T> = if (item in this) this - item else this + item
