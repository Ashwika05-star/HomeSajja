package com.homesajja.app.ui.util

import com.homesajja.app.data.model.FurnitureDimensions
import java.text.NumberFormat
import java.util.Locale

private val indianNumbers: NumberFormat = NumberFormat.getNumberInstance(Locale("en", "IN"))

/** Whole rupees with Indian digit grouping, e.g. 125000 -> "₹1,25,000". */
fun formatPrice(price: Long): String = "₹" + indianNumbers.format(price)

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
