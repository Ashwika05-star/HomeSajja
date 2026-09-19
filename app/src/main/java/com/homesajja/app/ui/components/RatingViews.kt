package com.homesajja.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.homesajja.app.data.model.RatingSummary
import com.homesajja.app.data.model.Review
import com.homesajja.app.ui.util.formatTimeAgo

/** Five stars, [rating] of them filled. With [onRate] the stars can be tapped to choose a rating. */
@Composable
fun StarRow(rating: Int, modifier: Modifier = Modifier, starSize: Dp = 18.dp, onRate: ((Int) -> Unit)? = null) {
    Row(modifier = modifier.semantics { contentDescription = "$rating out of 5 stars" }) {
        (1..5).forEach { star ->
            Icon(
                imageVector = if (star <= rating) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(starSize).then(if (onRate != null) Modifier.clickable { onRate(star) } else Modifier),
            )
        }
    }
}

/** "★ 4.5 (12 reviews)" or "☆ No ratings yet". */
@Composable
fun RatingLine(summary: RatingSummary?, modifier: Modifier = Modifier) {
    Text(
        text = ratingText(summary),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

fun ratingText(summary: RatingSummary?): String =
    if (summary == null || !summary.hasRatings) "☆ No ratings yet"
    else "★ %.1f (%d review%s)".format(summary.average, summary.count, if (summary.count == 1) "" else "s")

/** The reviews section of a profile: the average and count, then each review, or a friendly note if there are none. */
@Composable
fun ReviewsSection(reviews: List<Review>, modifier: Modifier = Modifier) {
    val summary = RatingSummary.of(reviews)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Reviews", style = MaterialTheme.typography.titleMedium)
        if (!summary.hasRatings) {
            Text("No reviews yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("%.1f".format(summary.average), style = MaterialTheme.typography.headlineSmall)
                StarRow(rating = Math.round(summary.average).toInt(), starSize = 20.dp)
                Text("${summary.count} review${if (summary.count == 1) "" else "s"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            reviews.forEach { review -> ReviewItem(review) }
        }
    }
}

@Composable
private fun ReviewItem(review: Review) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StarRow(rating = review.rating, starSize = 14.dp)
            Text(review.reviewerName.ifBlank { "Someone" }, style = MaterialTheme.typography.labelLarge)
            Text(formatTimeAgo(review.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (review.comment.isNotBlank()) Text(review.comment, style = MaterialTheme.typography.bodyMedium)
    }
}
