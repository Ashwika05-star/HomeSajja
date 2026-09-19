package com.homesajja.app.data.model

/** Also used to describe what a listing is made of. */
enum class MaterialType(val displayName: String) {
    WOOD("Wood"),
    METAL("Metal"),
    FABRIC("Fabric"),
    LEATHER("Leather"),
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

/** Stored at `materialRequests/{id}`. A public board post by a vendor: any signed-in user can
 * read it and offer one of their own listings against it (see [MaterialOffer]). Only the
 * owning vendor can edit it. */
data class MaterialRequest(
    val id: String = "",
    val vendorId: String = "",
    val vendorName: String = "",
    val materialType: MaterialType = MaterialType.OTHER,
    val title: String = "",
    val description: String = "",
    val quantity: String = "",
    /** Budget range in rupees; either end may be left open. */
    val budgetMin: Long? = null,
    val budgetMax: Long? = null,
    val city: String = "",
    val status: MaterialRequestStatus = MaterialRequestStatus.OPEN,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

enum class OfferStatus(val displayName: String) {
    PENDING("Pending"),
    ACCEPTED("Accepted"),
    DECLINED("Declined"),
}

/** Stored at `materialRequests/{requestId}/offers/{offererId}`. The document id is the offerer's uid, so
 * a person can have at most one offer on a request. The listing's title and photo are copied in so
 * the vendor can read the offer without loading the listing. */
data class MaterialOffer(
    val id: String = "",
    val requestId: String = "",
    val offererId: String = "",
    val offererName: String = "",
    val listingId: String = "",
    val listingTitle: String = "",
    val listingImage: String? = null,
    val status: OfferStatus = OfferStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
