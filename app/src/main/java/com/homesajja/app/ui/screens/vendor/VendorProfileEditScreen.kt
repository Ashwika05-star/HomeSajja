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
import com.homesajja.app.ui.components.CopyableValue
import com.homesajja.app.ui.components.AppTopBar
import com.homesajja.app.ui.components.ErrorState
import com.homesajja.app.ui.components.InlineErrorBanner
import com.homesajja.app.ui.components.LoadingState
import com.homesajja.app.ui.components.LocationPickerMap
import com.homesajja.app.ui.components.OutlinedButton
import com.homesajja.app.ui.components.PhotoPicker
import com.homesajja.app.ui.components.PrimaryButton
import com.homesajja.app.ui.i18n.VendorStrings
import com.homesajja.app.ui.i18n.isAppHindi
import com.homesajja.app.viewmodel.VendorEditScreenState
import com.homesajja.app.viewmodel.VendorProfileEditViewModel
import com.homesajja.app.viewmodel.VendorSaveState

/** Set up or edit the shop: business details, the map pin for the shop location, and brochure photos. */
@Composable
fun VendorProfileEditScreen(onBackClick: () -> Unit) {
    val isHindi = isAppHindi
    val viewModel: VendorProfileEditViewModel = viewModel(factory = ViewModelFactory(LocalAppContainer.current))
    val saveState = viewModel.saveState

    LaunchedEffect(saveState) {
        if (saveState is VendorSaveState.Saved) onBackClick()
    }

    Scaffold(
        topBar = { AppTopBar(title = VendorStrings.shopProfileTitle(isHindi), onBackClick = onBackClick) },
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
            title = { Text(VendorStrings.saving(isHindi)) },
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
            title = { Text(VendorStrings.couldntSave(isHindi)) },
            text = { Text(saveState.message) },
            confirmButton = { TextButton(onClick = viewModel::save) { Text(VendorStrings.tryAgain(isHindi)) } },
            dismissButton = { TextButton(onClick = viewModel::dismissSaveError) { Text(VendorStrings.close(isHindi)) } },
        )
        VendorSaveState.Idle, VendorSaveState.Saved -> Unit
    }
}

@Composable
private fun EditForm(viewModel: VendorProfileEditViewModel) {
    val isHindi = isAppHindi
    val form = viewModel.form
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AppTextField(
            value = form.businessName,
            onValueChange = { v -> viewModel.update { it.copy(businessName = v) } },
            label = VendorStrings.businessNameLabel(isHindi),
            modifier = Modifier.fillMaxWidth(),
        )
        AppDropdownField(
            label = VendorStrings.businessTypeLabel(isHindi),
            selectedOption = form.businessType?.let { VendorStrings.businessType(it.name, isHindi) }.orEmpty(),
            options = VendorBusinessType.entries.map { VendorStrings.businessType(it.name, isHindi) },
            onOptionSelected = { selected ->
                viewModel.update { f ->
                    f.copy(businessType = VendorBusinessType.entries.firstOrNull { VendorStrings.businessType(it.name, isHindi) == selected })
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        AppTextField(
            value = form.description,
            onValueChange = { v -> viewModel.update { it.copy(description = v) } },
            label = VendorStrings.aboutBusinessLabel(isHindi),
            singleLine = false,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(VendorStrings.paymentsTitle(isHindi), style = MaterialTheme.typography.titleMedium)
        Text(
            VendorStrings.upiExplanation(isHindi),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AppTextField(
            value = form.upiId,
            onValueChange = { v -> viewModel.update { it.copy(upiId = v) } },
            label = VendorStrings.upiIdLabel(isHindi),
            placeholder = "name@bank",
            isError = form.upiIdInvalid,
            errorMessage = if (form.upiIdInvalid) VendorStrings.upiInvalidError(isHindi) else null,
            modifier = Modifier.fillMaxWidth(),
        )
        form.upiId.trim().takeIf { it.isNotEmpty() && !form.upiIdInvalid }?.let { CopyableValue(label = VendorStrings.customersPayTo(isHindi), value = it) }

        Text(VendorStrings.shopLocationTitle(isHindi), style = MaterialTheme.typography.titleMedium)
        Text(
            VendorStrings.shopLocationDesc(viewModel.city, isHindi),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LocationPickerMap(
            latitude = viewModel.pinLatitude,
            longitude = viewModel.pinLongitude,
            hasPin = form.latitude != null,
            onCenterChanged = viewModel::onPinMoved,
            modifier = Modifier.fillMaxWidth().height(260.dp).clip(MaterialTheme.shapes.medium),
        )
        OutlinedButton(text = VendorStrings.setLocationToPin(isHindi), onClick = viewModel::setLocationToPin, modifier = Modifier.fillMaxWidth())
        val lat = form.latitude
        val lng = form.longitude
        Text(
            text = if (lat != null && lng != null) {
                VendorStrings.shopLocationSet(lat, lng, isHindi)
            } else {
                VendorStrings.noShopLocationSet(isHindi)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AppTextField(
            value = form.shopAddress,
            onValueChange = { v -> viewModel.update { it.copy(shopAddress = v) } },
            label = VendorStrings.addressLabel(isHindi),
            placeholder = VendorStrings.addressPlaceholder(isHindi),
            modifier = Modifier.fillMaxWidth(),
        )

        Text(VendorStrings.brochurePhotosTitle(isHindi), style = MaterialTheme.typography.titleMedium)
        PhotoPicker(photos = form.brochure, onAdd = viewModel::addBrochure, onRemove = viewModel::removeBrochure)

        viewModel.formError?.let { InlineErrorBanner(message = it) }
        PrimaryButton(text = VendorStrings.saveProfile(isHindi), onClick = viewModel::save, modifier = Modifier.fillMaxWidth())
    }
}
