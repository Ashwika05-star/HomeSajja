package com.homesajja.app.ui.screens.repair

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.homesajja.app.data.model.FurnitureCategory
import com.homesajja.app.data.model.FurnitureListing
import com.homesajja.app.data.model.RepairProblemType
import com.homesajja.app.ui.components.AppTextField
import com.homesajja.app.ui.components.CategoryChip
import com.homesajja.app.ui.components.ItemThumbnail
import com.homesajja.app.ui.components.PhotoPicker
import com.homesajja.app.ui.components.StepTitle
import com.homesajja.app.viewmodel.LoadState
import com.homesajja.app.viewmodel.MAX_PHOTOS
import com.homesajja.app.viewmodel.MIN_REPAIR_DESCRIPTION
import com.homesajja.app.viewmodel.RepairForm
import com.homesajja.app.viewmodel.RepairRequestViewModel
import com.homesajja.app.viewmodel.RepairStep

@Composable
internal fun RepairStepContent(viewModel: RepairRequestViewModel) {
    val form = viewModel.form
    when (viewModel.step) {
        RepairStep.FURNITURE -> FurnitureStep(viewModel)
        RepairStep.PHOTOS -> {
            StepTitle(
                "Photos of the damage",
                if (form.listing != null) "We started with your listing's photos. Add close-ups of the problem."
                else "1 to $MAX_PHOTOS photos.",
            )
            PhotoPicker(photos = form.photos, onAdd = viewModel::addPhotos, onRemove = viewModel::removePhoto)
        }
        RepairStep.PROBLEM -> ProblemStep(form, onSelect = viewModel::selectProblem)
        RepairStep.DESCRIPTION -> {
            StepTitle("Describe the problem", "What happened, and where is the damage?")
            AppTextField(
                value = form.description,
                onValueChange = { v -> viewModel.update { it.copy(description = v) } },
                label = "Description",
                placeholder = "e.g. Front left leg cracked after a move",
                singleLine = false,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "At least $MIN_REPAIR_DESCRIPTION characters.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        RepairStep.LOCATION -> LocationStep(form)
        RepairStep.PROVIDER -> ProviderStep(viewModel)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FurnitureStep(viewModel: RepairRequestViewModel) {
    val form = viewModel.form
    StepTitle("Which furniture needs repair?", "Pick one of your items, or add one that isn't listed.")

    when (val listings = viewModel.listings) {
        LoadState.Loading -> CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        is LoadState.Error -> InlineRetry(listings.message, onRetry = viewModel::retryListings)
        is LoadState.Loaded -> {
            if (listings.items.isEmpty()) {
                Text(
                    "You have no listings yet. Use \"Furniture not listed\" below.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            listings.items.forEach { listing ->
                ListingChoice(listing, selected = form.listing?.id == listing.id, onClick = { viewModel.chooseListing(listing) })
            }
        }
    }

    Card(
        onClick = viewModel::chooseNotListed,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (form.notListed) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Build, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("Furniture not listed", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 12.dp))
        }
    }

    if (form.notListed) {
        Text("Category", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FurnitureCategory.entries.forEach { category ->
                CategoryChip(
                    label = category.displayName,
                    selected = form.category == category,
                    onClick = { viewModel.update { it.copy(category = category) } },
                )
            }
        }
        AppTextField(
            value = form.name,
            onValueChange = { v -> viewModel.update { it.copy(name = v) } },
            label = "Name (optional)",
            placeholder = "e.g. Grandma's rocking chair",
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ListingChoice(listing: FurnitureListing, selected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (selected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ItemThumbnail(
                imageUrl = listing.images.firstOrNull(),
                description = listing.title,
                modifier = Modifier.width(88.dp).clip(RoundedCornerShape(10.dp)),
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(listing.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listing.category.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProblemStep(form: RepairForm, onSelect: (RepairProblemType) -> Unit) {
    StepTitle("What's the problem?", "Pick the closest match.")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RepairProblemType.entries.forEach { problem ->
            CategoryChip(label = problem.displayName, selected = form.problem == problem, onClick = { onSelect(problem) })
        }
    }
}

@Composable
private fun LocationStep(form: RepairForm) {
    StepTitle("Confirm your location", "We'll look for repair providers in your city.")
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Text(
                form.city.ifBlank { "City not set" },
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

@Composable
private fun ProviderStep(viewModel: RepairRequestViewModel) {
    val form = viewModel.form
    StepTitle("Choose a repair provider", "Your request goes to the provider you pick.")
    when (val providers = viewModel.providers) {
        LoadState.Loading -> Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
        }
        is LoadState.Error -> InlineRetry(providers.message, onRetry = viewModel::retryProviders)
        is LoadState.Loaded -> {
            if (providers.items.isEmpty()) {
                Text(
                    "No repair providers in ${form.city} yet. Check back soon.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }
            providers.items.forEach { provider ->
                ProviderCard(provider, selected = form.provider?.uid == provider.uid, onClick = { viewModel.selectProvider(provider) })
            }
        }
    }
}

@Composable
private fun InlineRetry(message: String, onRetry: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        com.homesajja.app.ui.components.OutlinedButton(text = "Retry", onClick = onRetry)
    }
}
