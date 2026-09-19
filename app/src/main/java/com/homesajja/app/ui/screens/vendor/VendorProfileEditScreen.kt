package com.homesajja.app.ui.screens.vendor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.homesajja.app.data.model.VendorBusinessType
import com.homesajja.app.di.LocalAppContainer
import com.homesajja.app.di.ViewModelFactory
import com.homesajja.app.ui.components.AppDropdownField
import com.homesajja.app.ui.components.AppTextField
import com.homesajja.app.ui.components.AppTopBar
import com.homesajja.app.ui.components.ErrorState
import com.homesajja.app.ui.components.InlineErrorBanner
import com.homesajja.app.ui.components.LoadingState
import com.homesajja.app.ui.components.LocationPickerMap
import com.homesajja.app.ui.components.OutlinedButton
import com.homesajja.app.ui.components.PhotoPicker
import com.homesajja.app.ui.components.PrimaryButton
import com.homesajja.app.viewmodel.VendorEditScreenState
import com.homesajja.app.viewmodel.VendorProfileEditViewModel
import com.homesajja.app.viewmodel.VendorSaveState

/** Set up or edit the shop: business details, the map pin for the shop location, and brochure photos. */
@Composable
fun VendorProfileEditScreen(onBackClick: () -> Unit) {
    val viewModel: VendorProfileEditViewModel = viewModel(factory = ViewModelFactory(LocalAppContainer.current))
    val saveState = viewModel.saveState

    LaunchedEffect(saveState) {
        if (saveState is VendorSaveState.Saved) onBackClick()
    }

    Scaffold(
        topBar = { AppTopBar(title = "Shop profile", onBackClick = onBackClick) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val screen = viewModel.screenState) {
                VendorEditScreenState.Loading -> LoadingState()
                is VendorEditScreenState.Error -> ErrorState(message = screen.message, onRetry = viewModel::load)
                VendorEditScreenState.Ready -> EditForm(viewModel)
            }
        }
    }

    when (saveState) {
        is VendorSaveState.Saving -> AlertDialog(
            onDismissRequest = {},
            title = { Text("Saving") },
            text = {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                    Text(saveState.progress)
                }
            },
            confirmButton = {},
        )
        is VendorSaveState.Failed -> AlertDialog(
            onDismissRequest = viewModel::dismissSaveError,
            title = { Text("Couldn't save") },
            text = { Text(saveState.message) },
            confirmButton = { TextButton(onClick = viewModel::save) { Text("Try again") } },
            dismissButton = { TextButton(onClick = viewModel::dismissSaveError) { Text("Close") } },
        )
        VendorSaveState.Idle, VendorSaveState.Saved -> Unit
    }
}

@Composable
private fun EditForm(viewModel: VendorProfileEditViewModel) {
    val form = viewModel.form
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AppTextField(
            value = form.businessName,
            onValueChange = { v -> viewModel.update { it.copy(businessName = v) } },
            label = "Business name",
            modifier = Modifier.fillMaxWidth(),
        )
        AppDropdownField(
            label = "Business type",
            selectedOption = form.businessType?.displayName.orEmpty(),
            options = VendorBusinessType.entries.map { it.displayName },
            onOptionSelected = { selected ->
                viewModel.update { f -> f.copy(businessType = VendorBusinessType.entries.firstOrNull { it.displayName == selected }) }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        AppTextField(
            value = form.description,
            onValueChange = { v -> viewModel.update { it.copy(description = v) } },
            label = "About your business",
            singleLine = false,
            modifier = Modifier.fillMaxWidth(),
        )

        Text("Shop location", style = MaterialTheme.typography.titleMedium)
        Text(
            "Move the map so the pin sits on your shop, then tap \"Set location\". Customers in ${viewModel.city} see this pin on your profile.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LocationPickerMap(
            latitude = viewModel.pinLatitude,
            longitude = viewModel.pinLongitude,
            hasPin = form.latitude != null,
            onCenterChanged = viewModel::onPinMoved,
            modifier = Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(16.dp)),
        )
        OutlinedButton(text = "Set location to the pin", onClick = viewModel::setLocationToPin, modifier = Modifier.fillMaxWidth())
        Text(
            text = if (form.latitude != null && form.longitude != null) {
                "Shop location set (%.5f, %.5f)".format(form.latitude, form.longitude)
            } else {
                "No shop location set yet."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AppTextField(
            value = form.shopAddress,
            onValueChange = { v -> viewModel.update { it.copy(shopAddress = v) } },
            label = "Shop address (optional)",
            placeholder = "e.g. 12 MG Road, Andheri East",
            modifier = Modifier.fillMaxWidth(),
        )

        Text("Brochure & catalogue photos", style = MaterialTheme.typography.titleMedium)
        PhotoPicker(photos = form.brochure, onAdd = viewModel::addBrochure, onRemove = viewModel::removeBrochure)

        viewModel.formError?.let { InlineErrorBanner(message = it) }
        PrimaryButton(text = "Save profile", onClick = viewModel::save, modifier = Modifier.fillMaxWidth())
    }
}
