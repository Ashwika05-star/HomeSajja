package com.homesajja.app.viewmodel

import com.homesajja.app.data.model.PayDirection
import com.homesajja.app.data.model.Payment
import com.homesajja.app.data.model.PaymentMethod
import com.homesajja.app.data.model.PaymentStatus
import com.homesajja.app.data.model.PurchaseRequest
import com.homesajja.app.data.model.RatingSummary
import com.homesajja.app.data.model.RecycleMaterial
import com.homesajja.app.data.model.RecyclingRequest
import com.homesajja.app.data.model.Refund
import com.homesajja.app.data.model.RepairRequest
import com.homesajja.app.data.model.VendorProfile
import com.homesajja.app.report.CsvReport
import com.homesajja.app.report.buildMonthlyReport
import com.homesajja.app.report.reportFileName
import com.homesajja.app.ui.util.formatCompactPrice
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Calendar
import java.util.TimeZone

private val IST: TimeZone = TimeZone.getTimeZone("Asia/Kolkata")

/** A moment in [IST]: `at(2026, Calendar.SEPTEMBER, 15)`. */
private fun at(year: Int, month: Int, day: Int, hour: Int = 12): Long =
    Calendar.getInstance(IST).apply { clear(); set(year, month, day, hour, 0, 0) }.timeInMillis

class MonthMathTest {

    @Test
    fun monthStart_isTheFirstInstantOfTheMonth() {
        val start = monthStart(at(2026, Calendar.SEPTEMBER, 15), IST)
        assertEquals(at(2026, Calendar.SEPTEMBER, 1, 0), start)
        assertEquals(start, monthStart(start, IST))
        // The very last moment of September still belongs to September.
        assertEquals(start, monthStart(at(2026, Calendar.OCTOBER, 1, 0) - 1, IST))
    }

    @Test
    fun shiftMonth_crossesYearEnds_andShortMonths() {
        val jan = at(2026, Calendar.JANUARY, 1, 0)
        assertEquals(at(2025, Calendar.DECEMBER, 1, 0), shiftMonth(jan, -1, IST))
        assertEquals(at(2026, Calendar.FEBRUARY, 1, 0), shiftMonth(jan, 1, IST))
        assertEquals(at(2026, Calendar.MARCH, 1, 0), shiftMonth(at(2026, Calendar.JANUARY, 1, 0), 2, IST))
    }

    @Test
    fun lastMonthStarts_areSixMonthsOldestFirst_endingWithTheCurrentOne() {
        val starts = lastMonthStarts(at(2026, Calendar.OCTOBER, 4), 6, IST)
        assertEquals(6, starts.size)
        assertEquals(listOf("May", "Jun", "Jul", "Aug", "Sep", "Oct"), starts.map { monthLabel(it, zone = IST) })
        assertEquals(starts.sorted(), starts)
    }

    @Test
    fun lastMonthStarts_acrossANewYear() {
        val labels = lastMonthStarts(at(2026, Calendar.FEBRUARY, 10), 6, IST).map { monthLabel(it, zone = IST) }
        assertEquals(listOf("Sep", "Oct", "Nov", "Dec", "Jan", "Feb"), labels)
    }

    @Test
    fun monthLabel_canBeLong() {
        assertEquals("September 2026", monthLabel(at(2026, Calendar.SEPTEMBER, 1, 0), full = true, zone = IST))
    }

    @Test
    fun fileName_namesTheMonth() {
        assertEquals("HomeSajja-report-2026-09.pdf", reportFileName(at(2026, Calendar.SEPTEMBER, 1, 0), "pdf", IST))
        assertEquals("HomeSajja-report-2026-09.csv", reportFileName(at(2026, Calendar.SEPTEMBER, 20), "csv", IST))
    }

    @Test
    fun compactPrices_readLikeIndianAmounts() {
        assertEquals("₹0", formatCompactPrice(0))
        assertEquals("₹850", formatCompactPrice(850))
        assertEquals("₹1k", formatCompactPrice(1_000))
        assertEquals("₹1.2k", formatCompactPrice(1_240))
        assertEquals("₹1.3k", formatCompactPrice(1_250))
        assertEquals("₹45k", formatCompactPrice(45_000))
        assertEquals("₹1L", formatCompactPrice(100_000))
        assertEquals("₹1.5L", formatCompactPrice(150_000))
        assertEquals("₹2.4Cr", formatCompactPrice(24_000_000))
    }
}

class EarnedItemTest {

    private fun payment(status: PaymentStatus, payee: String = "vendor", payer: String = "customer", confirmedAt: Long? = 500L) =
        Payment(payerId = payer, payeeId = payee, status = status, method = PaymentMethod.UPI, confirmedAt = confirmedAt)

    @Test
    fun onlyAConfirmedPayment_toThatPerson_isEarned() {
        fun repair(p: Payment?) = RepairRequest(id = "r", userName = "Uma", furnitureTitle = "Sofa", agreedAmount = 2500, payment = p)
        assertNotNull(repair(payment(PaymentStatus.CONFIRMED)).toEarnedItem("vendor"))
        assertNull(repair(payment(PaymentStatus.MARKED_PAID)).toEarnedItem("vendor"))
        assertNull(repair(payment(PaymentStatus.UNPAID)).toEarnedItem("vendor"))
        assertNull(repair(null).toEarnedItem("vendor"))
        // a confirmed payment that went to someone else is that person's money, not this vendor's
        assertNull(repair(payment(PaymentStatus.CONFIRMED, payee = "customer", payer = "vendor")).toEarnedItem("vendor"))
        assertNull(RepairRequest(payment = payment(PaymentStatus.CONFIRMED), agreedAmount = null).toEarnedItem("vendor"))
    }

    @Test
    fun aRefundedPayment_isNotEarned() {
        val refunded = PurchaseRequest(agreedAmount = 900, payment = payment(PaymentStatus.CONFIRMED), refund = Refund(amount = 900))
        assertNull(refunded.toEarnedItem("vendor"))
        assertNull(RecyclingRequest(agreedAmount = 300, payment = payment(PaymentStatus.CONFIRMED), refund = Refund(300)).toEarnedItem("vendor"))
    }

    @Test
    fun theItemCarriesWhatTheReportsNeed() {
        val sale = PurchaseRequest(id = "p", listingTitle = "Teak sofa", buyerName = "Aarav", agreedAmount = 4000, payment = payment(PaymentStatus.CONFIRMED))
            .toEarnedItem("vendor")!!
        assertEquals(EarnedKind.SALE, sale.kind)
        assertEquals("Teak sofa", sale.title)
        assertEquals("Aarav", sale.counterpart)
        assertEquals(4000L, sale.amount)
        assertEquals(PaymentMethod.UPI, sale.method)
        assertEquals(500L, sale.confirmedAt)
        val recycling = RecyclingRequest(userName = "Uma", material = RecycleMaterial.METAL, agreedAmount = 300, payment = payment(PaymentStatus.CONFIRMED)).toEarnedItem("vendor")!!
        assertEquals(EarnedKind.RECYCLING, recycling.kind)
        assertEquals("Metal furniture", recycling.title)
    }

    @Test
    fun anOldPaymentWithoutAConfirmationTime_fallsBackToTheLastUpdate() {
        val item = PurchaseRequest(agreedAmount = 100, updatedAt = 77L, payment = payment(PaymentStatus.CONFIRMED, confirmedAt = null)).toEarnedItem("vendor")!!
        assertEquals(77L, item.confirmedAt)
    }
}

class MonthlyEarningsTest {

    private fun item(amount: Long, confirmedAt: Long, kind: EarnedKind = EarnedKind.SALE, id: String = "i$confirmedAt") =
        EarnedItem(id, kind, "Item", "Customer", amount, PaymentMethod.CASH, confirmedAt)

    private val now = at(2026, Calendar.OCTOBER, 4)

    @Test
    fun monthlyTotals_alwaysGiveSixMonths_includingEmptyOnes() {
        val totals = monthlyTotals(listOf(item(1000, at(2026, Calendar.OCTOBER, 2))), now, 6, IST)
        assertEquals(6, totals.size)
        assertEquals(listOf(0L, 0L, 0L, 0L, 0L, 1000L), totals.map { it.amount })
        assertEquals("Oct", totals.last().label)
        assertEquals(1, totals.last().count)
        assertEquals(0, totals.first().count)
    }

    @Test
    fun paymentsAreBucketedByTheirConfirmationMonth() {
        val items = listOf(
            item(500, at(2026, Calendar.SEPTEMBER, 30, 23)),
            item(700, at(2026, Calendar.OCTOBER, 1, 0)),
            item(300, at(2026, Calendar.MAY, 1, 0)),
        )
        val totals = monthlyTotals(items, now, 6, IST).associate { it.label to it.amount }
        assertEquals(500L, totals["Sep"])
        assertEquals(700L, totals["Oct"])
        assertEquals(300L, totals["May"])
    }

    @Test
    fun paymentsOlderThanSixMonths_dropOffTheChartButNotTheTotal() {
        val old = item(900, at(2025, Calendar.DECEMBER, 5))
        val recent = item(100, at(2026, Calendar.OCTOBER, 3))
        assertEquals(100L, monthlyTotals(listOf(old, recent), now, 6, IST).sumOf { it.amount })
        assertEquals(1000L, totalOf(listOf(old, recent)))
    }

    @Test
    fun earnedInMonth_includesTheFirstInstant_andExcludesTheNextMonth() {
        val sep = at(2026, Calendar.SEPTEMBER, 1, 0)
        val items = listOf(item(1, sep), item(2, at(2026, Calendar.OCTOBER, 1, 0)), item(3, sep - 1))
        assertEquals(listOf(1L), earnedInMonth(items, sep, IST).map { it.amount })
    }

    @Test
    fun totals_ofNothing_areZero() {
        assertEquals(0L, totalOf(emptyList()))
        assertTrue(monthlyTotals(emptyList(), now, 6, IST).all { it.amount == 0L && it.count == 0 })
    }
}

class DashboardBuildTest {

    private val now = at(2026, Calendar.OCTOBER, 4)
    private val vendor = VendorProfile(uid = "vendor", name = "Vic", businessName = "Vic Repairs", city = "Mumbai")

    private fun item(amount: Long, confirmedAt: Long) =
        EarnedItem("i$confirmedAt", EarnedKind.REPAIR, "Sofa", "Uma", amount, PaymentMethod.UPI, confirmedAt)

    @Test
    fun totalAndThisMonth_countOnlyTheEarnedPayments() {
        val earned = listOf(item(2500, at(2026, Calendar.OCTOBER, 2)), item(1500, at(2026, Calendar.SEPTEMBER, 20)))
        val data = buildDashboard(vendor, 3, VendorTaskCounts(), earned, RatingSummary(), now = now, zone = IST)
        assertEquals(4000L, data.summary.totalEarned)
        assertEquals(2500L, data.summary.earnedThisMonth)
        assertEquals(listOf(0L, 0L, 0L, 0L, 1500L, 2500L), data.summary.months.map { it.amount })
        assertEquals(3, data.activeListings)
    }

    @Test
    fun aNewVendor_isRecognised_andOneWithAnyHistoryIsNot() {
        assertTrue(buildDashboard(vendor, 0, VendorTaskCounts(), emptyList(), RatingSummary(), now = now, zone = IST).isNew)
        val withPending = VendorTaskCounts(sales = TaskCount(pending = 1))
        assertFalse(buildDashboard(vendor, 0, withPending, emptyList(), RatingSummary(), now = now, zone = IST).isNew)
        val withReview = RatingSummary(4.5, 2)
        assertFalse(buildDashboard(vendor, 0, VendorTaskCounts(), emptyList(), withReview, now = now, zone = IST).isNew)
        assertFalse(buildDashboard(vendor, 0, VendorTaskCounts(), listOf(item(10, now)), RatingSummary(), now = now, zone = IST).isNew)
    }

    @Test
    fun taskCounts_addUpAcrossTheFourTypes() {
        val counts = VendorTaskCounts(TaskCount(2, 1), TaskCount(3, 0), TaskCount(1, 2), TaskCount(0, 4))
        assertEquals(6, counts.completed)
        assertEquals(7, counts.pending)
    }

    @Test
    fun partialEarnings_areFlagged() {
        assertTrue(buildDashboard(vendor, 0, VendorTaskCounts(), emptyList(), RatingSummary(), earnedIsPartial = true, now = now, zone = IST).summary.earnedIsPartial)
        assertFalse(buildDashboard(vendor, 0, VendorTaskCounts(), emptyList(), RatingSummary(), now = now, zone = IST).summary.earnedIsPartial)
    }

    @Test
    fun profileIncomplete_untilDescriptionAndLocationAreSet() {
        fun incomplete(v: VendorProfile) = buildDashboard(v, 0, VendorTaskCounts(), emptyList(), RatingSummary(), now = now, zone = IST).profileIncomplete
        assertTrue(incomplete(vendor))
        val noPin = vendor.copy(description = "We fix furniture")
        assertTrue(incomplete(noPin))
        assertFalse(incomplete(noPin.copy(shopLatitude = 19.0, shopLongitude = 72.8)))
    }
}

class ProfileSummaryTest {

    @Test
    fun itemsReused_countsSalesPurchasesAndBothItemsOfEverySwap() {
        val counts = UserTaskCounts(itemsSold = 2, itemsBought = 3, exchanges = 4, repaired = 1, recycled = 5)
        assertEquals(2 + 3 + 4 * 2, counts.itemsReused)
    }

    @Test
    fun theProfileSummary_isEmptyUntilSomethingHappened() {
        assertTrue(ProfileSummary(0, false, UserTaskCounts()).isEmpty)
        assertFalse(ProfileSummary(500, false, UserTaskCounts()).isEmpty)
        assertFalse(ProfileSummary(0, false, UserTaskCounts(repaired = 1)).isEmpty)
        assertFalse(ProfileSummary(0, false, UserTaskCounts(recycled = 1)).isEmpty)
        assertFalse(ProfileSummary(0, false, UserTaskCounts(itemsBought = 1)).isEmpty)
    }

    @Test
    fun itemsSold_comesFromCompletedSales() {
        val summary = ProfileSummary(4200, false, UserTaskCounts(itemsSold = 3))
        assertEquals(3, summary.itemsSold)
        assertEquals(4200L, summary.totalEarned)
    }
}

class MonthlyReportTest {

    private val payments = listOf(
        EarnedItem("a", EarnedKind.SALE, "Teak sofa", "Aarav", 4000, PaymentMethod.UPI, at(2026, Calendar.SEPTEMBER, 12)),
        EarnedItem("b", EarnedKind.REPAIR, "Oak chair", "Uma", 1500, PaymentMethod.CASH, at(2026, Calendar.SEPTEMBER, 20)),
        EarnedItem("c", EarnedKind.REPAIR, "Desk", "Ravi", 900, PaymentMethod.UPI, at(2026, Calendar.SEPTEMBER, 25)),
        EarnedItem("d", EarnedKind.SALE, "Other month", "Zed", 777, PaymentMethod.UPI, at(2026, Calendar.AUGUST, 30)),
    )
    private val september = at(2026, Calendar.SEPTEMBER, 1, 0)

    private fun report() = buildMonthlyReport("Vic Repairs", "Mumbai", september, payments, VendorTaskCounts(), RatingSummary(4.5, 2), now = at(2026, Calendar.OCTOBER, 4), zone = IST)

    @Test
    fun theReportOnlyHoldsThatMonthsPayments_newestFirst() {
        val r = report()
        assertEquals(listOf("c", "b", "a"), r.payments.map { it.id })
        assertEquals(6400L, r.totalEarned)
        assertEquals("September 2026", r.monthLabel)
    }

    @Test
    fun theReportTotalsEachKind() {
        val byKind = report().byKind.associateBy { it.kind }
        assertEquals(1, byKind.getValue(EarnedKind.SALE).count)
        assertEquals(4000L, byKind.getValue(EarnedKind.SALE).amount)
        assertEquals(2, byKind.getValue(EarnedKind.REPAIR).count)
        assertEquals(2400L, byKind.getValue(EarnedKind.REPAIR).amount)
        assertEquals(0, byKind.getValue(EarnedKind.RECYCLING).count)
    }

    @Test
    fun anEmptyMonth_isStillAValidReport() {
        val r = buildMonthlyReport("Vic", "Mumbai", at(2026, Calendar.JANUARY, 1, 0), payments, VendorTaskCounts(), RatingSummary(), zone = IST)
        assertTrue(r.payments.isEmpty())
        assertEquals(0L, r.totalEarned)
        assertEquals(3, r.byKind.size)
    }

    @Test
    fun csv_hasTheSummaryThePaymentsAndTheStatus() {
        val csv = CsvReport.build(report(), IST)
        val lines = csv.trimEnd().split("\r\n")
        assertEquals("HomeSajja monthly report", lines[0])
        assertTrue(lines.contains("Business,Vic Repairs"))
        assertTrue(lines.contains("Month,September 2026"))
        assertTrue(lines.contains("Generated,2026-10-04"))
        assertTrue(lines.contains("Total earned (INR),6400"))
        assertTrue(lines.contains("Sales,1,4000"))
        assertTrue(lines.contains("Repairs,2,2400"))
        assertTrue(lines.contains("Date,Type,Item,Customer,Method,Amount (INR)"))
        assertTrue(lines.contains("2026-09-25,Repair,Desk,Ravi,UPI,900"))
        assertTrue(lines.contains("2026-09-12,Sale,Teak sofa,Aarav,UPI,4000"))
        assertTrue(lines.contains("Average rating,4.5,2"))
        assertFalse(csv.contains("Other month"))
        assertTrue(csv.endsWith("\r\n"))
    }

    @Test
    fun csvFields_areQuotedAndSafe() {
        assertEquals("plain", CsvReport.field("plain"))
        assertEquals("\"a,b\"", CsvReport.field("a,b"))
        assertEquals("\"say \"\"hi\"\"\"", CsvReport.field("say \"hi\""))
        assertEquals("\"two\nlines\"", CsvReport.field("two\nlines"))
        // A name that starts like a formula is defused so a spreadsheet never runs it.
        assertEquals("'=SUM(A1)", CsvReport.field("=SUM(A1)"))
        assertEquals("'+91 98", CsvReport.field("+91 98"))
        assertEquals("'@cmd", CsvReport.field("@cmd"))
        assertEquals("", CsvReport.field(""))
    }

    @Test
    fun csv_keepsAwkwardCustomerNamesIntact() {
        val tricky = payments.map { it.copy(counterpart = "Rao, \"Ravi\" =x") }
        val r = buildMonthlyReport("Vic", "Mumbai", september, tricky, VendorTaskCounts(), RatingSummary(), zone = IST)
        assertTrue(CsvReport.build(r, IST).contains("\"Rao, \"\"Ravi\"\" =x\""))
    }
}

/** The new queries need composite indexes (money received, newest first; the rating aggregation). A missing one only shows up on the real database. */
class StatsIndexTest {

    private fun indexes(collection: String): List<List<String>> {
        val file = listOf(File("../firestore.indexes.json"), File("firestore.indexes.json")).first { it.exists() }
        val all = JSONObject(file.readText()).getJSONArray("indexes")
        return (0 until all.length()).map { all.getJSONObject(it) }.filter { it.getString("collectionGroup") == collection }.map { index ->
            val fields = index.getJSONArray("fields")
            (0 until fields.length()).map { fields.getJSONObject(it).getString("fieldPath") }
        }
    }

    @Test
    fun confirmedPaymentsAreIndexedForEachCollection() {
        assertTrue(indexes("purchaseRequests").contains(listOf("sellerId", "payment.status", "payment.confirmedAt")))
        assertTrue(indexes("repairRequests").contains(listOf("vendorId", "payment.status", "payment.confirmedAt")))
        assertTrue(indexes("recyclingRequests").contains(listOf("vendorId", "payment.payeeId", "payment.status", "payment.confirmedAt")))
    }

    @Test
    fun theRatingAggregationIsIndexed() {
        assertTrue(indexes("reviews").contains(listOf("targetUserId", "rating")))
    }
}
