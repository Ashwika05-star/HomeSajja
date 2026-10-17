package com.homesajja.app.ui.screens.requests

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.homesajja.app.data.model.FurnitureCategory
import com.homesajja.app.ui.components.PrimaryButton
import com.homesajja.app.ui.i18n.VendorStrings
import com.homesajja.app.ui.i18n.isAppHindi

@Composable
fun ItemRequestCreateDialog(
    initialCity: String,
    onDismiss: () -> Unit,
    onSubmit: (title: String, category: FurnitureCategory, budget: Long, description: String, city: String) -> Unit,
) {
    val isHindi = isAppHindi
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(FurnitureCategory.TABLE) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var budgetText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var city by remember { mutableStateOf(initialCity) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = VendorStrings.requestItemTitle(isHindi),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = VendorStrings.requestItemSubtitle(isHindi),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Item Title
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        error = null
                    },
                    label = { Text(VendorStrings.whatAreYouLookingFor(isHindi)) },
                    placeholder = { Text(VendorStrings.itemTitleHint(isHindi)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Category Selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedCategory.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(if (isHindi) "श्रेणी" else "Category") },
                        trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { categoryExpanded = true },
                    )
                    DropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false },
                    ) {
                        FurnitureCategory.entries.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.displayName) },
                                onClick = {
                                    selectedCategory = cat
                                    categoryExpanded = false
                                },
                            )
                        }
                    }
                }

                // Target Budget
                OutlinedTextField(
                    value = budgetText,
                    onValueChange = {
                        budgetText = it.filter { ch -> ch.isDigit() }
                        error = null
                    },
                    label = { Text(VendorStrings.targetBudget(isHindi)) },
                    placeholder = { Text(VendorStrings.budgetHint(isHindi)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                // City
                OutlinedTextField(
                    value = city,
                    onValueChange = {
                        city = it
                        error = null
                    },
                    label = { Text(if (isHindi) "शहर" else "City") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(if (isHindi) "विवरण (आकार, रंग, सामग्री आदि)" else "Description & preferences") },
                    placeholder = { Text(VendorStrings.descriptionHint(isHindi)) },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                )

                error?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isBlank()) {
                        error = if (isHindi) "कृपया बताएं कि आप क्या ढूंढ रहे हैं" else "Please enter what you are looking for"
                        return@TextButton
                    }
                    val budget = budgetText.toLongOrNull() ?: 0L
                    if (budget <= 0L) {
                        error = if (isHindi) "कृपया एक मान्य बजट दर्ज करें" else "Please enter a valid target budget"
                        return@TextButton
                    }
                    if (city.isBlank()) {
                        error = if (isHindi) "कृपया शहर का नाम लिखें" else "Please enter your city"
                        return@TextButton
                    }
                    onSubmit(title.trim(), selectedCategory, budget, description.trim(), city.trim())
                },
            ) {
                Text(VendorStrings.requestItem(isHindi))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(VendorStrings.cancel(isHindi))
            }
        },
    )
}
