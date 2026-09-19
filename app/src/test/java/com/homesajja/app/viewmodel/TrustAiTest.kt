package com.homesajja.app.viewmodel

import android.net.Uri
import com.homesajja.app.data.model.FlowPrefill
import com.homesajja.app.data.model.FurnitureAssessment
import com.homesajja.app.data.model.FurnitureCategory
import com.homesajja.app.data.model.FurnitureListing
import com.homesajja.app.data.model.ListingActionType
import com.homesajja.app.data.model.ListingStatus
import com.homesajja.app.data.model.MaterialType
import com.homesajja.app.data.model.RatingSummary
import com.homesajja.app.data.model.Recommendation
import com.homesajja.app.data.model.RecycleCondition
import com.homesajja.app.data.model.RecycleMaterial
import com.homesajja.app.data.model.RepairProblemType
import com.homesajja.app.data.model.Review
import com.homesajja.app.repository.AiParsing
import com.homesajja.app.repository.AiResponseException
import com.homesajja.app.ui.components.ratingText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class AiParsingTest {

    @Test
    fun aSellAnswer_isReadWithItsPriceRange() {
        val a = AiParsing.parseAssessment(
            """{"recommendation":"SELL","reasoning":"Solid teak in good shape.","category":"SOFA","material":"WOOD","priceMin":8000,"priceMax":12000}""",
        )
        assertEquals(Recommendation.SELL, a.recommendation)
        assertEquals("Solid teak in good shape.", a.reasoning)
        assertEquals(FurnitureCategory.SOFA, a.category)
        assertEquals(RecycleMaterial.WOOD, a.material)
        assertEquals(8000L, a.priceMin)
        assertEquals(12000L, a.priceMax)
    }

    @Test
    fun repairAndRecycleAnswers_dropAnyPrice() {
        val repair = AiParsing.parseAssessment("""{"recommendation":"REPAIR","reasoning":"Loose leg.","problemType":"LOOSE_JOINTS","priceMin":100,"priceMax":200}""")
        assertNull(repair.priceMin)
        assertNull(repair.priceMax)
        assertEquals(RepairProblemType.LOOSE_JOINTS, repair.problemType)
        val recycle = AiParsing.parseAssessment("""{"recommendation":"recycle","reasoning":"Too far gone.","recycleCondition":"BEYOND_REPAIR"}""")
        assertEquals(Recommendation.RECYCLE, recycle.recommendation)
        assertEquals(RecycleCondition.BEYOND_REPAIR, recycle.recycleCondition)
    }

    @Test
    fun aReversedPriceRange_isPutRightWayRound() {
        val a = AiParsing.parseAssessment("""{"recommendation":"EXCHANGE","reasoning":"Swap it.","priceMin":5000,"priceMax":3000}""")
        assertEquals(3000L, a.priceMin)
        assertEquals(5000L, a.priceMax)
    }

    @Test
    fun unknownWords_areDroppedNotFatal() {
        val a = AiParsing.parseAssessment("""{"recommendation":"SELL","reasoning":"Fine.","category":"SPACESHIP","material":"UNOBTAINIUM"}""")
        assertNull(a.category)
        assertNull(a.material)
    }

    @Test
    fun aCodeFencedAnswer_isStillReadable() {
        val a = AiParsing.parseAssessment("```json\n{\"recommendation\":\"SELL\",\"reasoning\":\"Ok.\"}\n```")
        assertEquals(Recommendation.SELL, a.recommendation)
    }

    @Test
    fun unusableAnswers_areRejected() {
        listOf("not json", "{}", """{"recommendation":"DONATE","reasoning":"x"}""", """{"recommendation":"SELL","reasoning":"  "}""").forEach {
            try {
                AiParsing.parseAssessment(it)
                fail("should have been rejected: $it")
            } catch (expected: AiResponseException) {
            }
        }
    }

    @Test
    fun chatbotReply_keepsOnlyListingsItWasShown() {
        val reply = AiParsing.parseChatbotReply(
            """{"reply":"Try these.","listingIds":["a","ghost","b","a"]}""",
            knownListingIds = setOf("a", "b", "c"),
        )
        assertEquals("Try these.", reply.text)
        assertEquals(listOf("a", "b"), reply.listingIds)
    }

    @Test
    fun chatbotReply_isCappedAtFourListings() {
        val ids = (1..9).joinToString(",") { "\"id$it\"" }
        val reply = AiParsing.parseChatbotReply("""{"reply":"Lots.","listingIds":[$ids]}""", (1..9).map { "id$it" }.toSet())
        assertEquals(4, reply.listingIds.size)
    }

    @Test
    fun chatbotReply_plainTextIsUsedAsTheReply() {
        assertEquals("Just text", AiParsing.parseChatbotReply("Just text", emptySet()).text)
    }

    @Test
    fun chatbotReply_emptyIsRejected() {
        try {
            AiParsing.parseChatbotReply("""{"reply":""}""", emptySet())
            fail()
        } catch (expected: AiResponseException) {
        }
    }
}

class FlowPrefillTest {

    private val assessment = FurnitureAssessment(
        recommendation = Recommendation.SELL,
        reasoning = "Good.",
        category = FurnitureCategory.TABLE,
        material = RecycleMaterial.MIXED,
        recycleCondition = RecycleCondition.REUSABLE,
        problemType = RepairProblemType.SCRATCHES,
        priceMin = 4000,
        priceMax = 5000,
    )

    @Test
    fun suggestedPrice_isTheRoundedMiddle() {
        assertEquals("4500", suggestedPriceText(4000, 5000))
        assertEquals("4600", suggestedPriceText(4520, 4640))
        assertNull(suggestedPriceText(null, 5000))
        assertNull(suggestedPriceText(0, 0))
    }

    @Test
    fun sellPrefill_fillsCategoryMaterialAndPrice() {
        val form = SellForm().withPrefill(FlowPrefill(Recommendation.SELL, null, assessment))
        assertEquals(FurnitureCategory.TABLE, form.category)
        assertEquals(MaterialType.OTHER, form.material)
        assertEquals("4500", form.price)
        assertEquals(ListingActionType.SELL, form.actionType)
    }

    @Test
    fun exchangePrefill_switchesToExchange_andLeavesPriceAlone() {
        val form = SellForm().withPrefill(FlowPrefill(Recommendation.EXCHANGE, null, assessment))
        assertEquals(ListingActionType.EXCHANGE, form.actionType)
        assertEquals("", form.price)
    }

    @Test
    fun aPrefillWithNothingInIt_changesNothing() {
        val blank = FlowPrefill(Recommendation.SELL, null, null)
        assertEquals(SellForm().copy(actionType = ListingActionType.SELL), SellForm().withPrefill(blank))
        assertNull(RepairForm().withPrefill(FlowPrefill(Recommendation.REPAIR, null, null)).problem)
    }

    @Test
    fun repairPrefill_startsAFreeformRequestWithTheProblem() {
        val form = RepairForm().withPrefill(FlowPrefill(Recommendation.REPAIR, null, assessment))
        assertTrue(form.notListed)
        assertEquals(RepairProblemType.SCRATCHES, form.problem)
        assertEquals(FurnitureCategory.TABLE, form.category)
    }

    @Test
    fun recyclePrefill_fillsConditionAndMaterial() {
        val form = RecycleForm().withPrefill(FlowPrefill(Recommendation.RECYCLE, null, assessment))
        assertEquals(RecycleCondition.REUSABLE, form.condition)
        assertEquals(RecycleMaterial.MIXED, form.material)
    }
}

class RatingTest {

    private fun review(rating: Int) = Review(rating = rating)

    @Test
    fun summary_isTheAverageAndCount() {
        val s = RatingSummary.of(listOf(review(5), review(4), review(3)))
        assertEquals(4.0, s.average, 0.001)
        assertEquals(3, s.count)
        assertTrue(s.hasRatings)
    }

    @Test
    fun noReviews_meansNoRatings() {
        assertFalse(RatingSummary.of(emptyList()).hasRatings)
    }

    @Test
    fun ratingText_readsWell() {
        assertEquals("☆ No ratings yet", ratingText(null))
        assertEquals("☆ No ratings yet", ratingText(RatingSummary()))
        assertEquals("★ 4.5 (2 reviews)", ratingText(RatingSummary(4.5, 2)))
        assertEquals("★ 5.0 (1 review)", ratingText(RatingSummary(5.0, 1)))
    }
}

class SavedItemTest {

    private fun item(status: ListingStatus?) = SavedItem("l", status?.let { FurnitureListing(id = "l", status = it) })

    @Test
    fun activeAndReserved_areStillAvailable() {
        assertTrue(item(ListingStatus.ACTIVE).isAvailable)
        assertTrue(item(ListingStatus.RESERVED).isAvailable)
    }

    @Test
    fun deletedSoldAndHidden_areNoLongerAvailable() {
        assertFalse(item(null).isAvailable)
        listOf(ListingStatus.SOLD, ListingStatus.EXCHANGED, ListingStatus.REMOVED, ListingStatus.UNAVAILABLE).forEach {
            assertFalse(item(it).isAvailable)
        }
    }
}

class ImageUrlTest {

    private val original = "https://res.cloudinary.com/demo/image/upload/v1789763882/listings/u/l/photo.jpg"

    @Test
    fun cloudinaryPhotos_areAskedForInASmallerCompressedVersion() {
        assertEquals(
            "https://res.cloudinary.com/demo/image/upload/w_560,c_limit,q_auto,f_auto/v1789763882/listings/u/l/photo.jpg",
            com.homesajja.app.ui.util.optimizedImage(original, 560),
        )
    }

    @Test
    fun otherImages_areLeftAlone() {
        assertEquals("https://picsum.photos/seed/x/400/300", com.homesajja.app.ui.util.optimizedImage("https://picsum.photos/seed/x/400/300", 560))
        assertNull(com.homesajja.app.ui.util.optimizedImage(null, 560))
        val localPhoto = Any()
        assertEquals(localPhoto, com.homesajja.app.ui.util.optimizedImage(localPhoto, 560))
    }

    @Test
    fun anAlreadyResizedUrl_isNotResizedTwice() {
        val resized = "https://res.cloudinary.com/demo/image/upload/w_100/v1/x.jpg"
        assertEquals(resized, com.homesajja.app.ui.util.optimizedImage(resized, 560))
    }
}
