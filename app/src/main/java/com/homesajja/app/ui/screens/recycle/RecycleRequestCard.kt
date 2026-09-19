package com.homesajja.app.ui.screens.recycle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.homesajja.app.data.model.RecycleMethod
import com.homesajja.app.data.model.RecyclingRequest
import com.homesajja.app.ui.components.ItemThumbnail
import com.homesajja.app.ui.components.StatusBadge

/** One recycling request in a list: material, condition, how it's handed over, and the current status badge. */
@Composable
fun RecycleRequestCard(
    request: RecyclingRequest,
    viewerIsRecycler: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ItemThumbnail(
                imageUrl = request.images.firstOrNull(),
                description = "${request.material.displayName} furniture",
                modifier = Modifier.width(104.dp).clip(RoundedCornerShape(12.dp)),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "${request.material.displayName} furniture",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${request.condition.displayName} · ${request.method.displayName}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = when {
                        viewerIsRecycler -> "From ${request.userName}"
                        request.method == RecycleMethod.DROP_OFF -> "To ${request.vendorName}"
                        else -> "Waiting for a recycler"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(status = request.status.displayName)
                }
            }
        }
    }
}
