package com.homesajja.app.ui.screens.sell

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.homesajja.app.data.model.Cities
import com.homesajja.app.data.model.FurnitureCategory
import com.homesajja.app.data.model.FurnitureCondition
import com.homesajja.app.data.model.FurnitureDimensions
import com.homesajja.app.data.model.ListingActionType
import com.homesajja.app.data.model.MaterialType
import com.homesajja.app.ui.components.AppDropdownField
import com.homesajja.app.ui.components.AppTextField
import com.homesajja.app.ui.components.CategoryChip
import com.homesajja.app.ui.components.ImageCarousel
import com.homesajja.app.ui.util.displayText
import com.homesajja.app.ui.util.formatAge
import com.homesajja.app.ui.util.formatPrice
import com.homesajja.app.viewmodel.MAX_PHOTOS
import com.homesajja.app.viewmodel.SellForm
import com.homesajja.app.viewmodel.SellPhoto
import com.homesajja.app.viewmodel.SellStep
import com.homesajja.app.viewmodel.SellViewModel

@Composable
internal fun SellStepContent(viewModel: SellViewModel) {
    val form = viewModel.form
    val update = viewModel::update
    when (viewModel.step) {
        SellStep.CATEGORY -> CategoryStep(form, update)
        SellStep.PHOTOS -> PhotosStep(form, onAdd = viewModel::addPhotos, onRemove = viewModel::removePhoto)
        SellStep.DETAILS -> DetailsStep(form, update)
        SellStep.CONDITION -> ConditionStep(form, update)
        SellStep.PRICE -> PriceStep(form, update)
        SellStep.LOCATION -> LocationStep(form, update)
        SellStep.PREVIEW -> PreviewStep(form)
    }
}

private typealias FormUpdate = ((SellForm) -> SellForm) -> Unit

@Composable
private fun StepTitle(title: String, hint: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        if (hint != null) {
            Text(hint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryStep(form: SellForm, update: FormUpdate) {
    StepTitle("What are you selling?", "Pick the closest category.")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FurnitureCategory.entries.forEach { category ->
            CategoryChip(
                label = category.displayName,
                selected = form.category == category,
                onClick = { update { it.copy(category = category) } },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PhotosStep(form: SellForm, onAdd: (List<Uri>) -> Unit, onRemove: (Int) -> Unit) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(MAX_PHOTOS)) { uris ->
        if (uris.isNotEmpty()) onAdd(uris)
    }

    StepTitle("Add photos", "1 to $MAX_PHOTOS photos. The first one is the cover.")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        form.photos.forEachIndexed { index, photo ->
            Box(modifier = Modifier.size(104.dp)) {
                AsyncImage(
                    model = when (photo) {
                        is SellPhoto.Remote -> photo.url
                        is SellPhoto.Local -> photo.uri
                    },
                    contentDescription = "Photo ${index + 1}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(104.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), RoundedCornerShape(12.dp)),
                )
                IconButton(
                    onClick = { onRemove(index) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(28.dp)
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f), CircleShape),
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove photo ${index + 1}", modifier = Modifier.size(16.dp))
                }
            }
        }
        if (form.photos.size < MAX_PHOTOS) {
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(12.dp))
                    .clickable {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Add", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun DetailsStep(form: SellForm, update: FormUpdate) {
    StepTitle("Tell buyers about it")
    AppTextField(
        value = form.title,
        onValueChange = { v -> update { it.copy(title = v) } },
        label = "Title",
        placeholder = "e.g. Teak 3-seater sofa",
        modifier = Modifier.fillMaxWidth(),
    )
    AppTextField(
        value = form.description,
        onValueChange = { v -> update { it.copy(description = v) } },
        label = "Description",
        singleLine = false,
        minLines = 3,
        modifier = Modifier.fillMaxWidth(),
    )
    AppDropdownField(
        label = "Main material",
        selectedOption = form.material?.displayName.orEmpty(),
        options = MaterialType.entries.map { it.displayName },
        onOptionSelected = { name ->
            update { f -> f.copy(material = MaterialType.entries.first { it.displayName == name }) }
        },
        modifier = Modifier.fillMaxWidth(),
    )
    AppTextField(
        value = form.ageYears,
        onValueChange = { v -> update { it.copy(ageYears = v.filter(Char::isDigit)) } },
        label = "Age (years)",
        placeholder = "0 if under a year",
        keyboardType = KeyboardType.Number,
        modifier = Modifier.fillMaxWidth(),
    )
    Text("Dimensions (cm, optional)", style = MaterialTheme.typography.labelLarge)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AppTextField(
            value = form.lengthCm,
            onValueChange = { v -> update { it.copy(lengthCm = v.filter(Char::isDigit)) } },
            label = "Length",
            keyboardType = KeyboardType.Number,
            modifier = Modifier.weight(1f),
        )
        AppTextField(
            value = form.widthCm,
            onValueChange = { v -> update { it.copy(widthCm = v.filter(Char::isDigit)) } },
            label = "Width",
            keyboardType = KeyboardType.Number,
            modifier = Modifier.weight(1f),
        )
        AppTextField(
            value = form.heightCm,
            onValueChange = { v -> update { it.copy(heightCm = v.filter(Char::isDigit)) } },
            label = "Height",
            keyboardType = KeyboardType.Number,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ConditionStep(form: SellForm, update: FormUpdate) {
    StepTitle("What condition is it in?")
    FurnitureCondition.entries.forEach { condition ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { update { it.copy(condition = condition) } }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = form.condition == condition, onClick = { update { it.copy(condition = condition) } })
            Text(condition.displayName, style = MaterialTheme.typography.bodyLarge)
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text("This item has been refurbished", style = MaterialTheme.typography.bodyLarge)
            Text(
                "Restored or repaired before selling.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = form.refurbished, onCheckedChange = { v -> update { it.copy(refurbished = v) } })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PriceStep(form: SellForm, update: FormUpdate) {
    val isExchange = form.actionType == ListingActionType.EXCHANGE
    StepTitle(
        title = if (isExchange) "List it for exchange" else "Set your price",
        hint = if (isExchange) {
            "People will offer you a swap instead of buying it. An estimated value is optional."
        } else {
            "Whole rupees. Buyers can also make lower offers."
        },
    )
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        ListingActionType.entries.forEachIndexed { index, type ->
            SegmentedButton(
                selected = form.actionType == type,
                onClick = { update { it.copy(actionType = type) } },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = ListingActionType.entries.size),
            ) {
                Text(type.displayName)
            }
        }
    }
    AppTextField(
        value = form.price,
        onValueChange = { v -> update { it.copy(price = v.filter(Char::isDigit)) } },
        label = if (isExchange) "Estimated value (₹, optional)" else "Price (₹)",
        keyboardType = KeyboardType.Number,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun LocationStep(form: SellForm, update: FormUpdate) {
    StepTitle("Confirm the location", "Listings are shown to people in this city. It defaults to your city.")
    AppDropdownField(
        label = "City",
        selectedOption = form.city,
        options = Cities.ALL,
        onOptionSelected = { city -> update { it.copy(city = city) } },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun PreviewStep(form: SellForm) {
    StepTitle("Preview", "This is how your listing will look.")
    ImageCarousel(
        images = form.photos.map {
            when (it) {
                is SellPhoto.Remote -> it.url
                is SellPhoto.Local -> it.uri
            }
        },
        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            if (form.actionType == ListingActionType.EXCHANGE) "For exchange" else formatPrice(form.price.trim().toLongOrNull() ?: 0),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(form.title.trim(), style = MaterialTheme.typography.titleLarge)
        Text(form.city, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Text(form.description.trim(), style = MaterialTheme.typography.bodyLarge)

    val dimensions = FurnitureDimensions(
        lengthCm = form.lengthCm.trim().toIntOrNull(),
        widthCm = form.widthCm.trim().toIntOrNull(),
        heightCm = form.heightCm.trim().toIntOrNull(),
    )
    val rows = listOfNotNull(
        "Category" to form.category?.displayName.orEmpty(),
        "Material" to form.material?.displayName.orEmpty(),
        "Condition" to (form.condition?.displayName.orEmpty() + if (form.refurbished) " · Refurbished" else ""),
        "Age" to formatAge(form.ageYears.trim().toIntOrNull() ?: 0),
        dimensions.displayText()?.let { "Dimensions" to it },
    )
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            rows.forEachIndexed { index, (label, value) ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                    Text(
                        label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(110.dp),
                    )
                    Text(value, style = MaterialTheme.typography.bodyMedium)
                }
                if (index != rows.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}
