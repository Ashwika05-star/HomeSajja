package com.homesajja.app.report

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.homesajja.app.data.model.PaymentMethod
import com.homesajja.app.ui.util.formatPrice
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * The monthly report as a PDF, drawn with Android's own [PdfDocument] (no library): an A4 page with a title, the summary and a table of the
 * payments received, continuing onto more pages when there are many.
 */
object PdfReport {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f
    private const val BOTTOM = PAGE_HEIGHT - 60f
    private val BRAND = Color.rgb(0x6B, 0x1E, 0x2C)

    private val title = paint(22f, bold = true, color = BRAND)
    private val heading = paint(14f, bold = true)
    private val body = paint(11f)
    private val small = paint(9f, color = Color.DKGRAY)
    private val tableHead = paint(10f, bold = true)
    private val big = paint(26f, bold = true, color = BRAND)
    private val metricValue = paint(18f, bold = true, color = BRAND)
    private val rule = Paint().apply { color = Color.LTGRAY; strokeWidth = 0.8f }

    private fun paint(size: Float, bold: Boolean = false, color: Int = Color.BLACK) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }

    /** Columns of the tables: x position and the width the text may use. */
    private data class Column(val label: String, val x: Float, val width: Float, val alignRight: Boolean = false)

    private val soldColumns = listOf(
        Column("Date", MARGIN, 62f),
        Column("Type", MARGIN + 66f, 58f),
        Column("Item", MARGIN + 128f, 150f),
        Column("Customer", MARGIN + 282f, 110f),
        Column("Method", MARGIN + 396f, 44f),
        Column("Amount", MARGIN + 444f, 71f, alignRight = true),
    )

    private val boughtColumns = listOf(
        Column("Date", MARGIN, 70f),
        Column("Item", MARGIN + 74f, 180f),
        Column("Seller", MARGIN + 258f, 130f),
        Column("Method", MARGIN + 392f, 50f),
        Column("Amount", MARGIN + 444f, 71f, alignRight = true),
    )

    fun write(report: MonthlyReport, output: OutputStream, zone: TimeZone = TimeZone.getDefault()) {
        val date = SimpleDateFormat("d MMM yyyy", Locale.ENGLISH).apply { timeZone = zone }
        val document = PdfDocument()
        var pageNumber = 0
        var page: PdfDocument.Page? = null
        var y = 0f

        fun startPage() {
            page?.let { finish(document, it, pageNumber) }
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            y = MARGIN + 6f
        }

        fun soldTableHeader() {
            val canvas = page!!.canvas
            soldColumns.forEach { drawCell(canvas, it.label, it, y, tableHead) }
            y += 6f
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, rule)
            y += 14f
        }

        fun boughtTableHeader() {
            val canvas = page!!.canvas
            boughtColumns.forEach { drawCell(canvas, it.label, it, y, tableHead) }
            y += 6f
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, rule)
            y += 14f
        }

        startPage()
        page!!.canvas.apply {
            drawText("HomeSajja", MARGIN, y + 16f, title)
            y += 26f
            drawText("Monthly report: ${report.monthLabel}", MARGIN, y + 10f, heading)
            y += 22f
            drawText("${report.businessName} · ${report.city}", MARGIN, y + 4f, body)
            y += 14f
            drawText("Generated ${date.format(report.generatedAt)}. Only confirmed payments and purchases are counted.", MARGIN, y + 4f, small)
            y += 28f

            // 3 Key Financial Metrics: Sales, Expenditure, Profit
            val colW = (PAGE_WIDTH - MARGIN * 2) / 3f
            drawText("Sales", MARGIN, y, small)
            drawText("Expenditure", MARGIN + colW, y, small)
            drawText("Profit", MARGIN + colW * 2, y, small)
            y += 20f

            drawText(formatPrice(report.totalEarned), MARGIN, y, metricValue)
            drawText(formatPrice(report.totalExpenditure), MARGIN + colW, y, metricValue)
            drawText(formatPrice(report.profit), MARGIN + colW * 2, y, metricValue)
            y += 24f

            report.byKind.forEach {
                drawText("${it.kind.plural}: ${it.count} payment${if (it.count == 1) "" else "s"}, ${formatPrice(it.amount)}", MARGIN, y, body)
                y += 15f
            }
            y += 10f
            drawText(
                "Done so far: ${report.tasks.sales.completed} sales, ${report.tasks.repairs.completed} repairs, ${report.tasks.exchanges.completed} exchanges, " +
                    "${report.tasks.recycling.completed} recycling jobs. Waiting: ${report.tasks.pending}.",
                MARGIN, y, small,
            )
            y += 13f
            drawText(
                if (report.rating.hasRatings) "Average rating ${String.format(Locale.US, "%.1f", report.rating.average)} from ${report.rating.count} reviews" else "No reviews yet",
                MARGIN, y, small,
            )
            y += 26f
            drawText("Items sold", MARGIN, y, heading)
            y += 20f
        }
        if (report.isPartial) {
            page!!.canvas.drawText("This month has more records than were read, so only the newest are listed.", MARGIN, y, small)
            y += 16f
        }

        soldTableHeader()
        if (report.payments.isEmpty()) {
            page!!.canvas.drawText("No items were sold in this month.", MARGIN, y, body)
            y += 18f
        } else {
            report.payments.forEach { item ->
                if (y > BOTTOM) {
                    startPage()
                    soldTableHeader()
                }
                val canvas = page!!.canvas
                drawCell(canvas, date.format(item.confirmedAt), soldColumns[0], y, body)
                drawCell(canvas, item.kind.label, soldColumns[1], y, body)
                drawCell(canvas, item.title, soldColumns[2], y, body)
                drawCell(canvas, item.counterpart, soldColumns[3], y, body)
                drawCell(canvas, methodText(item.method), soldColumns[4], y, body)
                drawCell(canvas, formatPrice(item.amount), soldColumns[5], y, body)
                y += 16f
            }
        }

        y += 16f
        if (y > BOTTOM - 60f) {
            startPage()
        }
        page!!.canvas.drawText("Items bought", MARGIN, y, heading)
        y += 20f
        boughtTableHeader()
        if (report.expenditures.isEmpty()) {
            page!!.canvas.drawText("No items were bought in this month.", MARGIN, y, body)
            y += 18f
        } else {
            report.expenditures.forEach { item ->
                if (y > BOTTOM) {
                    startPage()
                    boughtTableHeader()
                }
                val canvas = page!!.canvas
                drawCell(canvas, date.format(item.confirmedAt), boughtColumns[0], y, body)
                drawCell(canvas, item.title, boughtColumns[1], y, body)
                drawCell(canvas, item.counterpart, boughtColumns[2], y, body)
                drawCell(canvas, methodText(item.method), boughtColumns[3], y, body)
                drawCell(canvas, formatPrice(item.amount), boughtColumns[4], y, body)
                y += 16f
            }
        }

        page?.let { finish(document, it, pageNumber) }
        document.writeTo(output)
        document.close()
    }

    private fun methodText(method: PaymentMethod?) = method?.displayName.orEmpty()

    private fun finish(document: PdfDocument, page: PdfDocument.Page, number: Int) {
        page.canvas.drawText("Page $number", PAGE_WIDTH - MARGIN - 40f, PAGE_HEIGHT - 28f, small)
        document.finishPage(page)
    }

    /** Draws [text] in a table cell, cut with "…" if it is wider than the column. */
    private fun drawCell(canvas: android.graphics.Canvas, text: String, column: Column, y: Float, paint: Paint) {
        val fitted = fit(text, column.width, paint)
        val x = if (column.alignRight) column.x + column.width - paint.measureText(fitted) else column.x
        canvas.drawText(fitted, x, y, paint)
    }

    private fun fit(text: String, width: Float, paint: Paint): String {
        if (paint.measureText(text) <= width) return text
        val count = paint.breakText(text, true, width - paint.measureText("…"), null)
        return text.take(count).trimEnd() + "…"
    }
}
