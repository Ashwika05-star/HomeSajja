package com.homesajja.app.viewmodel

import android.net.Uri
import com.homesajja.app.data.model.Cities
import com.homesajja.app.data.model.FurnitureCategory
import com.homesajja.app.data.model.FurnitureCondition
import com.homesajja.app.data.model.MaterialType

const val MAX_PHOTOS = 5

/** The steps of the sell flow, in order. */
enum class SellStep(val title: String) {
    CATEGORY("Category"),
    PHOTOS("Photos"),
    DETAILS("Details"),
    CONDITION("Condition"),
    PRICE("Price"),
    LOCATION("Location"),
    PREVIEW("Preview"),
}

/** A photo in the form: already uploaded (editing) or freshly picked from the gallery. */
sealed interface SellPhoto {
    data class Remote(val url: String) : SellPhoto
    data class Local(val uri: Uri) : SellPhoto
}

/** Everything typed or picked in the sell flow. Numeric fields stay as text so the
 * user's half-typed input is never rewritten; [validate] checks them per step. */
data class SellForm(
    val category: FurnitureCategory? = null,
    val photos: List<SellPhoto> = emptyList(),
    val title: String = "",
    val description: String = "",
    val material: MaterialType? = null,
    val ageYears: String = "",
    val lengthCm: String = "",
    val widthCm: String = "",
    val heightCm: String = "",
    val condition: FurnitureCondition? = null,
    val refurbished: Boolean = false,
    val price: String = "",
    val city: String = "",
) {
    /** Returns the first problem with [step], or null if it can be left. */
    fun validate(step: SellStep): String? = when (step) {
        SellStep.CATEGORY -> if (category == null) "Pick a category to continue." else null
        SellStep.PHOTOS -> when {
            photos.isEmpty() -> "Add at least one photo."
            photos.size > MAX_PHOTOS -> "You can add up to $MAX_PHOTOS photos."
            else -> null
        }
        SellStep.DETAILS -> validateDetails()
        SellStep.CONDITION -> if (condition == null) "Select the item's condition." else null
        SellStep.PRICE -> {
            val value = price.trim().toLongOrNull()
            if (value == null || value <= 0) "Enter a price greater than zero." else null
        }
        SellStep.LOCATION -> if (city !in Cities.ALL) "Select a city." else null
        SellStep.PREVIEW -> null
    }

    private fun validateDetails(): String? {
        val age = ageYears.trim().toIntOrNull()
        return when {
            title.trim().length < 3 -> "Enter a title (at least 3 characters)."
            description.isBlank() -> "Add a short description."
            material == null -> "Select the main material."
            age == null || age < 0 || age > 100 -> "Enter the item's age in years (0 if under a year)."
            !isOptionalPositive(lengthCm) || !isOptionalPositive(widthCm) || !isOptionalPositive(heightCm) ->
                "Dimensions must be whole numbers greater than zero, or left blank."
            else -> null
        }
    }

    private fun isOptionalPositive(text: String): Boolean =
        text.isBlank() || (text.trim().toIntOrNull()?.let { it > 0 } ?: false)
}
