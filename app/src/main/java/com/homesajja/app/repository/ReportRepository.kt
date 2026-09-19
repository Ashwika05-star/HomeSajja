package com.homesajja.app.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.homesajja.app.data.model.Report
import kotlinx.coroutines.tasks.await

private const val COLLECTION = "reports"

/** Reports at `reports/{reporterId_targetType_targetId}`. Write-only from the app: nobody reads reports back. */
class ReportRepository(firestore: FirebaseFirestore) {

    private val reports = firestore.collection(COLLECTION)

    /** Files the report. Reporting the same thing twice just replaces the earlier report. */
    suspend fun submit(report: Report) {
        val id = "${report.reporterId}_${report.targetType.name}_${report.targetId}"
        reports.document(id).set(report.copy(id = id)).await()
    }
}
