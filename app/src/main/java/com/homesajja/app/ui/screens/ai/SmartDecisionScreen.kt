package com.homesajja.app.ui.screens.ai

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.homesajja.app.data.model.FurnitureAssessment
import com.homesajja.app.data.model.Recommendation
import com.homesajja.app.di.LocalAppContainer
import com.homesajja.app.di.ViewModelFactory
import com.homesajja.app.ui.components.AppTextField
import com.homesajja.app.ui.components.AppTopBar
import com.homesajja.app.ui.components.OutlinedButton
import com.homesajja.app.ui.components.PrimaryButton
import com.homesajja.app.ui.util.budgetLabel
import com.homesajja.app.viewmodel.SmartDecisionPhase
import com.homesajja.app.viewmodel.SmartDecisionViewModel

/** "Not sure what to do? Ask HomeSajja": a photo and a few notes in, a recommendation and a way into the right flow out. */
@Composable
fun SmartDecisionScreen(onBackClick: () -> Unit, onOpenFlow: (Recommendation) -> Unit) {
    val viewModel: SmartDecisionViewModel = viewModel(factory = ViewModelFactory(LocalAppContainer.current))
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(viewModel::pickPhoto) }

    LaunchedEffect(viewModel) {
        viewModel.openFlow.collect { onOpenFlow(it) }
    }

    Scaffold(
        topBar = { AppTopBar(title = "Ask HomeSajja", onBackClick = onBackClick) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Add a photo and tell us a little about your furniture. We'll suggest whether to sell, repair, exchange or recycle it. It's only a suggestion: you decide.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            PhotoBox(viewModel.photo, onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) })

            when (val phase = viewModel.phase) {
                SmartDecisionPhase.Input -> {
                    AppTextField(
                        value = viewModel.notes,
                        onValueChange = viewModel::onNotesChange,
                        label = "About it (optional)",
                        placeholder = "e.g. Wobbly leg, a few scratches, bought in 2019",
                        singleLine = false,
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    AppTextField(
                        value = viewModel.ageText,
                        onValueChange = viewModel::onAgeChange,
                        label = "Age in years (optional)",
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PrimaryButton(text = "Ask HomeSajja", onClick = viewModel::ask, enabled = viewModel.photo != null, modifier = Modifier.fillMaxWidth())
                    if (viewModel.photo == null) {
                        Text("Add a photo to get a suggestion.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                SmartDecisionPhase.Thinking -> Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    CircularProgressIndicator()
                    Text("Looking at your furniture…", style = MaterialTheme.typography.bodyLarge)
                }
                is SmartDecisionPhase.Suggestion -> {
                    SuggestionCard(phase.assessment, onContinue = { viewModel.proceed(phase.assessment.recommendation) })
                    OutlinedButton(text = "Ask again", onClick = viewModel::tryAgain, modifier = Modifier.fillMaxWidth())
                }
                is SmartDecisionPhase.Failed -> {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(phase.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                            Text("You can still choose what to do yourself below.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                    OutlinedButton(text = "Try again", onClick = viewModel::tryAgain, modifier = Modifier.fillMaxWidth())
                }
            }

            // Always available, so a suggestion (or a failed one) never blocks the person.
            Text(
                if (viewModel.phase is SmartDecisionPhase.Suggestion) "Or choose something else" else "Or decide yourself",
                style = MaterialTheme.typography.titleSmall,
            )
            Recommendation.entries.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { option ->
                        OutlinedButton(text = option.displayName, onClick = { viewModel.proceed(option) }, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoBox(photo: android.net.Uri?, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (photo == null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Add a photo", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        } else {
            AsyncImage(model = photo, contentDescription = "Your furniture", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun SuggestionCard(assessment: FurnitureAssessment, onContinue: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("We suggest", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            Text(assessment.recommendation.verb, style = MaterialTheme.typography.headlineSmall)
            Text(assessment.reasoning, style = MaterialTheme.typography.bodyLarge)
            budgetLabel(assessment.priceMin, assessment.priceMax)?.let {
                Text("Suggested price: $it", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            }
            PrimaryButton(text = "Continue to ${assessment.recommendation.displayName}", onClick = onContinue, modifier = Modifier.fillMaxWidth())
        }
    }
}
