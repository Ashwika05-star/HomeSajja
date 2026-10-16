package com.homesajja.app.ui.screens.vendor

import androidx.compose.foundation.BorderStroke
import android.content.ActivityNotFoundException
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.homesajja.app.di.LocalAppContainer
import com.homesajja.app.di.ViewModelFactory
import com.homesajja.app.report.MonthlyReport
import com.homesajja.app.report.ReportFormat
import com.homesajja.app.ui.components.AppTopBar
import com.homesajja.app.ui.components.CategoryChip
import com.homesajja.app.ui.components.ErrorState
import com.homesajja.app.ui.components.LoadingState
import com.homesajja.app.ui.components.OutlinedButton
import com.homesajja.app.ui.components.PrimaryButton
import com.homesajja.app.ui.i18n.VendorStrings
import com.homesajja.app.ui.i18n.isAppHindi
import com.homesajja.app.ui.util.formatPrice
import com.homesajja.app.viewmodel.EarnedItem
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.homesajja.app.viewmodel.ExpenditureItem
import com.homesajja.app.viewmodel.ReportUiState
import com.homesajja.app.viewmodel.VendorReportViewModel
import java.text.SimpleDateFormat
import java.util.Locale

/** The monthly report: pick a month, read the summary, then export it as PDF or CSV and share it from the Android share sheet. */
@Composable
fun VendorReportScreen(onBackClick: () -> Unit) {
    val isHindi = isAppHindi
    val container = LocalAppContainer.current
    val viewModel: VendorReportViewModel = viewModel(factory = ViewModelFactory(container))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }
    LaunchedEffect(viewModel) {
        viewModel.shares.collect { request ->
            try {
                context.startActivity(container.reportExporter.shareIntent(request.file, request.format, request.subject))
            } catch (e: ActivityNotFoundException) {
                snackbarHostState.showSnackbar("No app on this phone can open the share sheet for this file.")
            }
        }
    }

    Scaffold(
        topBar = { AppTopBar(title = VendorStrings.monthlyReport(isHindi), onBackClick = onBackClick) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            (state as? ReportUiState.Content)?.let { content ->
                Surface(color = MaterialTheme.colorScheme.background) {
                    Column {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            val busy = content.isExporting || content.isMonthLoading
                            PrimaryButton(
                                text = if (content.isExporting) VendorStrings.preparing(isHindi) else VendorStrings.exportPdf(isHindi),
                                onClick = { viewModel.export(ReportFormat.PDF) },
                                enabled = !busy,
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedButton(text = VendorStrings.exportCsv(isHindi), onClick = { viewModel.export(ReportFormat.CSV) }, enabled = !busy, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val current = state) {
                ReportUiState.Loading -> LoadingState()
                is ReportUiState.Error -> ErrorState(message = current.message, onRetry = viewModel::retry)
                is ReportUiState.Content -> ReportContent(current, onSelectMonth = viewModel::selectMonth)
            }
        }
    }
}

@Composable
private fun ReportContent(content: ReportUiState.Content, onSelectMonth: (Long) -> Unit) {
    val isHindi = isAppHindi
    val report = content.report
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(content.months, key = { it.start }) { month ->
                    CategoryChip(label = month.label, selected = month.start == content.selected, onClick = { onSelectMonth(month.start) })
                }
            }
        }
        item { SummaryCard(report, loading = content.isMonthLoading) }
        item {
            Text(
                VendorStrings.reportNotice(isHindi),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (report.payments.isEmpty() && report.expenditures.isEmpty()) {
            item {
                Text(
                    VendorStrings.noActivityInMonth(report.monthLabel, isHindi),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        } else {
            if (report.payments.isNotEmpty()) {
                item {
                    Text(
                        VendorStrings.itemsSoldTitle(isHindi),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                items(report.payments, key = { "payment_${it.id}" }) { PaymentRow(it) }
            }
            if (report.expenditures.isNotEmpty()) {
                item {
                    Text(
                        VendorStrings.itemsBoughtTitle(isHindi),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                items(report.expenditures, key = { "exp_${it.id}" }) { ExpenditureRow(it) }
            }
        }
    }
}

@Composable
private fun SummaryCard(report: MonthlyReport, loading: Boolean) {
    val isHindi = isAppHindi
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                report.monthLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )

            // 3 Numbers: Sales, Expenditure, Profit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MetricTile(
                    title = VendorStrings.sales(isHindi),
                    amount = if (loading) "…" else formatPrice(report.totalEarned) + if (report.isPartial) "+" else "",
                    valueColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                MetricTile(
                    title = VendorStrings.expenditure(isHindi),
                    amount = if (loading) "…" else formatPrice(report.totalExpenditure),
                    valueColor = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f),
                )
                MetricTile(
                    title = VendorStrings.profit(isHindi),
                    amount = if (loading) "…" else formatPrice(report.profit),
                    valueColor = if (report.profit >= 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f),
                )
            }

            if (report.byKind.any { it.count > 0 }) {
                HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    report.byKind.filter { it.count > 0 }.forEach {
                        Text(
                            "${it.kind.plural}: ${it.count} · ${formatPrice(it.amount)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricTile(
    title: String,
    amount: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = amount,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PaymentRow(item: EarnedItem) {
    val date = remember { SimpleDateFormat("d MMM", Locale.ENGLISH) }
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.title, style = MaterialTheme.typography.titleSmall)
                Text(
                    "${item.kind.label} · ${item.counterpart} · ${date.format(item.confirmedAt)}${item.method?.let { " · ${it.displayName}" }.orEmpty()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(formatPrice(item.amount), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ExpenditureRow(item: ExpenditureItem) {
    val date = remember { SimpleDateFormat("d MMM", Locale.ENGLISH) }
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.title, style = MaterialTheme.typography.titleSmall)
                Text(
                    "${item.counterpart} · ${date.format(item.confirmedAt)}${item.method?.let { " · ${it.displayName}" }.orEmpty()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text("-${formatPrice(item.amount)}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error)
        }
    }
}
