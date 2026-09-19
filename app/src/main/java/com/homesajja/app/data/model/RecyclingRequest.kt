package com.homesajja.app.data.model

/** REQUESTED -> ACCEPTED -> SCHEDULED -> COMPLETED.
 * The recycler moves it forward (or REJECTs it); the user can CANCEL until it is scheduled. */
enum class RecyclingStatus(val displayName: String) {
    REQUESTED("Requested"),
    ACCEPTED("Accepted"),
    SCHEDULED("Scheduled"),
    COMPLETED("Completed"),
    REJECTED("Rejected"),
    CANCELLED("Cancelled"),
}

/** What state the furniture is in, which tells the recycler what can be done with it. */
enum class RecycleCondition(val displayName: String, val hint: String) {
    REPAIRABLE("Repairable", "Damaged, but could be fixed"),
    REUSABLE("Reusable", "Works as it is, or with parts reused"),
    BEYOND_REPAIR("Beyond repair", "Only the raw material is worth recovering"),
}

/** What the furniture is mostly made of. Deliberately separate from the marketplace's [MaterialType]. */
enum class RecycleMaterial(val displayName: String) {
    WOOD("Wood"),
    METAL("Metal"),
    FABRIC("Fabric"),
    PLASTIC("Plastic"),
    MIXED("Mixed"),
    OTHER("Other"),
}

/** How the furniture gets to the recycler. */
enum class RecycleMethod(val displayName: String) {
    PICKUP("Pickup"),
    DROP_OFF("Drop-off"),
}

/** Stored at `recyclingRequests/{id}`. A DROP_OFF request is addressed to the recycler the user chose;
 * a PICKUP request has no vendor ([vendorId] null) until a recycler claims it from the vendor dashboard. */
data class RecyclingRequest(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val vendorId: String? = null,
    val vendorName: String? = null,
    val condition: RecycleCondition = RecycleCondition.REUSABLE,
    val material: RecycleMaterial = RecycleMaterial.OTHER,
    val method: RecycleMethod = RecycleMethod.PICKUP,
    val images: List<String> = emptyList(),
    val city: String = "",
    val status: RecyclingStatus = RecyclingStatus.REQUESTED,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
