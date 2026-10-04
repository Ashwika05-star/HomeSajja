package com.homesajja.app.report

import com.homesajja.app.data.model.PaymentMethod
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * The monthly report as CSV text, written by hand: a few summary lines, then a table of the payments received. Plain Kotlin, so it is unit-tested;
 * amounts are whole rupees without a currency sign so spreadsheets read them as numbers.
 */
object CsvReport {

    /** Quotes a text field when it holds a comma, quote or line break, and defuses spreadsheet formulas ("=1+1" would otherwise run). */
    fun field(value: String): String {
        val safe = if (value.isNotEmpty() && value[0] in FORMULA_STARTS) "'$value" else value
        return if (safe.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + safe.replace("\"", "\"\"") + "\"" else safe
    }

    fun build(report: MonthlyReport, zone: TimeZone = TimeZone.getDefault()): String {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).apply { timeZone = zone }
        fun row(vararg cells: Any) = cells.joinToString(",") { if (it is String) field(it) else it.toString() }
        return buildList {
            add(row("HomeSajja monthly report"))
            add(row("Business", report.businessName))
            add(row("City", report.city))
            add(row("Month", report.monthLabel))
            add(row("Generated", date.format(report.generatedAt)))
            add(row("Only payments confirmed as received are counted"))
            if (report.isPartial) add(row("Note", "This month has more payments than were read, so only the newest are listed"))
            add("")
            add(row("Summary"))
            add(row("Total earned (INR)", report.totalEarned))
            add(row("Type", "Payments", "Amount (INR)"))
            report.byKind.forEach { add(row(it.kind.plural, it.count, it.amount)) }
            add("")
            add(row("Payments received"))
            add(row("Date", "Type", "Item", "Customer", "Method", "Amount (INR)"))
            report.payments.forEach {
                add(row(date.format(it.confirmedAt), it.kind.label, it.title, it.counterpart, methodText(it.method), it.amount))
            }
            add("")
            add(row("Status today"))
            add(row("Type", "Completed", "Pending"))
            add(row("Sales", report.tasks.sales.completed, report.tasks.sales.pending))
            add(row("Repairs", report.tasks.repairs.completed, report.tasks.repairs.pending))
            add(row("Exchanges", report.tasks.exchanges.completed, report.tasks.exchanges.pending))
            add(row("Recycling", report.tasks.recycling.completed, report.tasks.recycling.pending))
            add(row("Average rating", if (report.rating.hasRatings) String.format(Locale.US, "%.1f", report.rating.average) else "No reviews yet", report.rating.count))
        }.joinToString("\r\n", postfix = "\r\n")
    }

    private fun methodText(method: PaymentMethod?) = method?.displayName.orEmpty()

    private val FORMULA_STARTS = charArrayOf('=', '+', '-', '@', '\t', '\r')
}
