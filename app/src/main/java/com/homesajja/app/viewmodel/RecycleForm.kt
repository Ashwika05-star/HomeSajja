package com.homesajja.app.viewmodel

import com.homesajja.app.data.model.RecycleCondition
import com.homesajja.app.data.model.RecycleMaterial
import com.homesajja.app.data.model.RecycleMethod
import com.homesajja.app.data.model.VendorProfile

/** The steps of the recycle flow, in order. DESTINATION shows the city (pickup) or the recyclers (drop-off). */
enum class RecycleStep(val title: String) {
    PHOTOS("Photos"),
    CONDITION("Condition"),
    MATERIAL("Material"),
    METHOD("Pickup or drop-off"),
    DESTINATION("Where"),
    CONFIRM("Confirm"),
}

/** Everything chosen in the recycle flow. [recycler] is only used for a drop-off. */
data class RecycleForm(
    val photos: List<SellPhoto> = emptyList(),
    val condition: RecycleCondition? = null,
    val material: RecycleMaterial? = null,
    val method: RecycleMethod? = null,
    val city: String = "",
    val recycler: VendorProfile? = null,
) {
    /** Returns the first problem with [step], or null if it can be left. */
    fun validate(step: RecycleStep): String? = when (step) {
        RecycleStep.PHOTOS -> if (photos.isEmpty()) "Add at least one photo of the furniture." else null
        RecycleStep.CONDITION -> if (condition == null) "Pick the condition that fits best." else null
        RecycleStep.MATERIAL -> if (material == null) "Pick what it's mostly made of." else null
        RecycleStep.METHOD -> if (method == null) "Choose pickup or drop-off." else null
        RecycleStep.DESTINATION -> when {
            city.isBlank() -> "Your city isn't set, so we can't arrange this near you."
            method == RecycleMethod.DROP_OFF && recycler == null -> "Choose a recycler to drop it off at."
            else -> null
        }
        RecycleStep.CONFIRM -> null
    }
}
