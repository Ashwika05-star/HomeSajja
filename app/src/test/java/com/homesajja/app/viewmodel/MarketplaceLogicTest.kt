package com.homesajja.app.viewmodel

import com.homesajja.app.data.model.Cities
import com.homesajja.app.data.model.FurnitureCategory
import com.homesajja.app.data.model.FurnitureCondition
import com.homesajja.app.data.model.MaterialType
import com.homesajja.app.data.model.PurchaseStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MarketplaceLogicTest {

    private val validForm = SellForm(
        category = FurnitureCategory.SOFA,
        photos = listOf(SellPhoto.Remote("https://example.com/a.jpg")),
        title = "Teak sofa",
        description = "Three seater in good shape",
        material = MaterialType.WOOD,
        ageYears = "3",
        condition = FurnitureCondition.GOOD,
        price = "8000",
        city = Cities.ALL.first(),
    )

    @Test
    fun completeForm_passesEveryStep() {
        SellStep.entries.forEach { assertNull("step $it", validForm.validate(it)) }
    }

    @Test
    fun eachStep_rejectsMissingInput() {
        assertNotNull(validForm.copy(category = null).validate(SellStep.CATEGORY))
        assertNotNull(validForm.copy(photos = emptyList()).validate(SellStep.PHOTOS))
        assertNotNull(validForm.copy(title = "ab").validate(SellStep.DETAILS))
        assertNotNull(validForm.copy(material = null).validate(SellStep.DETAILS))
        assertNotNull(validForm.copy(ageYears = "abc").validate(SellStep.DETAILS))
        assertNotNull(validForm.copy(condition = null).validate(SellStep.CONDITION))
        assertNotNull(validForm.copy(price = "0").validate(SellStep.PRICE))
        assertNotNull(validForm.copy(city = "Atlantis").validate(SellStep.LOCATION))
    }

    @Test
    fun photos_areCappedAtFive() {
        val six = List(6) { SellPhoto.Remote("u$it") }
        assertNotNull(validForm.copy(photos = six).validate(SellStep.PHOTOS))
        assertNull(validForm.copy(photos = six.take(5)).validate(SellStep.PHOTOS))
    }

    @Test
    fun dimensions_areOptionalButMustBePositiveWhenGiven() {
        assertNull(validForm.copy(lengthCm = "", widthCm = "", heightCm = "").validate(SellStep.DETAILS))
        assertNull(validForm.copy(lengthCm = "180", widthCm = "80", heightCm = "75").validate(SellStep.DETAILS))
        assertNotNull(validForm.copy(lengthCm = "-5").validate(SellStep.DETAILS))
        assertNotNull(validForm.copy(widthCm = "wide").validate(SellStep.DETAILS))
    }

    @Test
    fun offer_mustBePositiveAndNotAboveAskingPrice() {
        assertNull(validateOffer("7000", 8000))
        assertNull(validateOffer("8000", 8000))
        assertNotNull(validateOffer("8001", 8000))
        assertNotNull(validateOffer("0", 8000))
        assertNotNull(validateOffer("abc", 8000))
    }

    @Test
    fun sellerActions_followThePipeline() {
        assertEquals(listOf(SellerAction.ACCEPT, SellerAction.REJECT), sellerActionsFor(PurchaseStatus.REQUESTED))
        assertEquals(listOf(SellerAction.MARK_READY, SellerAction.CANCEL), sellerActionsFor(PurchaseStatus.ACCEPTED))
        assertEquals(listOf(SellerAction.COMPLETE, SellerAction.CANCEL), sellerActionsFor(PurchaseStatus.READY_FOR_PICKUP))
        PurchaseStatus.entries
            .filter { it in setOf(PurchaseStatus.COMPLETED, PurchaseStatus.REJECTED, PurchaseStatus.CANCELLED) }
            .forEach { assertEquals(emptyList<SellerAction>(), sellerActionsFor(it)) }
    }
}
