package com.homesajja.app.data.model

/** REQUESTED -> ACCEPTED -> IN_PROGRESS -> READY -> COMPLETED.
 * The vendor moves it forward (or REJECTs it); the user can CANCEL until work starts. */
enum class RepairStatus(val displayName: String) {
    REQUESTED("Requested"),
    ACCEPTED("Accepted"),
    IN_PROGRESS("In progress"),
    READY("Ready"),
    COMPLETED("Completed"),
    REJECTED("Rejected"),
    CANCELLED("Cancelled"),
}

/** What is wrong with the furniture. [displayName] is shown in the UI; the enum name is stored. */
enum class RepairProblemType(val displayName: String) {
    BROKEN_LEG("Broken leg"),
    DAMAGED_WOOD("Damaged wood"),
    LOOSE_JOINTS("Loose joints"),
    FABRIC_DAMAGE("Fabric damage"),
    SCRATCHES("Scratches"),
    POLISHING("Polishing"),
    STRUCTURAL_DAMAGE("Structural damage"),
    OTHER("Other"),
}

/** Stored at `repairRequests/{id}`, addressed to one chosen vendor.
 * Parties: [userId] and [vendorId]. [listingId] is set when the furniture came from one of the
 * user's listings; otherwise the request stands alone with its own [furnitureTitle] and [images]. */
data class RepairRequest(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val vendorId: String = "",
    val vendorName: String = "",
    val listingId: String? = null,
    val furnitureTitle: String = "",
    val furnitureCategory: FurnitureCategory = FurnitureCategory.OTHER,
    val problemType: RepairProblemType = RepairProblemType.OTHER,
    val issueDescription: String = "",
    val images: List<String> = emptyList(),
    val city: String = "",
    val status: RepairStatus = RepairStatus.REQUESTED,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
