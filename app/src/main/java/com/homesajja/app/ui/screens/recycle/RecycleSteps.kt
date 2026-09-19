package com.homesajja.app.ui.screens.recycle

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.homesajja.app.data.model.RecycleCondition
import com.homesajja.app.data.model.RecycleMaterial
import com.homesajja.app.data.model.RecycleMethod
import com.homesajja.app.ui.components.CategoryChip
import com.homesajja.app.ui.components.OutlinedButton
import com.homesajja.app.ui.components.PhotoPicker
import com.homesajja.app.ui.components.StepTitle
import com.homesajja.app.viewmodel.LoadState
import com.homesajja.app.viewmodel.MAX_PHOTOS
import com.homesajja.app.viewmodel.RecycleForm
import com.homesajja.app.viewmodel.RecycleRequestViewModel
import com.homesajja.app.viewmodel.RecycleStep

@Composable
internal fun RecycleStepContent(viewModel: RecycleRequestViewModel, onOpenVendor: (String) -> Unit) {
    val form = viewModel.form
    when (viewModel.step) {
        RecycleStep.PHOTOS -> {
            StepTitle("Photos of the furniture", "1 to $MAX_PHOTOS photos, so the recycler can see what it is.")
            PhotoPicker(photos = form.photos, onAdd = viewModel::addPhotos, onRemove = viewModel::removePhoto)
        }
        RecycleStep.CONDITION -> {
            StepTitle("What condition is it in?", "This tells the recycler what can be done with it.")
            RecycleCondition.entries.forEach { condition ->
                ChoiceCard(condition.displayName, condition.hint, form.condition == condition) { viewModel.selectCondition(condition) }
            }
        }
        RecycleStep.MATERIAL -> MaterialStep(form, onSelect = viewModel::selectMaterial)
        RecycleStep.METHOD -> {
            StepTitle("How will it get to the recycler?")
            ChoiceCard("Pickup", "A recycler collects it from you", form.method == RecycleMethod.PICKUP) { viewModel.selectMethod(RecycleMethod.PICKUP) }
            ChoiceCard("Drop-off", "You take it to a recycler in your city", form.method == RecycleMethod.DROP_OFF) { viewModel.selectMethod(RecycleMethod.DROP_OFF) }
        }
        RecycleStep.DESTINATION ->
            if (form.method == RecycleMethod.DROP_OFF) RecyclerStep(viewModel, onOpenVendor) else PickupLocationStep(form)
        RecycleStep.CONFIRM -> ConfirmStep(form)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MaterialStep(form: RecycleForm, onSelect: (RecycleMaterial) -> Unit) {
    StepTitle("What is it mostly made of?", "Pick the closest match.")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RecycleMaterial.entries.forEach { material ->
            CategoryChip(label = material.displayName, selected = form.material == material, onClick = { onSelect(material) })
        }
    }
}

@Composable
private fun ChoiceCard(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PickupLocationStep(form: RecycleForm) {
    StepTitle("Confirm your location", "A recycler in your city will collect it.")
    CityCard(form.city)
}

@Composable
private fun CityCard(city: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Text(city.ifBlank { "City not set" }, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 12.dp))
        }
    }
}

@Composable
private fun RecyclerStep(viewModel: RecycleRequestViewModel, onOpenVendor: (String) -> Unit) {
    val form = viewModel.form
    StepTitle("Choose a recycler", "Recyclers in ${form.city.ifBlank { "your city" }} that accept drop-offs.")
    when (val recyclers = viewModel.recyclers) {
        LoadState.Loading -> Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
        }
        is LoadState.Error -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(recyclers.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            OutlinedButton(text = "Retry", onClick = viewModel::retryRecyclers)
        }
        is LoadState.Loaded -> {
            if (recyclers.items.isEmpty()) {
                Text(
                    "No recyclers in ${form.city} yet. Go back and choose pickup instead, or check back soon.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }
            recyclers.items.forEach { recycler ->
                RecyclerCard(recycler, rating = viewModel.ratings[recycler.uid], selected = form.recycler?.uid == recycler.uid, onClick = { viewModel.selectRecycler(recycler) }, onViewProfile = { onOpenVendor(recycler.uid) })
            }
        }
    }
}

@Composable
private fun ConfirmStep(form: RecycleForm) {
    StepTitle("Confirm your request", "Check the details, then submit.")
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryRow("Photos", "${form.photos.size}")
            SummaryRow("Condition", form.condition?.displayName.orEmpty())
            SummaryRow("Material", form.material?.displayName.orEmpty())
            SummaryRow("Handover", form.method?.displayName.orEmpty())
            SummaryRow("City", form.city)
            if (form.method == RecycleMethod.DROP_OFF) {
                SummaryRow("Drop off at", form.recycler?.let { it.businessName.ifBlank { it.name } }.orEmpty())
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
