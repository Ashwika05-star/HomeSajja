package com.homesajja.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.homesajja.app.ui.i18n.isAppHindi
import java.util.Calendar

/**
 * Amazon-style date range filters for request and completed item lists.
 * Supports: All time, Last 30 days, Last 3 months, and the last 5 years.
 */
sealed interface DateFilter {
    val displayName: String
    fun matches(timestamp: Long): Boolean
    fun label(isHindi: Boolean): String

    data object All : DateFilter {
        override val displayName: String = "All time"
        override fun matches(timestamp: Long): Boolean = true
        override fun label(isHindi: Boolean): String = if (isHindi) "सभी समय" else "All time"
    }

    data object Last30Days : DateFilter {
        override val displayName: String = "Last 30 days"
        override fun matches(timestamp: Long): Boolean {
            val cutoff = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000L
            return timestamp >= cutoff
        }
        override fun label(isHindi: Boolean): String = if (isHindi) "पिछले 30 दिन" else "Last 30 days"
    }

    data object Last3Months : DateFilter {
        override val displayName: String = "Last 3 months"
        override fun matches(timestamp: Long): Boolean {
            val cutoff = System.currentTimeMillis() - 90L * 24 * 60 * 60 * 1000L
            return timestamp >= cutoff
        }
        override fun label(isHindi: Boolean): String = if (isHindi) "पिछले 3 महीने" else "Last 3 months"
    }

    data class Year(val year: Int) : DateFilter {
        override val displayName: String = year.toString()

        override fun matches(timestamp: Long): Boolean {
            val cal = Calendar.getInstance()
            cal.set(year, Calendar.JANUARY, 1, 0, 0, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val start = cal.timeInMillis

            cal.set(year, Calendar.DECEMBER, 31, 23, 59, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val end = cal.timeInMillis

            return timestamp in start..end
        }

        override fun label(isHindi: Boolean): String = year.toString()
    }

    companion object {
        fun defaultOptions(currentYear: Int = Calendar.getInstance().get(Calendar.YEAR)): List<DateFilter> {
            val years = (0 until 5).map { Year(currentYear - it) }
            return listOf(All, Last30Days, Last3Months) + years
        }
    }
}

/**
 * A compact, Amazon-style filter dropdown showing the active date filter.
 */
@Composable
fun DateFilterDropdown(
    selected: DateFilter,
    onSelect: (DateFilter) -> Unit,
    modifier: Modifier = Modifier,
    options: List<DateFilter> = remember { DateFilter.defaultOptions() },
) {
    val isHindi = isAppHindi
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        FilterChip(
            selected = selected != DateFilter.All,
            onClick = { expanded = true },
            label = {
                Text(
                    text = "${if (isHindi) "अवधि: " else "Period: "}${selected.label(isHindi)}",
                    style = MaterialTheme.typography.labelMedium,
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.FilterList,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 4.dp),
                )
            },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (option == selected) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Text(
                                text = option.label(isHindi),
                                style = if (option == selected) {
                                    MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary)
                                } else {
                                    MaterialTheme.typography.bodyMedium
                                },
                            )
                        }
                    },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

/**
 * A horizontal scrollable chip row for quick Amazon-style date filtering.
 */
@Composable
fun DateFilterChipRow(
    selected: DateFilter,
    onSelect: (DateFilter) -> Unit,
    modifier: Modifier = Modifier,
    options: List<DateFilter> = remember { DateFilter.defaultOptions() },
) {
    val isHindi = isAppHindi
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(options) { option ->
            CategoryChip(
                label = option.label(isHindi),
                selected = option == selected,
                onClick = { onSelect(option) },
            )
        }
    }
}
