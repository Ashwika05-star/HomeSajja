package com.homesajja.app.report

import com.homesajja.app.data.model.RatingSummary
import com.homesajja.app.viewmodel.EarnedItem
import com.homesajja.app.viewmodel.EarnedKind
import com.homesajja.app.viewmodel.ExpenditureItem
import com.homesajja.app.viewmodel.VendorTaskCounts
import com.homesajja.app.viewmodel.earnedInMonth
import com.homesajja.app.viewmodel.expendituresInMonth
import com.homesajja.app.viewmodel.monthLabel
import com.homesajja.app.viewmodel.totalExpenditureOf
import com.homesajja.app.viewmodel.totalOf
import java.util.TimeZone

/** Payments of one kind received in the report's month. */
data class KindTotal(val kind: EarnedKind, val count: Int, val amount: Long)

/**
 * A vendor's summary for one month, ready to be turned into a PDF or CSV. [payments] are the payments confirmed as received during
 * [monthStart]'s month, newest first; only those count as earned. [expenditures] are purchases made by the vendor.
 * [tasks] and [rating] are the vendor's position when the report was made.
 */
data class MonthlyReport(
    val businessName: String,
    val city: String,
    val monthStart: Long,
    val monthLabel: String,
    val generatedAt: Long,
    val payments: List<EarnedItem>,
    val totalEarned: Long,
    val expenditures: List<ExpenditureItem> = emptyList(),
    val totalExpenditure: Long = 0L,
    val profit: Long = totalEarned - totalExpenditure,
    val byKind: List<KindTotal>,
    val tasks: VendorTaskCounts,
    val rating: RatingSummary,
    /** True when the month had more payments than one load reads, so the list and totals cover the newest ones. */
    val isPartial: Boolean,
)

/** Works out the report for the month starting at [monthStart] from the payments and expenditures that were read for it. */
fun buildMonthlyReport(
    businessName: String,
    city: String,
    monthStart: Long,
    earned: List<EarnedItem>,
    expenditures: List<ExpenditureItem> = emptyList(),
    tasks: VendorTaskCounts,
    rating: RatingSummary,
    isPartial: Boolean = false,
    now: Long = System.currentTimeMillis(),
    zone: TimeZone = TimeZone.getDefault(),
): MonthlyReport {
    val payments = earnedInMonth(earned, monthStart, zone).sortedByDescending { it.confirmedAt }
    val exps = expendituresInMonth(expenditures, monthStart, zone).sortedByDescending { it.confirmedAt }
    val totalEarned = totalOf(payments)
    val totalExpenditure = totalExpenditureOf(exps)
    return MonthlyReport(
        businessName = businessName,
        city = city,
        monthStart = monthStart,
        monthLabel = monthLabel(monthStart, full = true, zone = zone),
        generatedAt = now,
        payments = payments,
        totalEarned = totalEarned,
        expenditures = exps,
        totalExpenditure = totalExpenditure,
        profit = totalEarned - totalExpenditure,
        byKind = EarnedKind.entries.map { kind ->
            val ofKind = payments.filter { it.kind == kind }
            KindTotal(kind, ofKind.size, totalOf(ofKind))
        },
        tasks = tasks,
        rating = rating,
        isPartial = isPartial,
    )
}

/** Overload for callers that don't pass expenditures. */
fun buildMonthlyReport(
    businessName: String,
    city: String,
    monthStart: Long,
    earned: List<EarnedItem>,
    tasks: VendorTaskCounts,
    rating: RatingSummary,
    isPartial: Boolean = false,
    now: Long = System.currentTimeMillis(),
    zone: TimeZone = TimeZone.getDefault(),
): MonthlyReport = buildMonthlyReport(
    businessName = businessName,
    city = city,
    monthStart = monthStart,
    earned = earned,
    expenditures = emptyList(),
    tasks = tasks,
    rating = rating,
    isPartial = isPartial,
    now = now,
    zone = zone,
)

/** The file name for a report, e.g. `HomeSajja-report-2026-09.pdf`. */
fun reportFileName(monthStart: Long, extension: String, zone: TimeZone = TimeZone.getDefault()): String =
    "HomeSajja-report-" + java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.ENGLISH).apply { timeZone = zone }.format(monthStart) + "." + extension
