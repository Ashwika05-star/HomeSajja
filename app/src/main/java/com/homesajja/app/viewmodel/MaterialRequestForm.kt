package com.homesajja.app.viewmodel

import com.homesajja.app.data.model.MaterialType

const val MIN_MATERIAL_TITLE = 3
const val MIN_MATERIAL_DESCRIPTION = 10

/** What a vendor types when posting a material request. Budget stays as text until [validate] parses it. */
data class MaterialRequestForm(
    val title: String = "",
    val materialType: MaterialType? = null,
    val quantity: String = "",
    val budgetMin: String = "",
    val budgetMax: String = "",
    val description: String = "",
) {
    val parsedBudgetMin: Long? get() = budgetMin.trim().toLongOrNull()
    val parsedBudgetMax: Long? get() = budgetMax.trim().toLongOrNull()

    /** Returns the first problem with the form, or null if it can be posted. */
    fun validate(): String? {
        val min = parsedBudgetMin
        val max = parsedBudgetMax
        return when {
            title.trim().length < MIN_MATERIAL_TITLE -> "Give the request a short title."
            materialType == null -> "Pick the material you're looking for."
            quantity.isBlank() -> "Say how much you need, e.g. \"20 kg\" or \"5 chairs\"."
            budgetMin.isNotBlank() && min == null -> "The minimum budget must be a whole number."
            budgetMax.isNotBlank() && max == null -> "The maximum budget must be a whole number."
            min != null && max != null && min > max -> "The minimum budget can't be above the maximum."
            description.trim().length < MIN_MATERIAL_DESCRIPTION -> "Describe what you need in at least $MIN_MATERIAL_DESCRIPTION characters."
            else -> null
        }
    }
}
