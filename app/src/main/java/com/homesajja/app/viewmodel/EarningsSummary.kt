package com.homesajja.app.viewmodel

import com.homesajja.app.data.model.PaymentMethod
import com.homesajja.app.data.model.PaymentStatus
import com.homesajja.app.data.model.PurchaseRequest
import com.homesajja.app.data.model.RecyclingRequest
import com.homesajja.app.data.model.RepairRequest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/** What kind of request a payment came from. */
enum class EarnedKind(val label: String, val plural: String) {
    SALE("Sale", "Sales"),
    REPAIR("Repair", "Repairs"),
    RECYCLING("Recycling", "Recycling jobs"),
}

/**
 * One payment someone has actually earned: the payee confirmed it as received and it was not refunded. [counterpart] is the
 * person who paid. This is the only thing that counts as "Earned" anywhere in the app.
 */
data class EarnedItem(
    val id: String,
    val kind: EarnedKind,
    val title: String,
    val counterpart: String,
    val amount: Long,
    val method: PaymentMethod?,
    val confirmedAt: Long,
)

private fun earnedAmount(payeeId: String, personId: String, status: PaymentStatus, refunded: Boolean, amount: Long?): Long? =
    if (payeeId == personId && status == PaymentStatus.CONFIRMED && !refunded) amount else null

/** The earned payment on this request for [personId], or null when they are not the payee, it isn't confirmed yet, or it was refunded. */
fun PurchaseRequest.toEarnedItem(personId: String): EarnedItem? {
    val p = payment ?: return null
    val amount = earnedAmount(p.payeeId, personId, p.status, refund != null, agreedAmount) ?: return null
    return EarnedItem(id, EarnedKind.SALE, listingTitle, buyerName, amount, p.method, p.confirmedAt ?: updatedAt)
}

fun RepairRequest.toEarnedItem(personId: String): EarnedItem? {
    val p = payment ?: return null
    val amount = earnedAmount(p.payeeId, personId, p.status, refund != null, agreedAmount) ?: return null
    return EarnedItem(id, EarnedKind.REPAIR, furnitureTitle, userName, amount, p.method, p.confirmedAt ?: updatedAt)
}

fun RecyclingRequest.toEarnedItem(personId: String): EarnedItem? {
    val p = payment ?: return null
    val amount = earnedAmount(p.payeeId, personId, p.status, refund != null, agreedAmount) ?: return null
    return EarnedItem(id, EarnedKind.RECYCLING, "${material.displayName} furniture", userName, amount, p.method, p.confirmedAt ?: updatedAt)
}

/** One bar of the monthly earnings chart. [startMillis] is the first instant of the month in the device's time zone. */
data class MonthTotal(val startMillis: Long, val label: String, val amount: Long, val count: Int)

fun totalOf(items: List<EarnedItem>): Long = items.sumOf { it.amount }

/** The first instant of the month that [timestamp] falls in. */
fun monthStart(timestamp: Long, zone: TimeZone = TimeZone.getDefault()): Long =
    Calendar.getInstance(zone).apply {
        timeInMillis = timestamp
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

/** The first instant of the month [delta] months after (or before, if negative) the month starting at [monthStart]. */
fun shiftMonth(monthStart: Long, delta: Int, zone: TimeZone = TimeZone.getDefault()): Long =
    Calendar.getInstance(zone).apply {
        timeInMillis = monthStart
        add(Calendar.MONTH, delta)
    }.timeInMillis

/** "Mar" style short label, or "March 2026" when [full]. */
fun monthLabel(monthStart: Long, full: Boolean = false, zone: TimeZone = TimeZone.getDefault()): String =
    SimpleDateFormat(if (full) "MMMM yyyy" else "MMM", Locale.ENGLISH).apply { timeZone = zone }.format(monthStart)

/** The start of each of the last [count] months up to and including the month of [now], oldest first. */
fun lastMonthStarts(now: Long, count: Int = 6, zone: TimeZone = TimeZone.getDefault()): List<Long> {
    val current = monthStart(now, zone)
    return (count - 1 downTo 0).map { shiftMonth(current, -it, zone) }
}

/** The payments confirmed during the month that starts at [monthStart]. */
fun earnedInMonth(items: List<EarnedItem>, monthStart: Long, zone: TimeZone = TimeZone.getDefault()): List<EarnedItem> {
    val end = shiftMonth(monthStart, 1, zone)
    return items.filter { it.confirmedAt in monthStart until end }
}

/** The totals for each of the last [count] months (oldest first), including months with nothing earned, ready for the chart. */
fun monthlyTotals(items: List<EarnedItem>, now: Long, count: Int = 6, zone: TimeZone = TimeZone.getDefault()): List<MonthTotal> =
    lastMonthStarts(now, count, zone).map { start ->
        val inMonth = earnedInMonth(items, start, zone)
        MonthTotal(start, monthLabel(start, zone = zone), totalOf(inMonth), inMonth.size)
    }

/** Completed and still-open requests of one kind. */
data class TaskCount(val completed: Int = 0, val pending: Int = 0)

/** What a vendor has done and has waiting, by type of work. */
data class VendorTaskCounts(
    val sales: TaskCount = TaskCount(),
    val repairs: TaskCount = TaskCount(),
    val exchanges: TaskCount = TaskCount(),
    val recycling: TaskCount = TaskCount(),
) {
    val completed: Int get() = sales.completed + repairs.completed + exchanges.completed + recycling.completed
    val pending: Int get() = sales.pending + repairs.pending + exchanges.pending + recycling.pending
}

/** A person's own completed items, from which the profile's earnings and sustainability cards are worked out. */
data class UserTaskCounts(
    val itemsSold: Int = 0,
    val itemsBought: Int = 0,
    val exchanges: Int = 0,
    val repaired: Int = 0,
    val recycled: Int = 0,
) {
    /** Furniture that found a new home: each completed sale or purchase is one item, each completed exchange two (both items changed hands). */
    val itemsReused: Int get() = itemsSold + itemsBought + exchanges * 2
}
