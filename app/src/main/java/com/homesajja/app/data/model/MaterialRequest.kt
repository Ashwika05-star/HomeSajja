package com.homesajja.app.data.model

enum class MaterialType(val displayName: String) {
    WOOD("Wood"),
    METAL("Metal"),
    FABRIC("Fabric"),
    GLASS("Glass"),
    PLASTIC("Plastic"),
    OTHER("Other"),
}

/** OPEN -> FULFILLED, or CLOSED if the vendor withdraws it. */
enum class MaterialRequestStatus(val displayName: String) {
    OPEN("Open"),
    FULFILLED("Fulfilled"),
    CLOSED("Closed"),
}

/** Stored at `materialRequests/{id}`. A public board post by a vendor: any
 * signed-in user can read it, and responders reach the vendor through a Chat
 * whose context is this request. Only the owning vendor can edit it. */
data class MaterialRequest(
    val id: String = "",
    val vendorId: String = "",
    val vendorName: String = "",
    val materialType: MaterialType = MaterialType.OTHER,
    val title: String = "",
    val description: String = "",
    val quantity: String = "",
    val city: String = "",
    val status: MaterialRequestStatus = MaterialRequestStatus.OPEN,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
