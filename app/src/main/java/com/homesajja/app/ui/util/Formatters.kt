package com.homesajja.app.ui.util

import com.homesajja.app.data.model.FurnitureDimensions
import com.homesajja.app.data.model.FurnitureListing
import com.homesajja.app.data.model.ListingActionType
import java.text.NumberFormat
import java.util.Locale

private val indianNumbers: NumberFormat = NumberFormat.getNumberInstance(Locale("en", "IN"))

/** Whole rupees with Indian digit grouping, e.g. 125000 -> "₹1,25,000". */
fun formatPrice(price: Long): String = "₹" + indianNumbers.format(price)

/** The price line shown on cards and headers: the asking price, or "For exchange" for exchange items. */
fun FurnitureListing.priceLabel(): String =
    if (actionType == ListingActionType.EXCHANGE) "For exchange" else formatPrice(price)

fun formatAge(years: Int): String = when (years) {
    0 -> "Less than a year"
    1 -> "1 year"
    else -> "$years years"
}

/** e.g. "180 × 80 × 75 cm"; partly filled dimensions read "L 180 · H 75 cm". Null if none given. */
fun FurnitureDimensions.displayText(): String? {
    val all = listOf(lengthCm, widthCm, heightCm)
    if (all.all { it == null }) return null
    if (all.none { it == null }) return all.joinToString(" × ") + " cm"
    return listOf("L" to lengthCm, "W" to widthCm, "H" to heightCm)
        .filter { it.second != null }
        .joinToString(" · ") { "${it.first} ${it.second}" } + " cm"
}

/** A short "how long ago" label for activity feeds, e.g. "just now", "5 min ago", "3 h ago", "2 d ago". */
fun formatTimeAgo(timestamp: Long, now: Long = System.currentTimeMillis()): String {
    val minutes = ((now - timestamp) / 60_000).coerceAtLeast(0)
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes min ago"
        minutes < 24 * 60 -> "${minutes / 60} h ago"
        else -> "${minutes / (24 * 60)} d ago"
    }
}

/** "₹500 – ₹2,000", "From ₹500", "Up to ₹2,000", or null when no budget was given. */
fun budgetLabel(min: Long?, max: Long?): String? = when {
    min != null && max != null -> "${formatPrice(min)} – ${formatPrice(max)}"
    min != null -> "From ${formatPrice(min)}"
    max != null -> "Up to ${formatPrice(max)}"
    else -> null
}

private val clockFormat = java.text.SimpleDateFormat("h:mm a", Locale.getDefault())
private val dayFormat = java.text.SimpleDateFormat("d MMM, h:mm a", Locale.getDefault())

/** "3:45 PM" for today's messages, "12 Sep, 3:45 PM" for older ones. */
fun formatMessageTime(timestamp: Long, now: Long = System.currentTimeMillis()): String {
    val sameDay = (timestamp / 86_400_000L) == (now / 86_400_000L)
    val date = java.util.Date(timestamp)
    return if (sameDay) clockFormat.format(date) else dayFormat.format(date)
}
