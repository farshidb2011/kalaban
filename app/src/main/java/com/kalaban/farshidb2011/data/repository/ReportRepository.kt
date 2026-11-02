package com.kalaban.farshidb2011.data.repository

import com.kalaban.farshidb2011.core.Money
import com.kalaban.farshidb2011.core.pricing.PriceMethod
import com.kalaban.farshidb2011.core.pricing.PricingWarning
import com.kalaban.farshidb2011.data.local.KalabanDatabase
import com.kalaban.farshidb2011.data.local.entity.BatchStatus
import com.kalaban.farshidb2011.data.local.entity.CategoryEntity
import com.kalaban.farshidb2011.data.local.entity.ItemEntity
import com.kalaban.farshidb2011.data.local.entity.ItemStatus
import com.kalaban.farshidb2011.data.local.entity.PaymentType
import com.kalaban.farshidb2011.data.local.entity.PriceBatchEntity
import com.kalaban.farshidb2011.data.local.entity.PriceHistoryEntity
import java.math.BigDecimal
import java.util.Calendar

enum class AnalysisFlag { NEGATIVE_PROFIT, LOW_MARGIN, MISSING_COST, MISSING_PRICE, OUTDATED, OUT_OF_BOUNDS, FORMULA_ERROR, LOW_STOCK }

enum class AnalysisSort { MARGIN_ASC, MARGIN_DESC, PROFIT_DESC, PRICE_DESC, STOCK_VALUE_DESC, TITLE }

data class AnalysisFilter(
    val categoryId: Long? = null,
    val includeArchived: Boolean = false,
    val query: String = "",
    val minMarginPct: BigDecimal? = null,
    val maxMarginPct: BigDecimal? = null,
    val flags: Set<AnalysisFlag> = emptySet(),
    val sort: AnalysisSort = AnalysisSort.MARGIN_ASC,
)

data class AnalysisRow(
    val item: ItemEntity,
    val categoryName: String?,
    val fullCost: BigDecimal,
    val basePrice: BigDecimal,
    val cashPrice: BigDecimal,
    val revenue: BigDecimal,
    val profit: BigDecimal,
    val marginPct: BigDecimal?,
    val markupPct: BigDecimal?,
    val computedPrice: BigDecimal,
    val method: PriceMethod,
    val formulaName: String?,
    val stock: BigDecimal,
    val stockValueCost: BigDecimal,
    val stockValueRetail: BigDecimal,
    val flags: Set<AnalysisFlag>,
)

data class AnalysisSummary(
    val count: Int = 0,
    val averageMarginPct: BigDecimal? = null,
    val potentialProfit: BigDecimal = BigDecimal.ZERO,
    val stockValueCost: BigDecimal = BigDecimal.ZERO,
    val stockValueRetail: BigDecimal = BigDecimal.ZERO,
    val flagCounts: Map<AnalysisFlag, Int> = emptyMap(),
    /** Products per margin band: <0, 0–10, 10–20, 20–30, 30–50, ≥50 %. */
    val marginBands: List<Int> = List(6) { 0 },
)

data class AnalysisReport(val rows: List<AnalysisRow> = emptyList(), val summary: AnalysisSummary = AnalysisSummary())

data class TopProduct(val title: String, val quantity: BigDecimal, val revenue: BigDecimal, val profit: BigDecimal?)

data class SalesSummary(
    val count: Int = 0,
    val revenue: BigDecimal = BigDecimal.ZERO,
    val cashRevenue: BigDecimal = BigDecimal.ZERO,
    val creditRevenue: BigDecimal = BigDecimal.ZERO,
    val openBalance: BigDecimal = BigDecimal.ZERO,
    val tax: BigDecimal = BigDecimal.ZERO,
    val discount: BigDecimal = BigDecimal.ZERO,
    /** Net profit over lines that carry a cost snapshot; [linesWithoutCost] are excluded. */
    val profit: BigDecimal = BigDecimal.ZERO,
    val linesWithoutCost: Int = 0,
    val daily: List<Pair<Long, BigDecimal>> = emptyList(),
    val topProducts: List<TopProduct> = emptyList(),
)

data class PriceChangeRow(
    val history: PriceHistoryEntity,
    val title: String,
    val categoryName: String?,
    val difference: BigDecimal,
    val changePct: BigDecimal?,
)

data class DashboardSnapshot(
    val todaySales: BigDecimal = BigDecimal.ZERO,
    val todayCount: Int = 0,
    val monthSales: BigDecimal = BigDecimal.ZERO,
    val monthProfit: BigDecimal = BigDecimal.ZERO,
    val openCredits: BigDecimal = BigDecimal.ZERO,
    val openCreditCount: Int = 0,
    val overdueCredits: Int = 0,
    val activeProducts: Int = 0,
    val lowStock: Int = 0,
    val negativeProfit: Int = 0,
    val lowMargin: Int = 0,
    val outdatedPrices: Int = 0,
    val missingCost: Int = 0,
    val dollarRate: BigDecimal = BigDecimal.ZERO,
    val dollarUpdatedAt: Long? = null,
    val recentBatches: List<PriceBatchEntity> = emptyList(),
    val scheduledBatches: Int = 0,
    val backupDue: Boolean = false,
    val lastBackupAt: Long = 0L,
    val loaded: Boolean = false,
)

class ReportRepository(
    private val db: KalabanDatabase,
    private val pricing: PricingRepository,
    private val config: ConfigRepository,
) {
    private fun categoryName(categories: Map<Long, CategoryEntity>, id: Long?, fa: Boolean): String? =
        id?.let { categories[it] }?.let { if (fa) it.nameFa else it.nameEn }

    suspend fun pricingAnalysis(filter: AnalysisFilter, fa: Boolean): AnalysisReport {
        val (ctx, priced) = pricing.priceAll(includeArchived = filter.includeArchived)
        val categories = db.categoryDao().getAll().associateBy { it.id }
        val threshold = ctx.minMarginPct
        val q = filter.query.trim()
        val rows = priced.asSequence()
            .filter { filter.categoryId == null || it.item.categoryId == filter.categoryId }
            .filter { q.isEmpty() || it.item.title.contains(q, true) || it.item.sku.orEmpty().contains(q, true) || it.item.barcode.orEmpty().contains(q, true) }
            .map { p ->
                val stock = Money.of(p.item.stockQty)
                val profit = p.storedProfit.netProfit
                val margin = p.storedProfit.marginPct
                val flags = buildSet {
                    if (p.result.cost.fullCost.signum() == 0) add(AnalysisFlag.MISSING_COST)
                    if (p.stored.signum() == 0) add(AnalysisFlag.MISSING_PRICE)
                    if (p.result.cost.fullCost.signum() > 0 && profit.signum() < 0) add(AnalysisFlag.NEGATIVE_PROFIT)
                    if (threshold.signum() > 0 && margin != null && profit.signum() >= 0 && margin < threshold) add(AnalysisFlag.LOW_MARGIN)
                    if (p.outdated) add(AnalysisFlag.OUTDATED)
                    val min = p.item.minPrice
                    val max = p.item.maxPrice
                    if ((min != null && p.item.sellPrice < min) || (max != null && p.item.sellPrice > max)) add(AnalysisFlag.OUT_OF_BOUNDS)
                    if (PricingWarning.FORMULA_ERROR in p.result.warnings || PricingWarning.CONDITION_ERROR in p.result.warnings) {
                        add(AnalysisFlag.FORMULA_ERROR)
                    }
                    if (p.item.lowStockThreshold > 0 && p.item.stockQty <= p.item.lowStockThreshold) add(AnalysisFlag.LOW_STOCK)
                }
                AnalysisRow(
                    item = p.item,
                    categoryName = categoryName(categories, p.item.categoryId, fa),
                    fullCost = p.result.cost.fullCost,
                    basePrice = p.stored,
                    cashPrice = p.storedSale.cashPrice,
                    revenue = p.storedProfit.revenue,
                    profit = profit,
                    marginPct = margin,
                    markupPct = p.storedProfit.markupPct,
                    computedPrice = p.result.finalPrice,
                    method = p.result.method,
                    formulaName = p.result.formulaName,
                    stock = stock,
                    stockValueCost = p.result.cost.fullCost.multiply(stock, Money.MC),
                    stockValueRetail = p.storedProfit.revenue.multiply(stock, Money.MC),
                    flags = flags,
                )
            }
            .filter { r -> filter.minMarginPct?.let { min -> r.marginPct != null && r.marginPct >= min } ?: true }
            .filter { r -> filter.maxMarginPct?.let { max -> r.marginPct != null && r.marginPct <= max } ?: true }
            .filter { r -> filter.flags.isEmpty() || r.flags.any { it in filter.flags } }
            .toList()
        val sorted = when (filter.sort) {
            AnalysisSort.MARGIN_ASC -> rows.sortedWith(compareBy(nullsLast()) { it.marginPct })
            AnalysisSort.MARGIN_DESC -> rows.sortedWith(compareByDescending<AnalysisRow, BigDecimal?>(nullsFirst()) { it.marginPct })
            AnalysisSort.PROFIT_DESC -> rows.sortedByDescending { it.profit }
            AnalysisSort.PRICE_DESC -> rows.sortedByDescending { it.basePrice }
            AnalysisSort.STOCK_VALUE_DESC -> rows.sortedByDescending { it.stockValueCost }
            AnalysisSort.TITLE -> rows.sortedBy { it.item.title }
        }
        val margins = sorted.mapNotNull { it.marginPct }
        val bands = MutableList(6) { 0 }
        margins.forEach { m ->
            val band = when {
                m.signum() < 0 -> 0
                m < BigDecimal(10) -> 1
                m < BigDecimal(20) -> 2
                m < BigDecimal(30) -> 3
                m < BigDecimal(50) -> 4
                else -> 5
            }
            bands[band]++
        }
        val summary = AnalysisSummary(
            count = sorted.size,
            averageMarginPct = margins.takeIf { it.isNotEmpty() }
                ?.let { list -> list.fold(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal(list.size), Money.MC) },
            potentialProfit = sorted.fold(BigDecimal.ZERO) { acc, r -> acc.add(r.profit.multiply(r.stock, Money.MC)) },
            stockValueCost = sorted.fold(BigDecimal.ZERO) { acc, r -> acc.add(r.stockValueCost) },
            stockValueRetail = sorted.fold(BigDecimal.ZERO) { acc, r -> acc.add(r.stockValueRetail) },
            flagCounts = AnalysisFlag.entries.associateWith { flag -> sorted.count { flag in it.flags } },
            marginBands = bands,
        )
        return AnalysisReport(sorted, summary)
    }

    suspend fun salesSummary(fromEpochMs: Long, toEpochMs: Long, categoryId: Long? = null): SalesSummary {
        val sales = db.saleDao().getInRange(fromEpochMs, toEpochMs)
        val lines = db.saleLineDao().getInRange(fromEpochMs, toEpochMs)
        val itemCategory = if (categoryId != null) db.itemDao().getAll().associate { it.id to it.categoryId } else emptyMap()
        val scoped = if (categoryId == null) lines else lines.filter { itemCategory[it.itemId] == categoryId }
        val scopedSaleIds = scoped.map { it.saleId }.toSet()
        val scopedSales = if (categoryId == null) sales else sales.filter { it.id in scopedSaleIds }
        var profit = BigDecimal.ZERO
        var withoutCost = 0
        scoped.forEach { line ->
            val cost = line.unitCost
            if (cost == null) {
                withoutCost++
            } else {
                profit = profit.add(
                    Money.of(line.lineTotal).subtract(Money.of(line.taxAmount)).subtract(Money.of(cost).multiply(Money.of(line.quantity), Money.MC)),
                )
            }
        }
        val dayMs = 24L * 60 * 60 * 1000
        val daily = scoped.groupBy { line -> sales.firstOrNull { it.id == line.saleId }?.createdAtEpochMs?.let(::startOfDay) ?: 0L }
            .map { (day, dayLines) -> day to dayLines.fold(BigDecimal.ZERO) { acc, l -> acc.add(Money.of(l.lineTotal)) } }
            .filter { it.first > 0L }
            .sortedBy { it.first }
        val fullDaily = if (daily.isEmpty()) daily else {
            val byDay = daily.toMap()
            generateSequence(daily.first().first) { it + dayMs }.takeWhile { it <= daily.last().first }
                .map { it to (byDay[it] ?: BigDecimal.ZERO) }.toList()
        }
        val top = scoped.groupBy { it.itemId }.map { (_, itemLines) ->
            val qty = itemLines.fold(BigDecimal.ZERO) { acc, l -> acc.add(Money.of(l.quantity)) }
            val revenue = itemLines.fold(BigDecimal.ZERO) { acc, l -> acc.add(Money.of(l.lineTotal)) }
            val linesProfit = if (itemLines.all { it.unitCost != null }) {
                itemLines.fold(BigDecimal.ZERO) { acc, l ->
                    acc.add(Money.of(l.lineTotal).subtract(Money.of(l.taxAmount)).subtract(Money.of(l.unitCost!!).multiply(Money.of(l.quantity), Money.MC)))
                }
            } else {
                null
            }
            TopProduct(itemLines.first().itemTitle, qty, revenue, linesProfit)
        }.sortedByDescending { it.revenue }.take(10)
        fun sumLines(f: (com.kalaban.farshidb2011.data.local.entity.SaleLineEntity) -> Double) =
            scoped.fold(BigDecimal.ZERO) { acc, l -> acc.add(Money.of(f(l))) }
        return SalesSummary(
            count = scopedSales.size,
            revenue = sumLines { it.lineTotal },
            cashRevenue = scopedSales.filter { it.paymentType == PaymentType.CASH }.fold(BigDecimal.ZERO) { acc, s -> acc.add(Money.of(s.totalAmount)) },
            creditRevenue = scopedSales.filter { it.paymentType == PaymentType.CREDIT }.fold(BigDecimal.ZERO) { acc, s -> acc.add(Money.of(s.totalAmount)) },
            openBalance = scopedSales.fold(BigDecimal.ZERO) { acc, s -> acc.add(Money.of(s.openBalance)) },
            tax = sumLines { it.taxAmount },
            discount = sumLines { it.discountAmount },
            profit = profit,
            linesWithoutCost = withoutCost,
            daily = fullDaily,
            topProducts = top,
        )
    }

    suspend fun priceChanges(fromEpochMs: Long, toEpochMs: Long, categoryId: Long?, fa: Boolean): List<PriceChangeRow> {
        val items = db.itemDao().getAll().associateBy { it.id }
        val categories = db.categoryDao().getAll().associateBy { it.id }
        return pricing.historyInRange(fromEpochMs, toEpochMs)
            .filter { h -> categoryId == null || items[h.itemId]?.categoryId == categoryId }
            .map { h ->
                val old = h.oldSellPrice?.let(Money::of)
                val new = Money.of(h.newSellPrice)
                val diff = if (old == null) BigDecimal.ZERO else new.subtract(old)
                val item = items[h.itemId]
                PriceChangeRow(
                    history = h,
                    title = item?.title ?: "#${h.itemId}",
                    categoryName = categoryName(categories, item?.categoryId, fa),
                    difference = diff,
                    changePct = if (old == null || old.signum() == 0) null else diff.multiply(Money.HUNDRED).divide(old, Money.MC),
                )
            }
    }

    suspend fun dashboard(): DashboardSnapshot {
        val now = System.currentTimeMillis()
        val today = startOfDay(now)
        val dayMs = 24L * 60 * 60 * 1000
        val todaySales = db.saleDao().getInRange(today, today + dayMs - 1)
        val monthStart = startOfJalaliMonth(now)
        val month = salesSummary(monthStart, today + dayMs - 1)
        val credits = db.saleDao().getAll().filter { it.paymentType == PaymentType.CREDIT && it.openBalance > 0.0 && !it.manuallyZeroed }
        val (ctx, priced) = pricing.priceAll()
        val threshold = ctx.minMarginPct
        val cfg = config.current()
        val history = db.dollarRateHistoryDao().getAll()
        val batches = db.priceBatchDao().getAll()
        val reminderMs = cfg.backupReminderDays * dayMs
        return DashboardSnapshot(
            todaySales = todaySales.fold(BigDecimal.ZERO) { acc, s -> acc.add(Money.of(s.totalAmount)) },
            todayCount = todaySales.size,
            monthSales = month.revenue,
            monthProfit = month.profit,
            openCredits = credits.fold(BigDecimal.ZERO) { acc, s -> acc.add(Money.of(s.openBalance)) },
            openCreditCount = credits.size,
            overdueCredits = credits.count { (it.dueAtEpochMs ?: Long.MAX_VALUE) < now },
            activeProducts = priced.size,
            lowStock = priced.count { it.item.lowStockThreshold > 0 && it.item.stockQty <= it.item.lowStockThreshold },
            negativeProfit = priced.count { it.result.cost.fullCost.signum() > 0 && it.storedProfit.netProfit.signum() < 0 },
            lowMargin = if (threshold.signum() > 0) {
                priced.count { p -> p.storedProfit.netProfit.signum() >= 0 && p.storedProfit.marginPct?.let { it < threshold } == true }
            } else {
                0
            },
            outdatedPrices = priced.count { it.outdated },
            missingCost = priced.count { it.result.cost.fullCost.signum() == 0 },
            dollarRate = ctx.policy.dollarRate,
            dollarUpdatedAt = history.maxOfOrNull { it.recordedAtEpochMs },
            recentBatches = batches.filter { it.status != BatchStatus.SCHEDULED }.sortedByDescending { it.createdAtEpochMs }.take(3),
            scheduledBatches = batches.count { it.status == BatchStatus.SCHEDULED },
            backupDue = cfg.backupReminderDays > 0 && now - cfg.lastBackupAt > reminderMs,
            lastBackupAt = cfg.lastBackupAt,
            loaded = true,
        )
    }

    /** Archived products are excluded from the dashboard but remain in reports when requested. */
    suspend fun activeItems() = db.itemDao().getAll().filter { it.status == ItemStatus.ACTIVE }

    companion object {
        fun startOfDay(epochMs: Long): Long = Calendar.getInstance().apply {
            timeInMillis = epochMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        fun startOfJalaliMonth(epochMs: Long): Long {
            val today = com.kalaban.farshidb2011.core.JalaliDate.fromEpochMs(epochMs)
            val first = com.kalaban.farshidb2011.core.JalaliDate.Date(today.year, today.month, 1)
            return startOfDay(first.toGregorianEpochMs())
        }
    }
}
