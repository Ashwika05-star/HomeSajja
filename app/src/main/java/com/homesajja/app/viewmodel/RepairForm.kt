package com.homesajja.app.viewmodel

import com.homesajja.app.data.model.FurnitureCategory
import com.homesajja.app.data.model.FurnitureListing
import com.homesajja.app.data.model.RepairProblemType
import com.homesajja.app.data.model.VendorProfile

const val MIN_REPAIR_DESCRIPTION = 10

/** The steps of the repair request flow, in order. */
enum class RepairStep(val title: String) {
    FURNITURE("Furniture"),
    PHOTOS("Photos"),
    PROBLEM("Problem"),
    DESCRIPTION("Description"),
    LOCATION("Location"),
    PROVIDER("Provider"),
}

/** Everything chosen in the repair flow. The furniture is either one of the user's own
 * [listing]s or a freeform "not listed" item ([notListed], with a [category] and optional [name]). */
data class RepairForm(
    val listing: FurnitureListing? = null,
    val notListed: Boolean = false,
    val category: FurnitureCategory? = null,
    val name: String = "",
    val photos: List<SellPhoto> = emptyList(),
    val problem: RepairProblemType? = null,
    val description: String = "",
    val city: String = "",
    val provider: VendorProfile? = null,
) {
    /** What to call the furniture on cards and in the request. */
    val furnitureTitle: String
        get() = listing?.title ?: name.trim().ifEmpty { category?.displayName ?: "Furniture" }

    val furnitureCategory: FurnitureCategory
        get() = listing?.category ?: category ?: FurnitureCategory.OTHER

    /** Returns the first problem with [step], or null if it can be left. */
    fun validate(step: RepairStep): String? = when (step) {
        RepairStep.FURNITURE -> when {
            listing == null && !notListed -> "Pick one of your items, or choose \"Furniture not listed\"."
            notListed && category == null -> "Pick a category for your furniture."
            else -> null
        }
        RepairStep.PHOTOS -> if (photos.isEmpty()) "Add at least one photo of the damage." else null
        RepairStep.PROBLEM -> if (problem == null) "Pick the problem that fits best." else null
        RepairStep.DESCRIPTION ->
            if (description.trim().length < MIN_REPAIR_DESCRIPTION) "Describe the problem in at least $MIN_REPAIR_DESCRIPTION characters." else null
        RepairStep.LOCATION -> if (city.isBlank()) "Your city isn't set, so we can't find repairers near you." else null
        RepairStep.PROVIDER -> if (provider == null) "Choose a repair provider to send your request to." else null
    }
}
