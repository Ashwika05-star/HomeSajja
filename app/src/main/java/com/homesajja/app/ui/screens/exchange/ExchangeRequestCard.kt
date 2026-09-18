package com.homesajja.app.ui.screens.exchange

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.homesajja.app.data.model.ExchangeRequest
import com.homesajja.app.ui.components.ItemThumbnail
import com.homesajja.app.ui.components.StatusBadge

/** One request in the Incoming / Outgoing lists, showing both items being swapped. */
@Composable
fun ExchangeRequestCard(
    request: ExchangeRequest,
    isIncoming: Boolean,
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
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SwapSide(request.offeredTitle, request.offeredImageUrl, Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Filled.SwapHoriz,
                    contentDescription = "swapped for",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp).size(24.dp),
                )
                SwapSide(request.requestedTitle, request.requestedImageUrl, Modifier.weight(1f))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isIncoming) "From ${request.senderName}" else "To ${request.receiverName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                StatusBadge(status = request.status.displayName)
            }
        }
    }
}

@Composable
private fun SwapSide(title: String, imageUrl: String?, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ItemThumbnail(
            imageUrl = imageUrl,
            description = title,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
        )
        Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
