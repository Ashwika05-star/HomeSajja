package com.homesajja.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.homesajja.app.ui.util.ImageWidth
import com.homesajja.app.ui.util.optimizedImage

/** What one side of a comparison shows. [note] flags problems such as a removed listing. */
data class ComparisonItem(
    val title: String,
    val imageUrl: String?,
    val priceLabel: String?,
    val facts: List<Pair<String, String>>,
    val note: String? = null,
    /** The listing behind this item, so it can be opened; null when it no longer exists. */
    val listingId: String? = null,
)

/** Two items side by side with a swap icon between them — used wherever an exchange is reviewed. */
@Composable
fun ItemComparison(
    left: ComparisonItem,
    right: ComparisonItem,
    leftLabel: String,
    rightLabel: String,
    modifier: Modifier = Modifier,
    onItemClick: ((listingId: String) -> Unit)? = null,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        ComparisonColumn(label = leftLabel, item = left, onItemClick = onItemClick, modifier = Modifier.weight(1f))
        Icon(
            imageVector = Icons.Filled.SwapHoriz,
            contentDescription = "swapped for",
            tint = MaterialTheme.colorScheme.primary,
            // Sits level with the middle of the photos rather than the middle of the (variable-height) cards.
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 84.dp).size(24.dp),
        )
        ComparisonColumn(label = rightLabel, item = right, onItemClick = onItemClick, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ComparisonColumn(
    label: String,
    item: ComparisonItem,
    onItemClick: ((String) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val listingId = item.listingId
    val shape = RoundedCornerShape(16.dp)
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    val elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    val content: @Composable () -> Unit = { ComparisonCardContent(item) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (onItemClick != null && listingId != null) {
            Card(onClick = { onItemClick(listingId) }, shape = shape, colors = colors, elevation = elevation, modifier = Modifier.fillMaxWidth()) {
                content()
            }
        } else {
            Card(shape = shape, colors = colors, elevation = elevation, modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
private fun ComparisonCardContent(item: ComparisonItem) {
    Column {
        ItemThumbnail(imageUrl = item.imageUrl, description = item.title, modifier = Modifier.fillMaxWidth())
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(item.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            item.priceLabel?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
            item.facts.forEach { (name, value) ->
                Column {
                    Text(name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(value, style = MaterialTheme.typography.bodySmall)
                }
            }
            item.note?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

/** A 4:3 photo with the furniture icon as a placeholder; rounded by its parent card or by [clip] callers. */
@Composable
fun ItemThumbnail(imageUrl: String?, description: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(4f / 3f)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl.isNullOrBlank()) {
            Icon(Icons.Filled.Chair, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        } else {
            AsyncImage(
                model = optimizedImage(imageUrl, ImageWidth.THUMBNAIL),
                contentDescription = description,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f),
            )
        }
    }
}
