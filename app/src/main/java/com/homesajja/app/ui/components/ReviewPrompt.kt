package com.homesajja.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.homesajja.app.di.LocalAppContainer
import com.homesajja.app.di.ViewModelFactory
import com.homesajja.app.viewmodel.MAX_REVIEW_COMMENT
import com.homesajja.app.viewmodel.ReviewParams
import com.homesajja.app.viewmodel.ReviewParamsKey
import com.homesajja.app.viewmodel.ReviewUiState
import com.homesajja.app.viewmodel.ReviewViewModel

/**
 * The "Leave a review" card for one completed transaction. Show it only once the transaction is COMPLETED and the
 * person is the one who may review; it then offers a review, or shows (and lets them edit) the one they wrote.
 */
@Composable
fun ReviewPrompt(params: ReviewParams, modifier: Modifier = Modifier) {
    val owner = checkNotNull(LocalViewModelStoreOwner.current) { "No ViewModelStoreOwner" }
    val extras: CreationExtras = MutableCreationExtras((owner as HasDefaultViewModelProviderFactory).defaultViewModelCreationExtras).apply {
        set(ReviewParamsKey, params)
    }
    val viewModel: ReviewViewModel = viewModel(
        viewModelStoreOwner = owner,
        key = "review_${params.contextType}_${params.contextId}",
        factory = ViewModelFactory(LocalAppContainer.current),
        extras = extras,
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when (val state = viewModel.uiState) {
                ReviewUiState.Loading -> CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                is ReviewUiState.Error -> {
                    Text(state.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = viewModel::load) { Text("Try again") }
                }
                is ReviewUiState.Ready -> {
                    val review = state.review
                    if (review == null) {
                        Text("How was your experience with ${viewModel.targetName}?", style = MaterialTheme.typography.titleSmall)
                        PrimaryButton(text = "Leave a review", onClick = viewModel::openDialog, modifier = Modifier.fillMaxWidth())
                    } else {
                        Text("Your review of ${viewModel.targetName}", style = MaterialTheme.typography.titleSmall)
                        StarRow(rating = review.rating, starSize = 22.dp)
                        if (review.comment.isNotBlank()) Text(review.comment, style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = viewModel::openDialog) { Text("Edit review") }
                    }
                }
            }
        }
    }

    if (viewModel.dialogOpen) {
        AlertDialog(
            onDismissRequest = viewModel::closeDialog,
            title = { Text("Review ${viewModel.targetName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    StarRow(rating = viewModel.rating, starSize = 36.dp, onRate = viewModel::chooseRating)
                    AppTextField(
                        value = viewModel.comment,
                        onValueChange = viewModel::changeComment,
                        label = "Comment (optional)",
                        singleLine = false,
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text("${viewModel.comment.length}/$MAX_REVIEW_COMMENT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    viewModel.dialogError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (viewModel.saving) CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                    TextButton(onClick = viewModel::submit, enabled = !viewModel.saving) { Text("Submit") }
                }
            },
            dismissButton = { TextButton(onClick = viewModel::closeDialog, enabled = !viewModel.saving) { Text("Cancel") } },
        )
    }
}
