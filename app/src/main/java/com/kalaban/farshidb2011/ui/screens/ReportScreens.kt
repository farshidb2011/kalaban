package com.kalaban.farshidb2011.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kalaban.farshidb2011.R
import com.kalaban.farshidb2011.core.JalaliDate
import com.kalaban.farshidb2011.core.Money
import com.kalaban.farshidb2011.data.repository.AnalysisFlag
import com.kalaban.farshidb2011.data.repository.AnalysisSort
import com.kalaban.farshidb2011.domain.Permission
import com.kalaban.farshidb2011.ui.AdminViewModel
import com.kalaban.farshidb2011.ui.AnalysisColumn
import com.kalaban.farshidb2011.ui.AppViewModel
import com.kalaban.farshidb2011.ui.DateRange
import com.kalaban.farshidb2011.ui.ReportsViewModel
import com.kalaban.farshidb2011.ui.components.BarChart
import com.kalaban.farshidb2011.ui.components.ChoiceChips
import com.kalaban.farshidb2011.ui.components.DropdownField
import com.kalaban.farshidb2011.ui.components.ExpandableSection
import com.kalaban.farshidb2011.ui.components.InfoBanner
import com.kalaban.farshidb2011.ui.components.JalaliRangePicker
import com.kalaban.farshidb2011.ui.components.KeyValueRow
import com.kalaban.farshidb2011.ui.components.LoadingBox
import com.kalaban.farshidb2011.ui.components.MetricCard
import com.kalaban.farshidb2011.ui.components.RetailCard
import com.kalaban.farshidb2011.ui.components.RetailEmptyState
import com.kalaban.farshidb2011.ui.components.RetailFilterChip
import com.kalaban.farshidb2011.ui.components.RetailOutlinedButton
import com.kalaban.farshidb2011.ui.components.SectionHeader
import com.kalaban.farshidb2011.ui.components.Spacing
import com.kalaban.farshidb2011.ui.components.StatusChip
import com.kalaban.farshidb2011.ui.components.Tone
import com.kalaban.farshidb2011.ui.components.retailSwitchColors
import com.kalaban.farshidb2011.ui.theme.semantic
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AccountBalance

@Composable
fun ReportsHubScreen(
    appVm: AppViewModel,
    reportsVm: ReportsViewModel,
    adminVm: AdminViewModel,
    initialTab: Int,
    onOpenProduct: (Long) -> Unit,
) {
    val showAnalysis = appVm.can(Permission.VIEW_COSTS)
    val tabs = buildList {
        add(R.string.report_sales)
        if (showAnalysis) add(R.string.report_pricing)
        add(R.string.report_price_changes)
    }
    var tab by rememberSaveable { mutableIntStateOf(initialTab.coerceIn(0, tabs.lastIndex)) }
    Column(Modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.primary) {
            tabs.forEachIndexed { i, label ->
                Tab(selected = tab == i, onClick = { tab = i }, text = { Text(stringResource(label), maxLines = 1) })
            }
        }
        when (tabs[tab]) {
            R.string.report_sales -> SalesReportTab(appVm, reportsVm)
            R.string.report_pricing -> PricingAnalysisTab(appVm, reportsVm, adminVm, onOpenProduct)
            else -> PriceChangesTab(appVm, reportsVm, onOpenProduct)
        }
    }
}

@Composable
private fun RangeSelector(range: DateRange, onRange: (DateRange) -> Unit) {
    var custom by remember { mutableStateOf(false) }
    val presets = listOf(
        1 to stringResource(R.string.range_today),
        7 to stringResource(R.string.range_7_days),
        30 to stringResource(R.string.range_30_days),
        90 to stringResource(R.string.range_90_days),
        365 to stringResource(R.string.range_365_days),
    )
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ChoiceChips(
                options = presets.map { it.first to it.second } + (0 to stringResource(R.string.range_this_month)),
                selected = presets.firstOrNull { DateRange.lastDays(it.first) == range }?.first
                    ?: if (DateRange.thisJalaliMonth() == range) 0 else -1,
                onSelect = { days -> onRange(if (days == 0) DateRange.thisJalaliMonth() else DateRange.lastDays(days)) },
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { custom = true }) { Icon(Icons.Outlined.DateRange, stringResource(R.string.custom_range)) }
        }
        Text(
            "${JalaliDate.fromEpochMs(range.fromEpochMs).format()} — ${JalaliDate.fromEpochMs(range.toEpochMs).format()}",
            style = MaterialTheme.typography.labelMedium.copy(textDirection = TextDirection.Ltr),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    JalaliRangePicker(custom, onDismiss = { custom = false }) { from, to ->
        custom = false
        onRange(DateRange.ofJalali(from, to))
    }
}

@Composable
private fun SalesReportTab(appVm: AppViewModel, reportsVm: ReportsViewModel) {
    val range by reportsVm.salesRange.collectAsStateWithLifecycle()
    val summary by reportsVm.sales.collectAsStateWithLifecycle()
    val loading by reportsVm.loading.collectAsStateWithLifecycle()
    val detail by appVm.report.collectAsStateWithLifecycle()
    val settings by appVm.settings.collectAsStateWithLifecycle()
    val fa = settings.languageTag.ifBlank { "fa" }.startsWith("fa")
    val currency = settings.currencyCode
    val headers = listOf(
        stringResource(R.string.date), stringResource(R.string.buyer_name), stringResource(R.string.payment_type),
        stringResource(R.string.total), stringResource(R.string.open_balance), stringResource(R.string.user),
    )
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) reportsVm.exportSales(uri, headers)
    }
    LaunchedEffect(range) {
        reportsVm.refreshSales()
        appVm.loadReport(range.fromEpochMs, range.toEpochMs)
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
        item { RangeSelector(range, reportsVm::setSalesRange) }
        val s = summary
        if (loading && s == null) item { LoadingBox() }
        if (s != null) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                    MetricCard(stringResource(R.string.metric_revenue, s.count), Money.format(s.revenue), Icons.Outlined.Payments, Modifier.weight(1f))
                    if (appVm.can(Permission.VIEW_COSTS)) {
                        MetricCard(
                            stringResource(R.string.net_profit), Money.format(s.profit), Icons.Outlined.TrendingUp, Modifier.weight(1f),
                            tone = if (s.profit.signum() < 0) Tone.DANGER else Tone.SUCCESS,
                        )
                    } else {
                        MetricCard(stringResource(R.string.open_balance), Money.format(s.openBalance), Icons.Outlined.AccountBalance, Modifier.weight(1f))
                    }
                }
            }
            item {
                RetailCard {
                    Column(Modifier.padding(Spacing.l)) {
                        KeyValueRow(stringResource(R.string.cash), Money.format(s.cashRevenue) + " $currency")
                        KeyValueRow(stringResource(R.string.credit), Money.format(s.creditRevenue) + " $currency")
                        KeyValueRow(stringResource(R.string.open_balance), Money.format(s.openBalance) + " $currency")
                        KeyValueRow(stringResource(R.string.tax_collected), Money.format(s.tax) + " $currency")
                        KeyValueRow(stringResource(R.string.discounts_given), Money.format(s.discount) + " $currency")
                    }
                }
            }
            if (s.linesWithoutCost > 0 && appVm.can(Permission.VIEW_COSTS)) {
                item { InfoBanner(stringResource(R.string.profit_partial_note, s.linesWithoutCost), Tone.INFO) }
            }
            if (s.daily.size > 1) {
                item {
                    RetailCard {
                        Column(Modifier.padding(Spacing.l)) {
                            Text(stringResource(R.string.daily_sales_chart), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(Spacing.s))
                            BarChart(
                                values = s.daily.map { it.second.toFloat() },
                                labels = s.daily.map { JalaliDate.fromEpochMs(it.first).format().substring(5) },
                                description = stringResource(R.string.daily_sales_chart),
                            )
                        }
                    }
                }
            }
            if (s.topProducts.isNotEmpty()) {
                item { SectionHeader(stringResource(R.string.top_products)) }
                item {
                    RetailCard {
                        Column(Modifier.padding(Spacing.l)) {
                            s.topProducts.forEach { p ->
                                KeyValueRow(
                                    "${p.title} (${Money.input(p.quantity)})",
                                    Money.format(p.revenue) + (if (appVm.can(Permission.VIEW_COSTS)) p.profit?.let { " · ${Money.format(it)}" } ?: "" else ""),
                                )
                            }
                        }
                    }
                }
            }
        }
        if (detail.groups.isEmpty() && s != null && s.count == 0) {
            item { RetailEmptyState(Icons.Outlined.ReceiptLong, stringResource(R.string.no_sales_in_period)) }
        }
        detail.groups.forEach { group ->
            item(key = "g-${group.categoryId}-${group.categoryNameFa}") {
                SectionHeader(if (fa) group.categoryNameFa else group.categoryNameEn)
                RetailCard {
                    Column(Modifier.padding(Spacing.l)) {
                        group.lines.forEach { line ->
                            KeyValueRow("${line.itemTitle} × ${Money.input(line.quantity)}", Money.format(line.lineTotal))
                        }
                        KeyValueRow(stringResource(R.string.category_subtotal), Money.format(group.subtotal) + " $currency", emphasize = true)
                    }
                }
            }
        }
        if (s != null && s.count > 0 && appVm.can(Permission.EXPORT_DATA)) {
            item {
                RetailOutlinedButton(onClick = { export.launch("kalaban-sales.csv") }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.FileDownload, contentDescription = null)
                    Spacer(Modifier.padding(horizontal = 2.dp))
                    Text(stringResource(R.string.export_csv))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PricingAnalysisTab(appVm: AppViewModel, reportsVm: ReportsViewModel, adminVm: AdminViewModel, onOpenProduct: (Long) -> Unit) {
    val report by reportsVm.analysis.collectAsStateWithLifecycle()
    val filter by reportsVm.analysisFilter.collectAsStateWithLifecycle()
    val loading by reportsVm.loading.collectAsStateWithLifecycle()
    val categories by appVm.categories.collectAsStateWithLifecycle()
    val settings by appVm.settings.collectAsStateWithLifecycle()
    val config by appVm.config.collectAsStateWithLifecycle()
    val fa = settings.languageTag.ifBlank { "fa" }.startsWith("fa")
    val columns = config.reportColumns.split(',').mapNotNull { key -> AnalysisColumn.entries.firstOrNull { it.name == key } }
        .ifEmpty { listOf(AnalysisColumn.COST, AnalysisColumn.PRICE, AnalysisColumn.PROFIT, AnalysisColumn.MARGIN, AnalysisColumn.STOCK) }
    var columnsOpen by remember { mutableStateOf(false) }
    val columnLabels = AnalysisColumn.entries.associateWith { columnLabel(it) }
    val titleHeader = stringResource(R.string.title)
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) reportsVm.exportAnalysis(uri, columns, columnLabels, titleHeader)
    }
    LaunchedEffect(Unit) { reportsVm.refreshAnalysis() }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        item {
            DropdownField(
                label = stringResource(R.string.select_category),
                options = listOf<Pair<Long?, String>>(null to stringResource(R.string.all_categories)) + categories.map { it.id to (if (fa) it.nameFa else it.nameEn) },
                selected = filter.categoryId,
                onSelect = { v -> reportsVm.setAnalysisFilter { it.copy(categoryId = v) } },
            )
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                AnalysisFlag.entries.forEach { flag ->
                    val count = report?.summary?.flagCounts?.get(flag) ?: 0
                    RetailFilterChip(
                        selected = flag in filter.flags,
                        label = "${flagLabel(flag)} ($count)",
                        onClick = { reportsVm.setAnalysisFilter { f -> f.copy(flags = if (flag in f.flags) f.flags - flag else f.flags + flag) } },
                    )
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DropdownField(
                    label = stringResource(R.string.sort),
                    options = AnalysisSort.entries.map { it to sortLabel(it) },
                    selected = filter.sort,
                    onSelect = { v -> reportsVm.setAnalysisFilter { it.copy(sort = v) } },
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { columnsOpen = !columnsOpen }) { Icon(Icons.Outlined.Tune, stringResource(R.string.columns)) }
            }
        }
        if (columnsOpen) {
            item {
                RetailCard {
                    Column(Modifier.padding(Spacing.l)) {
                        Text(stringResource(R.string.columns), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                            AnalysisColumn.entries.forEach { c ->
                                RetailFilterChip(
                                    selected = c in columns,
                                    label = columnLabels.getValue(c),
                                    onClick = {
                                        val next = if (c in columns) columns - c else columns + c
                                        adminVm.updateConfig(Permission.VIEW_REPORTS, message = false) { it.copy(reportColumns = next.joinToString(",") { col -> col.name }) }
                                    },
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.include_archived), modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                            Switch(
                                checked = filter.includeArchived,
                                onCheckedChange = { v -> reportsVm.setAnalysisFilter { it.copy(includeArchived = v) } },
                                colors = retailSwitchColors(),
                            )
                        }
                    }
                }
            }
        }
        if (config.minMarginPct.signum() == 0) {
            item { InfoBanner(stringResource(R.string.min_margin_not_set), Tone.INFO) }
        }
        val r = report
        if (loading && r == null) item { LoadingBox() }
        if (r != null) {
            item {
                RetailCard {
                    Column(Modifier.padding(Spacing.l)) {
                        Text(stringResource(R.string.summary), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        KeyValueRow(stringResource(R.string.products), r.summary.count.toString())
                        KeyValueRow(stringResource(R.string.average_margin), r.summary.averageMarginPct?.let { Money.percent(it) } ?: "—")
                        KeyValueRow(stringResource(R.string.stock_value_cost), Money.format(r.summary.stockValueCost))
                        KeyValueRow(stringResource(R.string.stock_value_retail), Money.format(r.summary.stockValueRetail))
                        KeyValueRow(stringResource(R.string.potential_profit), Money.format(r.summary.potentialProfit), emphasize = true)
                        Spacer(Modifier.height(Spacing.s))
                        Text(stringResource(R.string.margin_distribution), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                        BarChart(
                            values = r.summary.marginBands.map { it.toFloat() },
                            labels = listOf("<0%", "0–10", "10–20", "20–30", "30–50", "≥50%"),
                            color = MaterialTheme.colorScheme.primary,
                            description = stringResource(R.string.margin_distribution),
                        )
                    }
                }
            }
            if (r.rows.isEmpty()) {
                item { RetailEmptyState(Icons.Outlined.QueryStats, stringResource(R.string.no_matching_products)) }
            }
            items(r.rows.take(500), key = { it.item.id }) { row ->
                RetailCard(Modifier.clickable { onOpenProduct(row.item.id) }) {
                    Column(Modifier.padding(Spacing.m)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(row.item.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            row.marginPct?.let {
                                Text(
                                    Money.percent(it),
                                    color = if (it.signum() < 0) MaterialTheme.colorScheme.error else MaterialTheme.semantic.success,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                        columns.forEach { c ->
                            when (c) {
                                AnalysisColumn.CATEGORY -> row.categoryName?.let { KeyValueRow(columnLabels.getValue(c), it) }
                                AnalysisColumn.COST -> KeyValueRow(columnLabels.getValue(c), Money.format(row.fullCost))
                                AnalysisColumn.PRICE -> KeyValueRow(columnLabels.getValue(c), Money.format(row.basePrice))
                                AnalysisColumn.CASH_PRICE -> KeyValueRow(columnLabels.getValue(c), Money.format(row.cashPrice))
                                AnalysisColumn.PROFIT -> KeyValueRow(
                                    columnLabels.getValue(c), Money.format(row.profit),
                                    valueColor = if (row.profit.signum() < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                )
                                AnalysisColumn.MARGIN -> Unit
                                AnalysisColumn.MARKUP -> KeyValueRow(columnLabels.getValue(c), row.markupPct?.let { Money.percent(it) } ?: "—")
                                AnalysisColumn.STOCK -> KeyValueRow(columnLabels.getValue(c), Money.input(row.stock))
                                AnalysisColumn.STOCK_VALUE -> KeyValueRow(columnLabels.getValue(c), Money.format(row.stockValueCost))
                                AnalysisColumn.METHOD -> KeyValueRow(columnLabels.getValue(c), row.formulaName ?: methodLabel(row.method))
                            }
                        }
                        if (row.flags.isNotEmpty()) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                                row.flags.forEach { f -> StatusChip(flagLabel(f), flagTone(f)) }
                            }
                        }
                    }
                }
            }
            if (r.rows.isNotEmpty() && appVm.can(Permission.EXPORT_DATA)) {
                item {
                    RetailOutlinedButton(onClick = { export.launch("kalaban-pricing-analysis.csv") }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.FileDownload, contentDescription = null)
                        Spacer(Modifier.padding(horizontal = 2.dp))
                        Text(stringResource(R.string.export_csv))
                    }
                }
            }
        }
    }
}

@Composable
private fun PriceChangesTab(appVm: AppViewModel, reportsVm: ReportsViewModel, onOpenProduct: (Long) -> Unit) {
    val range by reportsVm.changesRange.collectAsStateWithLifecycle()
    val changes by reportsVm.changes.collectAsStateWithLifecycle()
    val loading by reportsVm.loading.collectAsStateWithLifecycle()
    val headers = listOf(
        stringResource(R.string.date), stringResource(R.string.title), stringResource(R.string.select_category),
        stringResource(R.string.old_price), stringResource(R.string.new_price), stringResource(R.string.difference),
        stringResource(R.string.change_percent), stringResource(R.string.source), stringResource(R.string.user),
    )
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) reportsVm.exportChanges(uri, headers)
    }
    LaunchedEffect(range) { reportsVm.refreshChanges() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        item { RangeSelector(range, reportsVm::setChangesRange) }
        val list = changes
        if (loading && list == null) item { LoadingBox() }
        if (list != null) {
            item {
                val ups = list.count { it.difference.signum() > 0 }
                val downs = list.count { it.difference.signum() < 0 }
                RetailCard {
                    Column(Modifier.padding(Spacing.l)) {
                        KeyValueRow(stringResource(R.string.price_changes_count), list.size.toString(), emphasize = true)
                        KeyValueRow(stringResource(R.string.price_increases), ups.toString(), valueColor = MaterialTheme.semantic.success)
                        KeyValueRow(stringResource(R.string.price_decreases), downs.toString(), valueColor = MaterialTheme.colorScheme.error)
                    }
                }
            }
            if (list.isEmpty()) item { RetailEmptyState(Icons.Outlined.QueryStats, stringResource(R.string.no_price_changes)) }
            items(list.take(500), key = { it.history.id }) { c ->
                ListItem(
                    headlineContent = { Text(c.title, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    supportingContent = {
                        Text(
                            "${JalaliDate.fromEpochMs(c.history.changedAtEpochMs).format()} · ${priceSourceLabel(c.history.source)}" +
                                (c.history.actor?.let { " · ${actorLabel(it)}" } ?: ""),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    trailingContent = {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(Money.format(c.history.newSellPrice), color = MaterialTheme.colorScheme.onSurface)
                            c.changePct?.let {
                                Text(
                                    (if (it.signum() > 0) "+" else "") + Money.percent(it),
                                    style = MaterialTheme.typography.labelSmall.copy(textDirection = TextDirection.Ltr),
                                    color = if (it.signum() < 0) MaterialTheme.colorScheme.error else MaterialTheme.semantic.success,
                                )
                            }
                        }
                    },
                    modifier = Modifier.clickable { onOpenProduct(c.history.itemId) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                )
            }
            if (list.isNotEmpty() && appVm.can(Permission.EXPORT_DATA)) {
                item {
                    RetailOutlinedButton(onClick = { export.launch("kalaban-price-changes.csv") }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.FileDownload, contentDescription = null)
                        Spacer(Modifier.padding(horizontal = 2.dp))
                        Text(stringResource(R.string.export_csv))
                    }
                }
            }
        }
    }
}

@Composable
fun flagLabel(flag: AnalysisFlag): String = stringResource(
    when (flag) {
        AnalysisFlag.NEGATIVE_PROFIT -> R.string.flag_negative_profit
        AnalysisFlag.LOW_MARGIN -> R.string.flag_low_margin
        AnalysisFlag.MISSING_COST -> R.string.flag_missing_cost
        AnalysisFlag.MISSING_PRICE -> R.string.flag_missing_price
        AnalysisFlag.OUTDATED -> R.string.flag_outdated
        AnalysisFlag.OUT_OF_BOUNDS -> R.string.flag_out_of_bounds
        AnalysisFlag.FORMULA_ERROR -> R.string.flag_formula_error
        AnalysisFlag.LOW_STOCK -> R.string.low_stock
    },
)

private fun flagTone(flag: AnalysisFlag): Tone = when (flag) {
    AnalysisFlag.NEGATIVE_PROFIT, AnalysisFlag.FORMULA_ERROR, AnalysisFlag.MISSING_PRICE -> Tone.DANGER
    AnalysisFlag.OUTDATED -> Tone.INFO
    else -> Tone.WARNING
}

@Composable
private fun sortLabel(sort: AnalysisSort): String = stringResource(
    when (sort) {
        AnalysisSort.MARGIN_ASC -> R.string.sort_margin_asc
        AnalysisSort.MARGIN_DESC -> R.string.sort_margin_desc
        AnalysisSort.PROFIT_DESC -> R.string.sort_profit_desc
        AnalysisSort.PRICE_DESC -> R.string.sort_price_desc
        AnalysisSort.STOCK_VALUE_DESC -> R.string.sort_stock_value
        AnalysisSort.TITLE -> R.string.sort_title
    },
)

@Composable
private fun columnLabel(c: AnalysisColumn): String = stringResource(
    when (c) {
        AnalysisColumn.CATEGORY -> R.string.select_category
        AnalysisColumn.COST -> R.string.full_cost
        AnalysisColumn.PRICE -> R.string.sell_price_base
        AnalysisColumn.CASH_PRICE -> R.string.cash_price
        AnalysisColumn.PROFIT -> R.string.net_profit
        AnalysisColumn.MARGIN -> R.string.margin
        AnalysisColumn.MARKUP -> R.string.markup
        AnalysisColumn.STOCK -> R.string.stock
        AnalysisColumn.STOCK_VALUE -> R.string.stock_value_cost
        AnalysisColumn.METHOD -> R.string.pricing_method
    },
)
