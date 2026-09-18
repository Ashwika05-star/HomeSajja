package com.homesajja.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.homesajja.app.data.model.PurchaseStatus
import com.homesajja.app.ui.components.AppTextField
import com.homesajja.app.ui.components.AppTopBar
import com.homesajja.app.ui.components.CategoryChip
import com.homesajja.app.ui.components.EmptyState
import com.homesajja.app.ui.components.ErrorState
import com.homesajja.app.ui.components.FurnitureCard
import com.homesajja.app.ui.components.LoadingState
import com.homesajja.app.ui.components.NoResultsState
import com.homesajja.app.ui.components.OutlinedButton
import com.homesajja.app.ui.components.PrimaryButton
import com.homesajja.app.ui.components.PurchaseStatusTracker
import com.homesajja.app.ui.components.SecondaryButton
import com.homesajja.app.ui.components.StatusBadge

/**
 * Debug-only screen that renders every design-system component in one place for
 * visual review. Not part of the real navigation flow — reached only via a
 * debug-gated entry point on the Welcome screen.
 */
@Composable
fun ComponentPreviewScreen() {
    var textValue by remember { mutableStateOf("") }
    var selectedChip by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        AppTopBar(title = "Component Library")

        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            PreviewSection("Buttons") {
                PrimaryButton(text = "Primary Button", onClick = {}, modifier = Modifier.fillMaxWidth())
                SecondaryButton(text = "Secondary Button", onClick = {}, modifier = Modifier.fillMaxWidth())
                OutlinedButton(text = "Outlined Button", onClick = {}, modifier = Modifier.fillMaxWidth())
            }

            PreviewSection("Text field") {
                AppTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    label = "Full name",
                    modifier = Modifier.fillMaxWidth(),
                )
                AppTextField(
                    value = "",
                    onValueChange = {},
                    label = "Email",
                    isError = true,
                    errorMessage = "Enter a valid email address",
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            PreviewSection("Furniture card") {
                FurnitureCard(
                    title = "Teakwood Dining Table",
                    price = "₹8,500",
                    subtitle = "Mumbai · Good",
                    modifier = Modifier.width(180.dp),
                )
            }

            PreviewSection("Category chips") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Sofas", "Tables", "Chairs").forEachIndexed { index, label ->
                        CategoryChip(
                            label = label,
                            selected = selectedChip == index,
                            onClick = { selectedChip = index },
                        )
                    }
                }
            }

            PreviewSection("Status badges") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusBadge(status = "Pending")
                    StatusBadge(status = "Accepted")
                    StatusBadge(status = "Rejected")
                }
            }

            PreviewSection("Purchase status tracker") {
                PurchaseStatusTracker(status = PurchaseStatus.ACCEPTED)
                PurchaseStatusTracker(status = PurchaseStatus.CANCELLED)
            }

            PreviewSection("Loading state") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
                ) {
                    LoadingState()
                }
            }

            PreviewSection("Empty state") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
                ) {
                    EmptyState(
                        icon = Icons.Filled.Inventory2,
                        title = "No listings yet",
                        subtitle = "Items you list for sale will show up here.",
                        actionLabel = "Add listing",
                        onActionClick = {},
                    )
                }
            }

            PreviewSection("Error state") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
                ) {
                    ErrorState(
                        icon = Icons.Filled.ErrorOutline,
                        message = "Couldn't load your listings. Check your connection and try again.",
                        onRetry = {},
                    )
                }
            }

            PreviewSection("No results state") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
                ) {
                    NoResultsState(icon = Icons.Filled.SearchOff)
                }
            }
        }
    }
}

@Composable
private fun PreviewSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}
