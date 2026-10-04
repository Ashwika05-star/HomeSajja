package com.homesajja.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homesajja.app.data.model.RatingSummary
import com.homesajja.app.repository.AuthRepository
import com.homesajja.app.repository.EarnedBatch
import com.homesajja.app.repository.StatsRepository
import com.homesajja.app.repository.VendorRepository
import com.homesajja.app.report.MonthlyReport
import com.homesajja.app.report.ReportExporter
import com.homesajja.app.report.ReportFormat
import com.homesajja.app.report.buildMonthlyReport
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

/** One month the vendor can pick in the report screen. */
data class MonthOption(val start: Long, val label: String)

/** A finished export, ready for the screen to open in the share sheet. */
data class ShareRequest(val file: File, val format: ReportFormat, val subject: String)

sealed interface ReportUiState {
    data object Loading : ReportUiState
    data class Error(val message: String) : ReportUiState

    /** [report] is for the [selected] month; [isMonthLoading] while another month is being read, [isExporting] while a file is being made. */
    data class Content(
        val months: List<MonthOption>,
        val selected: Long,
        val report: MonthlyReport,
        val isMonthLoading: Boolean = false,
        val isExporting: Boolean = false,
    ) : ReportUiState
}

/**
 * The monthly report: pick one of the last six months, see its summary, export it as PDF or CSV and share it. Each month is read once with
 * a `payment.confirmedAt` range (so only that month's payments are read) and remembered while the screen is open.
 */
class VendorReportViewModel(
    private val authRepository: AuthRepository,
    private val vendorRepository: VendorRepository,
    private val statsRepository: StatsRepository,
    private val exporter: ReportExporter,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReportUiState>(ReportUiState.Loading)
    val uiState: StateFlow<ReportUiState> = _uiState

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages

    private val _shares = MutableSharedFlow<ShareRequest>(extraBufferCapacity = 1)
    val shares: SharedFlow<ShareRequest> = _shares

    private val monthCache = mutableMapOf<Long, EarnedBatch>()
    private var businessName = ""
    private var city = ""
    private var tasks = VendorTaskCounts()
    private var rating = RatingSummary()
    private var months: List<MonthOption> = emptyList()

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        val uid = authRepository.currentUserId
        if (uid == null) {
            _uiState.value = ReportUiState.Error("Please log in to see your report.")
            return
        }
        _uiState.value = ReportUiState.Loading
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val starts = lastMonthStarts(now, 6)
                months = starts.reversed().map { MonthOption(it, monthLabel(it, full = true)) }
                coroutineScope {
                    val vendor = async { vendorRepository.getVendorProfile(uid) }
                    val counts = async { statsRepository.vendorTaskCounts(uid) }
                    val stars = async { statsRepository.rating(uid) }
                    val profile = vendor.await() ?: throw IllegalStateException("No vendor profile")
                    businessName = profile.businessName.ifBlank { profile.name }
                    city = profile.city
                    tasks = counts.await()
                    rating = stars.await()
                }
                showMonth(uid, starts.last())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = ReportUiState.Error(mapError(e, "Couldn't load your report."))
            }
        }
    }

    /** Switches to another month, reading its payments if they haven't been read yet. */
    fun selectMonth(start: Long) {
        val current = _uiState.value as? ReportUiState.Content ?: return
        if (start == current.selected || current.isMonthLoading || current.isExporting) return
        val uid = authRepository.currentUserId ?: return
        _uiState.value = current.copy(isMonthLoading = true)
        viewModelScope.launch {
            try {
                showMonth(uid, start)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _messages.tryEmit(mapError(e, "Couldn't load that month."))
                _uiState.value = current.copy(isMonthLoading = false)
            }
        }
    }

    private suspend fun showMonth(uid: String, start: Long) {
        val batch = monthCache[start] ?: statsRepository.vendorEarned(uid, from = start, until = shiftMonth(start, 1)).also { monthCache[start] = it }
        _uiState.value = ReportUiState.Content(
            months = months,
            selected = start,
            report = buildMonthlyReport(businessName, city, start, batch.items, tasks, rating, batch.isPartial),
        )
    }

    /** Makes the PDF or CSV for the selected month and hands it to the screen to share. */
    fun export(format: ReportFormat) {
        val current = _uiState.value as? ReportUiState.Content ?: return
        if (current.isExporting || current.isMonthLoading) return
        _uiState.value = current.copy(isExporting = true)
        viewModelScope.launch {
            try {
                val file = exporter.export(current.report, format)
                _shares.tryEmit(ShareRequest(file, format, "HomeSajja report: ${current.report.monthLabel}"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _messages.tryEmit("Couldn't make the ${format.label}. Please try again.")
            } finally {
                (_uiState.value as? ReportUiState.Content)?.let { _uiState.value = it.copy(isExporting = false) }
            }
        }
    }
}
