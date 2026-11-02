package com.kalaban.farshidb2011.ui

import android.app.Application
import android.net.Uri
import com.kalaban.farshidb2011.KalabanApp
import com.kalaban.farshidb2011.R
import com.kalaban.farshidb2011.core.JalaliDate
import com.kalaban.farshidb2011.core.Money
import com.kalaban.farshidb2011.core.csv.Csv
import com.kalaban.farshidb2011.data.repository.AnalysisFilter
import com.kalaban.farshidb2011.data.repository.AnalysisReport
import com.kalaban.farshidb2011.data.repository.AuditAction
import com.kalaban.farshidb2011.data.repository.AuditEntity
import com.kalaban.farshidb2011.data.repository.DashboardSnapshot
import com.kalaban.farshidb2011.data.repository.PriceChangeRow
import com.kalaban.farshidb2011.data.repository.ReportRepository
import com.kalaban.farshidb2011.data.repository.SalesSummary
import com.kalaban.farshidb2011.domain.Permission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

/** Columns an administrator can show or hide in the pricing analysis (persisted in config). */
enum class AnalysisColumn { CATEGORY, COST, PRICE, CASH_PRICE, PROFIT, MARGIN, MARKUP, STOCK, STOCK_VALUE, METHOD }

data class DateRange(val fromEpochMs: Long, val toEpochMs: Long) {
    companion object {
        private const val DAY = 24L * 60 * 60 * 1000

        fun lastDays(days: Int): DateRange {
            val today = ReportRepository.startOfDay(System.currentTimeMillis())
            return DateRange(today - (days - 1) * DAY, today + DAY - 1)
        }

        fun thisJalaliMonth(): DateRange {
            val now = System.currentTimeMillis()
            return DateRange(ReportRepository.startOfJalaliMonth(now), ReportRepository.startOfDay(now) + DAY - 1)
        }

        fun ofJalali(from: JalaliDate.Date, to: JalaliDate.Date): DateRange = DateRange(
            ReportRepository.startOfDay(from.toGregorianEpochMs()),
            ReportRepository.startOfDay(to.toGregorianEpochMs()) + DAY - 1,
        )
    }
}

class ReportsViewModel(application: Application) : GuardedViewModel(application) {
    private val reports get() = app.reports

    private val _dashboard = MutableStateFlow(DashboardSnapshot())
    val dashboard: StateFlow<DashboardSnapshot> = _dashboard.asStateFlow()

    private val _analysisFilter = MutableStateFlow(AnalysisFilter())
    val analysisFilter: StateFlow<AnalysisFilter> = _analysisFilter.asStateFlow()
    private val _analysis = MutableStateFlow<AnalysisReport?>(null)
    val analysis: StateFlow<AnalysisReport?> = _analysis.asStateFlow()

    private val _salesRange = MutableStateFlow(DateRange.lastDays(30))
    val salesRange: StateFlow<DateRange> = _salesRange.asStateFlow()
    private val _sales = MutableStateFlow<SalesSummary?>(null)
    val sales: StateFlow<SalesSummary?> = _sales.asStateFlow()

    private val _changesRange = MutableStateFlow(DateRange.lastDays(30))
    val changesRange: StateFlow<DateRange> = _changesRange.asStateFlow()
    private val _changes = MutableStateFlow<List<PriceChangeRow>?>(null)
    val changes: StateFlow<List<PriceChangeRow>?> = _changes.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private suspend fun isFa(): Boolean = app.repository.getSettings().languageTag.ifBlank { "fa" }.startsWith("fa")

    private suspend fun <T> loading(block: suspend () -> T): T {
        _loading.value = true
        try {
            return withContext(Dispatchers.Default) { block() }
        } finally {
            _loading.value = false
        }
    }

    fun refreshDashboard() = launchSafely {
        _dashboard.value = withContext(Dispatchers.Default) { reports.dashboard() }
    }

    fun setAnalysisFilter(transform: (AnalysisFilter) -> AnalysisFilter) {
        _analysisFilter.update(transform)
        refreshAnalysis()
    }

    fun refreshAnalysis() = launchSafely {
        if (!can(Permission.VIEW_REPORTS)) return@launchSafely
        val fa = isFa()
        _analysis.value = loading { reports.pricingAnalysis(_analysisFilter.value, fa) }
    }

    fun setSalesRange(range: DateRange) {
        _salesRange.value = range
        refreshSales()
    }

    fun refreshSales() = launchSafely {
        if (!can(Permission.VIEW_REPORTS)) return@launchSafely
        val r = _salesRange.value
        _sales.value = loading { reports.salesSummary(r.fromEpochMs, r.toEpochMs) }
    }

    fun setChangesRange(range: DateRange) {
        _changesRange.value = range
        refreshChanges()
    }

    fun refreshChanges() = launchSafely {
        if (!can(Permission.VIEW_REPORTS)) return@launchSafely
        val r = _changesRange.value
        val fa = isFa()
        _changes.value = loading { reports.priceChanges(r.fromEpochMs, r.toEpochMs, null, fa) }
    }

    private suspend fun write(uri: Uri, rows: List<List<String>>, label: String) {
        withContext(Dispatchers.IO) {
            app.importExport.writeText(uri, Csv.write(rows, app.config.current().csvDelimiter))
        }
        app.audit.log(actor, AuditAction.DATA_EXPORT, AuditEntity.DATA, summary = "$label: ${rows.size - 1}")
        UiMessages.post(R.string.export_done, rows.size - 1)
    }

    /** Exports the visible analysis rows with the columns currently shown. */
    fun exportAnalysis(uri: Uri, columns: List<AnalysisColumn>, headers: Map<AnalysisColumn, String>, titleHeader: String) = launchSafely {
        if (!allowed(Permission.EXPORT_DATA)) return@launchSafely
        val report = _analysis.value ?: return@launchSafely
        val showCosts = can(Permission.VIEW_COSTS)
        val visible = columns.filter { showCosts || it !in setOf(AnalysisColumn.COST, AnalysisColumn.PROFIT, AnalysisColumn.MARGIN, AnalysisColumn.MARKUP, AnalysisColumn.STOCK_VALUE) }
        val rows = report.rows.map { r ->
            listOf(Csv.text(r.item.title)) + visible.map { c ->
                when (c) {
                    AnalysisColumn.CATEGORY -> Csv.text(r.categoryName)
                    AnalysisColumn.COST -> Money.input(r.fullCost)
                    AnalysisColumn.PRICE -> Money.input(r.basePrice)
                    AnalysisColumn.CASH_PRICE -> Money.input(r.cashPrice)
                    AnalysisColumn.PROFIT -> Money.input(r.profit)
                    AnalysisColumn.MARGIN -> r.marginPct?.let { Money.input(it) }.orEmpty()
                    AnalysisColumn.MARKUP -> r.markupPct?.let { Money.input(it) }.orEmpty()
                    AnalysisColumn.STOCK -> Money.input(r.stock)
                    AnalysisColumn.STOCK_VALUE -> Money.input(r.stockValueCost)
                    AnalysisColumn.METHOD -> r.formulaName ?: r.method.name
                }
            }
        }
        write(uri, listOf(listOf(titleHeader) + visible.map { headers[it].orEmpty() }) + rows, "pricing_analysis")
    }

    fun exportSales(uri: Uri, headers: List<String>) = launchSafely {
        if (!allowed(Permission.EXPORT_DATA)) return@launchSafely
        val r = _salesRange.value
        val sales = withContext(Dispatchers.IO) { app.database.saleDao().getInRange(r.fromEpochMs, r.toEpochMs) }
        val rows = sales.map {
            listOf(it.jalaliDate, Csv.text(it.buyerName), it.paymentType.name, Money.input(it.totalAmount), Money.input(it.openBalance), Csv.text(it.createdBy))
        }
        write(uri, listOf(headers) + rows, "sales")
    }

    fun exportChanges(uri: Uri, headers: List<String>) = launchSafely {
        if (!allowed(Permission.EXPORT_DATA)) return@launchSafely
        val rows = _changes.value.orEmpty().map { c ->
            listOf(
                JalaliDate.fromEpochMs(c.history.changedAtEpochMs).format(),
                Csv.text(c.title),
                Csv.text(c.categoryName),
                c.history.oldSellPrice?.let(Money::input).orEmpty(),
                Money.input(c.history.newSellPrice),
                Money.input(c.difference),
                c.changePct?.let { Money.input(it) }.orEmpty(),
                c.history.source,
                Csv.text(c.history.actor),
            )
        }
        write(uri, listOf(headers) + rows, "price_changes")
    }

    companion object {
        fun factory() = appViewModelFactory { ReportsViewModel(KalabanApp.instance) }
    }
}
