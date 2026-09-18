package com.homesajja.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ListingFiltersTest {

    private val sofa = FurnitureListing(
        title = "Teak Sofa",
        description = "Three seater",
        price = 8_000,
        condition = FurnitureCondition.GOOD,
        material = MaterialType.WOOD,
        category = FurnitureCategory.SOFA,
    )

    @Test
    fun emptyFilters_matchEverything() {
        assertFalse(ListingFilters().isActive)
        assertTrue(ListingFilters().matches(sofa))
    }

    @Test
    fun priceRange_isInclusive() {
        assertTrue(ListingFilters(minPrice = 8_000, maxPrice = 8_000).matches(sofa))
        assertFalse(ListingFilters(minPrice = 8_001).matches(sofa))
        assertFalse(ListingFilters(maxPrice = 7_999).matches(sofa))
    }

    @Test
    fun conditionAndMaterial_acceptAnyOfTheChosen() {
        val filters = ListingFilters(
            conditions = setOf(FurnitureCondition.NEW, FurnitureCondition.GOOD),
            materials = setOf(MaterialType.WOOD, MaterialType.METAL),
        )
        assertTrue(filters.matches(sofa))
        assertFalse(filters.copy(materials = setOf(MaterialType.GLASS)).matches(sofa))
    }

    @Test
    fun itemState_isDerivedFromConditionAndRefurbishedFlag() {
        assertEquals(ItemState.USED, sofa.itemState())
        assertEquals(ItemState.NEW, sofa.copy(condition = FurnitureCondition.NEW).itemState())
        // Refurbished wins even if the condition is also "new".
        assertEquals(
            ItemState.REFURBISHED,
            sofa.copy(condition = FurnitureCondition.NEW, refurbished = true).itemState(),
        )
        assertFalse(ListingFilters(itemState = ItemState.REFURBISHED).matches(sofa))
    }

    @Test
    fun search_matchesTitleDescriptionAndCategory_ignoringCase() {
        assertTrue(sofa.matchesSearch(""))
        assertTrue(sofa.matchesSearch("  teak "))
        assertTrue(sofa.matchesSearch("SEATER"))
        assertTrue(sofa.matchesSearch("sofa"))
        assertFalse(sofa.matchesSearch("wardrobe"))
    }
}
