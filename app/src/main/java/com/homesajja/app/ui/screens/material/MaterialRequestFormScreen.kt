package com.homesajja.app.ui.screens.material

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.homesajja.app.data.model.MaterialType
import com.homesajja.app.di.LocalAppContainer
import com.homesajja.app.di.ViewModelFactory
import com.homesajja.app.ui.components.AppTextField
import com.homesajja.app.ui.components.AppTopBar
import com.homesajja.app.ui.components.CategoryChip
import com.homesajja.app.ui.components.ErrorState
import com.homesajja.app.ui.components.InlineErrorBanner
import com.homesajja.app.ui.components.LoadingState
import com.homesajja.app.ui.components.PrimaryButton
import com.homesajja.app.viewmodel.MaterialFormScreenState
import com.homesajja.app.viewmodel.MaterialRequestFormViewModel
import com.homesajja.app.viewmodel.MaterialSaveState

/** Post a new material request, or edit one. */
@Composable
fun MaterialRequestFormScreen(onExit: () -> Unit, onSaved: (requestId: String) -> Unit) {
    val viewModel: MaterialRequestFormViewModel = viewModel(factory = ViewModelFactory(LocalAppContainer.current))
    val saveState = viewModel.saveState

    LaunchedEffect(saveState) {
        if (saveState is MaterialSaveState.Saved) onSaved(saveState.requestId)
    }

    Scaffold(
        topBar = { AppTopBar(title = if (viewModel.isEditing) "Edit request" else "New material request", onBackClick = onExit) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val screen = viewModel.screenState) {
                MaterialFormScreenState.Loading -> LoadingState()
                is MaterialFormScreenState.Error -> ErrorState(message = screen.message, onRetry = viewModel::load)
                MaterialFormScreenState.Ready -> FormContent(viewModel)
            }
        }
    }

    when (saveState) {
        MaterialSaveState.Saving -> AlertDialog(
            onDismissRequest = {},
            title = { Text("Saving") },
            text = { Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(modifier = Modifier.padding(4.dp)) } },
            confirmButton = {},
        )
        is MaterialSaveState.Failed -> AlertDialog(
            onDismissRequest = viewModel::dismissSaveError,
            title = { Text("Couldn't save") },
            text = { Text(saveState.message) },
            confirmButton = { TextButton(onClick = viewModel::save) { Text("Try again") } },
            dismissButton = { TextButton(onClick = viewModel::dismissSaveError) { Text("Close") } },
        )
        MaterialSaveState.Idle, is MaterialSaveState.Saved -> Unit
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FormContent(viewModel: MaterialRequestFormViewModel) {
    val form = viewModel.form
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AppTextField(
            value = form.title,
            onValueChange = { v -> viewModel.update { it.copy(title = v) } },
            label = "What are you looking for?",
            placeholder = "e.g. Old teak wood for restoration",
            modifier = Modifier.fillMaxWidth(),
        )
        Text("Material", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MaterialType.entries.forEach { type ->
                CategoryChip(
                    label = type.displayName,
                    selected = form.materialType == type,
                    onClick = { viewModel.update { it.copy(materialType = type) } },
                )
            }
        }
        AppTextField(
            value = form.quantity,
            onValueChange = { v -> viewModel.update { it.copy(quantity = v) } },
            label = "Quantity",
            placeholder = "e.g. 20 kg, or 5 chairs",
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AppTextField(
                value = form.budgetMin,
                onValueChange = { v -> viewModel.update { it.copy(budgetMin = v.filter(Char::isDigit)) } },
                label = "Budget from (₹)",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
            AppTextField(
                value = form.budgetMax,
                onValueChange = { v -> viewModel.update { it.copy(budgetMax = v.filter(Char::isDigit)) } },
                label = "Budget up to (₹)",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
        }
        AppTextField(
            value = form.description,
            onValueChange = { v -> viewModel.update { it.copy(description = v) } },
            label = "Description",
            singleLine = false,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "Visible to people in ${viewModel.city}.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        viewModel.formError?.let { InlineErrorBanner(message = it) }
        PrimaryButton(text = if (viewModel.isEditing) "Save changes" else "Post request", onClick = viewModel::save, modifier = Modifier.fillMaxWidth())
    }
}
