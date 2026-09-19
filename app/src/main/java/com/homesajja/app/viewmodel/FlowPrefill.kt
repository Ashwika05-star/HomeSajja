package com.homesajja.app.viewmodel

import com.homesajja.app.data.model.FlowPrefill
import com.homesajja.app.data.model.ListingActionType
import com.homesajja.app.data.model.MaterialType
import com.homesajja.app.data.model.Recommendation
import com.homesajja.app.data.model.RecycleMaterial

/*
 * How a Smart Decision suggestion pre-fills the Sell, Repair and Recycle flows. Only what the AI (or the person, by
 * picking a photo) actually provided is filled in; everything stays editable, and an empty prefill changes nothing.
 */

private const val PRICE_ROUNDING = 100L

/** The middle of the suggested range, rounded to the nearest ₹100, as text for the price box; null if there is no range. */
fun suggestedPriceText(min: Long?, max: Long?): String? {
    if (min == null || max == null) return null
    val middle = ((min + max) / 2 + PRICE_ROUNDING / 2) / PRICE_ROUNDING * PRICE_ROUNDING
    return middle.takeIf { it > 0 }?.toString()
}

private fun RecycleMaterial.toListingMaterial(): MaterialType = when (this) {
    RecycleMaterial.WOOD -> MaterialType.WOOD
    RecycleMaterial.METAL -> MaterialType.METAL
    RecycleMaterial.FABRIC -> MaterialType.FABRIC
    RecycleMaterial.PLASTIC -> MaterialType.PLASTIC
    RecycleMaterial.MIXED, RecycleMaterial.OTHER -> MaterialType.OTHER
}

fun SellForm.withPrefill(prefill: FlowPrefill): SellForm {
    val assessment = prefill.assessment
    val forExchange = prefill.target == Recommendation.EXCHANGE
    return copy(
        category = assessment?.category ?: category,
        photos = prefill.photo?.let { listOf(SellPhoto.Local(it)) } ?: photos,
        material = assessment?.material?.toListingMaterial() ?: material,
        actionType = if (forExchange) ListingActionType.EXCHANGE else ListingActionType.SELL,
        price = if (forExchange) price else suggestedPriceText(assessment?.priceMin, assessment?.priceMax) ?: price,
    )
}

fun RepairForm.withPrefill(prefill: FlowPrefill): RepairForm {
    val assessment = prefill.assessment
    return copy(
        listing = null,
        notListed = true,
        category = assessment?.category ?: category,
        photos = prefill.photo?.let { listOf(SellPhoto.Local(it)) } ?: photos,
        problem = assessment?.problemType ?: problem,
    )
}

fun RecycleForm.withPrefill(prefill: FlowPrefill): RecycleForm {
    val assessment = prefill.assessment
    return copy(
        photos = prefill.photo?.let { listOf(SellPhoto.Local(it)) } ?: photos,
        condition = assessment?.recycleCondition ?: condition,
        material = assessment?.material ?: material,
    )
}
