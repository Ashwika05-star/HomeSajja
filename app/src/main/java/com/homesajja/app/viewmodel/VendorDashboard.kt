package com.homesajja.app.viewmodel

import com.homesajja.app.data.model.RatingSummary
import com.homesajja.app.data.model.VendorProfile
import java.util.TimeZone

/** The few numbers the vendor dashboard shows. */
data class DashboardSummary(
    /** Money actually received: only payments the vendor confirmed, and not refunded (see [EarnedItem]). */
    val totalEarned: Long,
    val earnedThisMonth: Long,
    /** The last six months, oldest first, for the chart. */
    val months: List<MonthTotal>,
    val tasks: VendorTaskCounts,
    val rating: RatingSummary,
    /** True when there were more confirmed payments than one load reads, so [totalEarned] covers the most recent ones only. */
    val earnedIsPartial: Boolean,
)

/** [profileIncomplete] nudges the vendor to finish their profile (description and a shop location). */
data class DashboardData(
    val vendor: VendorProfile,
    val summary: DashboardSummary,
    val activeListings: Int,
    val profileIncomplete: Boolean,
) {
    /** A vendor with no sales, jobs, payments or reviews yet: the dashboard welcomes them instead of showing a wall of zeros. */
    val isNew: Boolean
        get() = summary.tasks.completed == 0 && summary.tasks.pending == 0 && summary.totalEarned == 0L && !summary.rating.hasRatings
}

/**
 * Turns what the repository read into what the dashboard shows. "Earned" counts only the [earned] payments, which are already limited
 * to money the vendor confirmed receiving; "this month" and the chart are worked out in the device's time zone.
 */
fun buildDashboard(
    vendor: VendorProfile,
    activeListings: Int,
    tasks: VendorTaskCounts,
    earned: List<EarnedItem>,
    rating: RatingSummary,
    earnedIsPartial: Boolean = false,
    now: Long = System.currentTimeMillis(),
    zone: TimeZone = TimeZone.getDefault(),
): DashboardData = DashboardData(
    vendor = vendor,
    summary = DashboardSummary(
        totalEarned = totalOf(earned),
        earnedThisMonth = totalOf(earnedInMonth(earned, monthStart(now, zone), zone)),
        months = monthlyTotals(earned, now, 6, zone),
        tasks = tasks,
        rating = rating,
        earnedIsPartial = earnedIsPartial,
    ),
    activeListings = activeListings,
    profileIncomplete = vendor.description.isBlank() || vendor.shopLatitude == null || vendor.shopLongitude == null,
)
