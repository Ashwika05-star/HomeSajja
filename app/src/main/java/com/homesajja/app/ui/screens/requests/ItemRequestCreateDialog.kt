package com.homesajja.app.ui.screens.requests

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.homesajja.app.data.model.Cities
import com.homesajja.app.data.model.FurnitureCategory
import com.homesajja.app.ui.components.AppDropdownField
import com.homesajja.app.ui.components.CategoryChip
import com.homesajja.app.ui.components.PrimaryButton
import com.homesajja.app.ui.i18n.VendorStrings
import com.homesajja.app.ui.i18n.isAppHindi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ItemRequestCreateDialog(
    initialCity: String,
    onDismiss: () -> Unit,
    onSubmit: (title: String, category: FurnitureCategory, budget: Long, description: String, city: String) -> Unit,
) {
    val isHindi = isAppHindi
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(FurnitureCategory.SOFA) }
    var budgetText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    // Normalize initialCity to match Cities.ALL if possible
    val defaultCity = Cities.ALL.firstOrNull { it.equals(initialCity.trim(), ignoreCase = true) }
        ?: Cities.ALL.first()
    var city by remember { mutableStateOf(defaultCity) }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header with icon, title and close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Inventory2,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Column {
                        Text(
                            text = if (isHindi) "कस्टम फर्नीचर का अनुरोध" else "Request Custom Furniture",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = if (isHindi) "स्थानीय कारीगर व विक्रेता आपका अनुरोध देखेंगे" else "Local verified vendors in your city will review & accept",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Category Selection (Clean chip row with CategoryChip)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (isHindi) "फर्नीचर श्रेणी चुनें" else "Select Category",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FurnitureCategory.entries.forEach { cat ->
                        CategoryChip(
                            label = cat.displayName,
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                        )
                    }
                }
            }

            // Item Title
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (isHindi) "आप क्या ढूंढ रहे हैं?" else "What furniture are you looking for?",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        error = null
                    },
                    placeholder = {
                        Text(if (isHindi) "जैसे: 6-सीटर सागौन डाइनिंग टेबल, स्टडी टेबल" else "e.g. 6-seater solid teak dining table, study desk")
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Target Budget and City in 2 columns or stacked cleanly
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Target Budget
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = if (isHindi) "अनुमानित बजट" else "Target Budget",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    OutlinedTextField(
                        value = budgetText,
                        onValueChange = {
                            budgetText = it.filter { ch -> ch.isDigit() }
                            error = null
                        },
                        prefix = { Text("₹ ", fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // City (Dropdown with Cities.ALL)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = if (isHindi) "शहर" else "City",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    AppDropdownField(
                        label = if (isHindi) "शहर चुनें" else "Select City",
                        selectedOption = city,
                        options = Cities.ALL,
                        onOptionSelected = {
                            city = it
                            error = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // Description / Specifications
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (isHindi) "विवरण व प्राथमिकताएं (वैकल्पिक)" else "Details & Specifications (Optional)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = {
                        Text(
                            if (isHindi)
                                "आकार, पसंदीदा लकड़ी, रंग या कोई विशेष आवश्यकता बताएं..."
                            else
                                "Mention dimensions, preferred wood/material, color, finish...",
                        )
                    },
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Error display
            error?.let { errText ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = errText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action Buttons
            PrimaryButton(
                text = if (isHindi) "अनुरोध सबमिट करें" else "Submit Request",
                onClick = {
                    if (title.isBlank()) {
                        error = if (isHindi) "कृपया बताएं कि आप क्या ढूंढ रहे हैं" else "Please enter what you are looking for"
                        return@PrimaryButton
                    }
                    val budget = budgetText.toLongOrNull() ?: 0L
                    if (budget <= 0L) {
                        error = if (isHindi) "कृपया एक मान्य बजट दर्ज करें" else "Please enter a valid target budget"
                        return@PrimaryButton
                    }
                    if (city.isBlank()) {
                        error = if (isHindi) "कृपया शहर चुनें" else "Please select a city"
                        return@PrimaryButton
                    }
                    onSubmit(title.trim(), selectedCategory, budget, description.trim(), city.trim())
                },
                modifier = Modifier.fillMaxWidth(),
            )

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = if (isHindi) "रद्द करें" else "Cancel",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
