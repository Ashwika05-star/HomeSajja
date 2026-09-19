package com.homesajja.app.ui.components

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.homesajja.app.ui.util.ImageWidth
import com.homesajja.app.ui.util.optimizedImage
import com.homesajja.app.viewmodel.MAX_PHOTOS
import com.homesajja.app.viewmodel.SellPhoto

/** Heading (and optional hint) at the top of a step in a multi-step flow. */
@Composable
fun StepTitle(title: String, hint: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        if (hint != null) {
            Text(hint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Thumbnails of the chosen photos, each removable, plus an "Add" tile that opens the gallery. Shared by the sell and repair flows. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PhotoPicker(
    photos: List<SellPhoto>,
    onAdd: (List<Uri>) -> Unit,
    onRemove: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(MAX_PHOTOS)) { uris ->
        if (uris.isNotEmpty()) onAdd(uris)
    }

    FlowRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        photos.forEachIndexed { index, photo ->
            Box(modifier = Modifier.size(104.dp)) {
                AsyncImage(
                    model = when (photo) {
                        is SellPhoto.Remote -> optimizedImage(photo.url, ImageWidth.THUMBNAIL)
                        is SellPhoto.Local -> photo.uri
                    },
                    contentDescription = "Photo ${index + 1}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(104.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), RoundedCornerShape(12.dp)),
                )
                // Standard 48dp touch target, with a smaller round backdrop drawn inside it.
                IconButton(onClick = { onRemove(index) }, modifier = Modifier.align(Alignment.TopEnd)) {
                    Box(
                        modifier = Modifier.size(28.dp).background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove photo ${index + 1}", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        if (photos.size < MAX_PHOTOS) {
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button, onClickLabel = "Add photos") {
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

