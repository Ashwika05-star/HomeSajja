package com.homesajja.app.ui.screens.exchange

import com.homesajja.app.data.model.ExchangeRequest
import com.homesajja.app.data.model.FurnitureListing
import com.homesajja.app.ui.components.ComparisonItem
import com.homesajja.app.ui.util.formatAge
import com.homesajja.app.ui.util.priceLabel

/** Full details of a listing for a comparison card. */
internal fun FurnitureListing.toComparisonItem() = ComparisonItem(
    title = title,
    imageUrl = images.firstOrNull(),
    priceLabel = priceLabel(),
    facts = listOf(
        "Condition" to (condition.displayName + if (refurbished) " · Refurbished" else ""),
        "Category" to category.displayName,
        "Material" to material.displayName,
        "Age" to formatAge(ageYears),
        "City" to city,
        "Owner" to ownerName,
    ),
    listingId = id,
)

/** Stand-in when a listing has been deleted: the request's own snapshot is all that's left. */
internal fun removedComparisonItem(title: String, imageUrl: String?) = ComparisonItem(
    title = title,
    imageUrl = imageUrl,
    priceLabel = null,
    facts = emptyList(),
    note = "This listing is no longer available.",
)

internal fun ExchangeRequest.offeredItem(listing: FurnitureListing?) =
    listing?.toComparisonItem() ?: removedComparisonItem(offeredTitle, offeredImageUrl)

internal fun ExchangeRequest.requestedItem(listing: FurnitureListing?) =
    listing?.toComparisonItem() ?: removedComparisonItem(requestedTitle, requestedImageUrl)
