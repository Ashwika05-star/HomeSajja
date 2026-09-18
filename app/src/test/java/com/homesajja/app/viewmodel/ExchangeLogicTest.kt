package com.homesajja.app.viewmodel

import com.homesajja.app.data.model.Cities
import com.homesajja.app.data.model.ExchangeRequest
import com.homesajja.app.data.model.ExchangeStatus
import com.homesajja.app.data.model.FurnitureCategory
import com.homesajja.app.data.model.FurnitureCondition
import com.homesajja.app.data.model.ListingActionType
import com.homesajja.app.data.model.MaterialType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ExchangeLogicTest {

    private fun request(status: ExchangeStatus) =
        ExchangeRequest(id = "r1", senderId = "sender", receiverId = "receiver", status = status)

    @Test
    fun pending_receiverAnswers_senderCanOnlyCancel() {
        val pending = request(ExchangeStatus.PENDING)
        assertEquals(listOf(ExchangeAction.ACCEPT, ExchangeAction.DECLINE), exchangeActionsFor(pending, "receiver"))
        assertEquals(listOf(ExchangeAction.CANCEL), exchangeActionsFor(pending, "sender"))
    }

    @Test
    fun accepted_eitherPartyCanComplete_noOneCanCancel() {
        val accepted = request(ExchangeStatus.ACCEPTED)
        assertEquals(listOf(ExchangeAction.COMPLETE), exchangeActionsFor(accepted, "sender"))
        assertEquals(listOf(ExchangeAction.COMPLETE), exchangeActionsFor(accepted, "receiver"))
    }

    @Test
    fun finishedRequests_offerNothing() {
        listOf(ExchangeStatus.DECLINED, ExchangeStatus.CANCELLED, ExchangeStatus.COMPLETED).forEach { status ->
            assertEquals(emptyList<ExchangeAction>(), exchangeActionsFor(request(status), "sender"))
            assertEquals(emptyList<ExchangeAction>(), exchangeActionsFor(request(status), "receiver"))
        }
    }

    @Test
    fun strangersAndLoggedOutUsers_getNoActions() {
        ExchangeStatus.entries.forEach { status ->
            assertEquals(emptyList<ExchangeAction>(), exchangeActionsFor(request(status), "someone-else"))
            assertEquals(emptyList<ExchangeAction>(), exchangeActionsFor(request(status), null))
        }
    }

    private val exchangeForm = SellForm(
        category = FurnitureCategory.CHAIR,
        photos = listOf(SellPhoto.Remote("https://example.com/a.jpg")),
        title = "Cane chair",
        description = "Light and sturdy",
        material = MaterialType.WOOD,
        ageYears = "2",
        condition = FurnitureCondition.GOOD,
        city = Cities.ALL.first(),
        actionType = ListingActionType.EXCHANGE,
    )

    @Test
    fun exchangeListing_priceIsOptional() {
        assertNull(exchangeForm.copy(price = "").validate(SellStep.PRICE))
        assertNull(exchangeForm.copy(price = "3500").validate(SellStep.PRICE))
        assertNotNull(exchangeForm.copy(price = "0").validate(SellStep.PRICE))
        assertNotNull(exchangeForm.copy(price = "abc").validate(SellStep.PRICE))
    }

    @Test
    fun saleListing_stillNeedsAPrice() {
        val sale = exchangeForm.copy(actionType = ListingActionType.SELL)
        assertNotNull(sale.copy(price = "").validate(SellStep.PRICE))
        assertNull(sale.copy(price = "3500").validate(SellStep.PRICE))
    }
}
