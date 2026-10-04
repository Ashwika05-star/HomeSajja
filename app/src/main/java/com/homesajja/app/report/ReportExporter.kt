package com.homesajja.app.report

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The two formats a report can be exported in. */
enum class ReportFormat(val extension: String, val mimeType: String, val label: String) {
    PDF("pdf", "application/pdf", "PDF"),
    CSV("csv", "text/csv", "CSV"),
}

/**
 * Writes a [MonthlyReport] to a temporary file on the phone and builds the Android share-sheet intent for it. Nothing is uploaded: the person
 * chooses where it goes (Drive, WhatsApp, email, Files...) in the system share sheet. The file lives in the app's cache.
 */
class ReportExporter(private val context: Context) {

    private val folder: File get() = File(context.cacheDir, "reports").apply { mkdirs() }

    /** Generates the file on a background thread and returns it. */
    suspend fun export(report: MonthlyReport, format: ReportFormat): File = withContext(Dispatchers.IO) {
        val file = File(folder, reportFileName(report.monthStart, format.extension))
        when (format) {
            ReportFormat.PDF -> file.outputStream().use { PdfReport.write(report, it) }
            // A byte-order mark makes Excel read the rupee and other non-English characters correctly.
            ReportFormat.CSV -> file.writeText("﻿" + CsvReport.build(report), Charsets.UTF_8)
        }
        file
    }

    /** The system share sheet for [file]. */
    fun shareIntent(file: File, format: ReportFormat, subject: String): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = format.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, "Share report").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}
