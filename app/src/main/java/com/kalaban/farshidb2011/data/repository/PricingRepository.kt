package com.kalaban.farshidb2011.data.repository

import androidx.room.withTransaction
import com.google.gson.Gson
import com.kalaban.farshidb2011.core.Money
import com.kalaban.farshidb2011.core.Numbers
import com.kalaban.farshidb2011.core.formula.FormulaEngine
import com.kalaban.farshidb2011.core.formula.FormulaError
import com.kalaban.farshidb2011.core.formula.FormulaException
import com.kalaban.farshidb2011.core.formula.ValueType
import com.kalaban.farshidb2011.core.pricing.CompiledFormula
import com.kalaban.farshidb2011.core.pricing.DiscountTier
import com.kalaban.farshidb2011.core.pricing.PriceAdjustment
import com.kalaban.farshidb2011.core.pricing.PriceCalculator
import com.kalaban.farshidb2011.core.pricing.PriceResult
import com.kalaban.farshidb2011.core.pricing.PriceRounding
import com.kalaban.farshidb2011.core.pricing.PricingPolicy
import com.kalaban.farshidb2011.core.pricing.PricingWarning
import com.kalaban.farshidb2011.core.pricing.ProductCostInput
import com.kalaban.farshidb2011.core.pricing.ProductPricingInput
import com.kalaban.farshidb2011.core.pricing.ProfitAnalysis
import com.kalaban.farshidb2011.core.pricing.RoundingPolicy
import com.kalaban.farshidb2011.core.pricing.SalePrice
import com.kalaban.farshidb2011.data.local.KalabanDatabase
import com.kalaban.farshidb2011.data.local.entity.AttributeDefinitionEntity
import com.kalaban.farshidb2011.data.local.entity.AttributeType
import com.kalaban.farshidb2011.data.local.entity.BatchStatus
import com.kalaban.farshidb2011.data.local.entity.FormulaVersionEntity
import com.kalaban.farshidb2011.data.local.entity.ItemEntity
import com.kalaban.farshidb2011.data.local.entity.ItemStatus
import com.kalaban.farshidb2011.data.local.entity.PriceBatchEntity
import com.kalaban.farshidb2011.data.local.entity.PriceHistoryEntity
import com.kalaban.farshidb2011.data.local.entity.PriceSource
import com.kalaban.farshidb2011.data.local.entity.PricingFormulaEntity
import com.kalaban.farshidb2011.data.local.entity.SettingsEntity
import com.kalaban.farshidb2011.domain.Actor
import java.math.BigDecimal

data class PricingContext(
    val policy: PricingPolicy,
    val formulas: List<CompiledFormula>,
    val knownVariables: Set<String>,
    val attributeDefinitions: List<AttributeDefinitionEntity>,
    val tiers: List<DiscountTier>,
    val minMarginPct: BigDecimal,
)

/** A product priced under the current rules, with its counter price and profit at that price. */
data class ProductPrice(
    val item: ItemEntity,
    val result: PriceResult,
    val storedSale: SalePrice,
    val storedProfit: ProfitAnalysis,
) {
    val stored: BigDecimal get() = Money.of(item.sellPrice)
    val outdated: Boolean get() = result.finalPrice.compareTo(stored) != 0
}

data class BulkScope(
    val categoryId: Long? = null,
    val itemIds: Set<Long>? = null,
    val includeArchived: Boolean = false,
)

sealed class BulkOperation {
    data object Recalculate : BulkOperation()
    data class Adjust(val adjustment: PriceAdjustment) : BulkOperation()
}

data class BulkRequest(
    val scope: BulkScope = BulkScope(),
    val operation: BulkOperation = BulkOperation.Recalculate,
    val applyRounding: Boolean = true,
    val respectBounds: Boolean = true,
)

data class BulkRow(
    val itemId: Long,
    val title: String,
    val oldPrice: BigDecimal,
    val newPrice: BigDecimal,
    val cost: BigDecimal,
    val stock: BigDecimal,
    val oldMarginPct: BigDecimal?,
    val newMarginPct: BigDecimal?,
    /** Change in profit if the current stock sells at the new price instead of the old one. */
    val profitImpact: BigDecimal,
    val warnings: Set<PricingWarning>,
) {
    val difference: BigDecimal get() = newPrice.subtract(oldPrice)
    val changePct: BigDecimal?
        get() = if (oldPrice.signum() == 0) null else difference.multiply(Money.HUNDRED).divide(oldPrice, Money.MC)
}

data class BulkPreview(
    val request: BulkRequest?,
    val rows: List<BulkRow>,
    val unchanged: Int,
    val invalid: Int,
    val conflicts: List<String> = emptyList(),
) {
    val affected: Int get() = rows.size
    val totalDifference: BigDecimal get() = rows.fold(BigDecimal.ZERO) { acc, r -> acc.add(r.difference) }
    val profitImpact: BigDecimal get() = rows.fold(BigDecimal.ZERO) { acc, r -> acc.add(r.profitImpact) }
    val averageChangePct: BigDecimal?
        get() = rows.mapNotNull { it.changePct }.takeIf { it.isNotEmpty() }
            ?.let { list -> list.fold(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal(list.size), Money.MC) }
    val belowCost: Int get() = rows.count { it.cost.signum() > 0 && it.newPrice < it.cost }
}

data class ApplyResult(val batchId: Long, val applied: Int, val conflicts: List<String>)

/** Serializable form of [BulkRequest] stored with scheduled batches. */
internal data class BulkRequestDto(
    val categoryId: Long? = null,
    val itemIds: List<Long>? = null,
    val includeArchived: Boolean = false,
    val op: String = "RECALC",
    val value: String? = null,
    val applyRounding: Boolean = true,
    val respectBounds: Boolean = true,
)

/** Pure encode/decode for scheduled bulk payloads (unit-testable without Room). */
object BulkRequestCodec {
    private val gson = Gson()

    fun encode(request: BulkRequest): String {
        val (op, value) = when (val o = request.operation) {
            BulkOperation.Recalculate -> "RECALC" to null
            is BulkOperation.Adjust -> when (val a = o.adjustment) {
                is PriceAdjustment.Percent -> "PERCENT" to a.pct.toPlainString()
                is PriceAdjustment.Amount -> "AMOUNT" to a.amount.toPlainString()
                is PriceAdjustment.Factor -> "FACTOR" to a.factor.toPlainString()
                is PriceAdjustment.SetPrice -> "SET" to a.price.toPlainString()
            }
        }
        return gson.toJson(
            BulkRequestDto(
                request.scope.categoryId,
                request.scope.itemIds?.toList(),
                request.scope.includeArchived,
                op,
                value,
                request.applyRounding,
                request.respectBounds,
            ),
        )
    }

    fun decode(payload: String): BulkRequest? = runCatching {
        val dto = gson.fromJson(payload, BulkRequestDto::class.java)
        val v = Numbers.parseDecimal(dto.value)
        val operation = when (dto.op) {
            "RECALC" -> BulkOperation.Recalculate
            "PERCENT" -> BulkOperation.Adjust(PriceAdjustment.Percent(v!!))
            "AMOUNT" -> BulkOperation.Adjust(PriceAdjustment.Amount(v!!))
            "FACTOR" -> BulkOperation.Adjust(PriceAdjustment.Factor(v!!))
            "SET" -> BulkOperation.Adjust(PriceAdjustment.SetPrice(v!!))
            else -> return@runCatching null
        }
        BulkRequest(BulkScope(dto.categoryId, dto.itemIds?.toSet(), dto.includeArchived), operation, dto.applyRounding, dto.respectBounds)
    }.getOrNull()
}

class PricingRepository(
    private val db: KalabanDatabase,
    private val config: ConfigRepository,
    private val audit: AuditRepository,
) {

    // ---- Formulas ----

    fun observeFormulas() = db.formulaDao().observeAll()
    fun observeVersions(formulaId: Long) = db.formulaDao().observeVersions(formulaId)
    suspend fun formula(id: Long) = db.formulaDao().getById(id)

    suspend fun knownVariables(): Set<String> = knownVariables(db.attributeDao().getDefinitions())

    private fun knownVariables(definitions: List<AttributeDefinitionEntity>): Set<String> =
        PriceCalculator.variableNames.toSet() + definitions
            .filter { it.active && it.type == AttributeType.NUMBER }
            .map { PriceCalculator.ATTRIBUTE_PREFIX + it.attrKey }

    fun validate(expression: String, condition: String?, known: Set<String>): Pair<FormulaError?, FormulaError?> =
        FormulaEngine.validate(expression, ValueType.NUMBER, known) to
            condition?.takeIf { it.isNotBlank() }?.let { FormulaEngine.validate(it, ValueType.CONDITION, known) }

    private fun PricingFormulaEntity.snapshot(by: Actor) = FormulaVersionEntity(
        formulaId = id,
        version = version,
        name = name,
        expression = expression,
        conditionExpr = conditionExpr,
        categoryId = categoryId,
        priority = priority,
        minPrice = minPrice,
        maxPrice = maxPrice,
        roundingStep = roundingStep,
        roundingMode = roundingMode,
        savedBy = by.name,
    )

    /** Inserts or updates; every update first stores the previous definition as a version. */
    suspend fun saveFormula(formula: PricingFormulaEntity, actor: Actor): Long = db.withTransaction {
        val now = System.currentTimeMillis()
        if (formula.id == 0L) {
            val id = db.formulaDao().insert(formula.copy(version = 1, createdAtEpochMs = now, updatedAtEpochMs = now))
            audit.log(actor, AuditAction.FORMULA_SAVE, AuditEntity.FORMULA, id, formula.name, "v1: ${formula.expression}")
            id
        } else {
            val previous = db.formulaDao().getById(formula.id) ?: error("Formula ${formula.id} not found")
            db.formulaDao().insertVersion(previous.snapshot(actor))
            val version = previous.version + 1
            db.formulaDao().update(
                formula.copy(version = version, createdAtEpochMs = previous.createdAtEpochMs, updatedAtEpochMs = now),
            )
            audit.log(
                actor, AuditAction.FORMULA_SAVE, AuditEntity.FORMULA, formula.id, formula.name,
                "v${previous.version} → v$version: ${previous.expression} → ${formula.expression}",
            )
            formula.id
        }
    }

    suspend fun setFormulaActive(id: Long, active: Boolean, actor: Actor) = db.withTransaction {
        val f = db.formulaDao().getById(id) ?: return@withTransaction
        db.formulaDao().update(f.copy(active = active, updatedAtEpochMs = System.currentTimeMillis()))
        audit.log(actor, AuditAction.FORMULA_ACTIVATE, AuditEntity.FORMULA, id, f.name, "active: ${f.active} → $active")
    }

    suspend fun duplicateFormula(id: Long, newName: String, actor: Actor): Long? = db.withTransaction {
        val f = db.formulaDao().getById(id) ?: return@withTransaction null
        val now = System.currentTimeMillis()
        val copyId = db.formulaDao().insert(
            f.copy(id = 0, name = newName, active = false, version = 1, createdAtEpochMs = now, updatedAtEpochMs = now),
        )
        audit.log(actor, AuditAction.FORMULA_SAVE, AuditEntity.FORMULA, copyId, newName, "copy of #$id")
        copyId
    }

    /** Products assigned to the formula fall back to automatic pricing; their stored prices are untouched. */
    suspend fun deleteFormula(id: Long, actor: Actor) = db.withTransaction {
        val f = db.formulaDao().getById(id) ?: return@withTransaction
        db.itemDao().detachFormula(id)
        db.formulaDao().delete(id)
        audit.log(actor, AuditAction.FORMULA_DELETE, AuditEntity.FORMULA, id, f.name, f.expression)
    }

    suspend fun revertFormula(versionId: Long, actor: Actor) = db.withTransaction {
        val v = db.formulaDao().getVersion(versionId) ?: return@withTransaction
        val current = db.formulaDao().getById(v.formulaId) ?: return@withTransaction
        db.formulaDao().insertVersion(current.snapshot(actor))
        db.formulaDao().update(
            current.copy(
                name = v.name,
                expression = v.expression,
                conditionExpr = v.conditionExpr,
                categoryId = v.categoryId,
                priority = v.priority,
                minPrice = v.minPrice,
                maxPrice = v.maxPrice,
                roundingStep = v.roundingStep,
                roundingMode = v.roundingMode,
                version = current.version + 1,
                updatedAtEpochMs = System.currentTimeMillis(),
            ),
        )
        audit.log(actor, AuditAction.FORMULA_REVERT, AuditEntity.FORMULA, current.id, current.name, "v${current.version} → v${v.version}")
    }

    // ---- Calculation ----

    fun policy(settings: SettingsEntity, cfg: AppConfig) = PricingPolicy(
        dollarRate = Money.of(settings.dollarRate),
        managerPct = Money.of(settings.managerPercent),
        profitPct = Money.of(settings.profitPercent),
        taxPct = Money.of(settings.taxPercent),
        dollarChangePct = Money.of(settings.dollarChangePercent),
        installmentPct = Money.of(settings.installmentDiffPercent),
        overheadPct = cfg.overheadPct,
        rounding = RoundingPolicy(cfg.moneyDecimals, cfg.roundingMode, cfg.roundingStep),
    )

    fun compile(f: PricingFormulaEntity, known: Set<String>, scale: Int): CompiledFormula {
        var error: FormulaError? = null
        val expression = try {
            FormulaEngine.compile(f.expression, ValueType.NUMBER, known)
        } catch (e: FormulaException) {
            error = e.error
            null
        }
        val condition = f.conditionExpr?.takeIf { it.isNotBlank() }?.let {
            try {
                FormulaEngine.compile(it, ValueType.CONDITION, known)
            } catch (e: FormulaException) {
                if (error == null) error = e.error
                null
            }
        }
        val rounding = if (f.roundingStep != null || f.roundingMode != null) {
            RoundingPolicy(scale, PriceRounding.parse(f.roundingMode), f.roundingStep?.let(Money::of))
        } else {
            null
        }
        return CompiledFormula(
            id = f.id,
            name = f.name,
            active = f.active,
            priority = f.priority,
            categoryId = f.categoryId,
            expression = if (error == null) expression else null,
            condition = condition,
            compileError = error,
            minPrice = f.minPrice?.let(Money::of),
            maxPrice = f.maxPrice?.let(Money::of),
            rounding = rounding,
        )
    }

    suspend fun loadContext(): PricingContext {
        val settings = db.settingsDao().get() ?: SettingsEntity()
        val cfg = config.current()
        val definitions = db.attributeDao().getDefinitions()
        val known = knownVariables(definitions)
        return PricingContext(
            policy = policy(settings, cfg),
            formulas = db.formulaDao().getAll().map { compile(it, known, cfg.moneyDecimals) },
            knownVariables = known,
            attributeDefinitions = definitions,
            tiers = db.discountRuleDao().getAll().filter { it.active }
                .map { DiscountTier(it.categoryId, Money.of(it.minQty), Money.of(it.discountPercent)) },
            minMarginPct = cfg.minMarginPct,
        )
    }

    /** Numeric attribute values keyed by item id, then by attribute key (formula variable `attr_<key>`). */
    suspend fun numericAttributes(definitions: List<AttributeDefinitionEntity>): Map<Long, Map<String, BigDecimal>> {
        val numeric = definitions.filter { it.active && it.type == AttributeType.NUMBER }.associateBy { it.id }
        if (numeric.isEmpty()) return emptyMap()
        return db.attributeDao().allValues()
            .mapNotNull { v -> numeric[v.attributeId]?.let { def -> Numbers.parseDecimal(v.value)?.let { Triple(v.itemId, def.attrKey, it) } } }
            .groupBy({ it.first }, { it.second to it.third })
            .mapValues { (_, pairs) -> pairs.toMap() }
    }

    fun input(item: ItemEntity, attributes: Map<String, BigDecimal> = emptyMap()) = ProductPricingInput(
        cost = ProductCostInput(
            purchasePrice = Money.of(item.purchasePrice),
            purchaseRate = Money.of(item.purchaseDollarRate),
            dollarLinked = item.dollarLinked,
            transport = Money.of(item.transportCost),
            packaging = Money.of(item.packagingCost),
            otherCost = Money.of(item.otherCost),
        ),
        mode = item.pricingMode,
        value = item.pricingValue?.let(Money::of),
        formulaId = item.formulaId,
        currentPrice = Money.of(item.sellPrice),
        minPrice = item.minPrice?.let(Money::of),
        maxPrice = item.maxPrice?.let(Money::of),
        categoryId = item.categoryId,
        stock = Money.of(item.stockQty),
        pieces = Money.of(item.piecesPerUnit),
        attributes = attributes,
    )

    fun price(item: ItemEntity, ctx: PricingContext, attributes: Map<String, BigDecimal>): ProductPrice {
        val result = PriceCalculator.calculate(input(item, attributes), ctx.policy, ctx.formulas, ctx.knownVariables)
        val sale = PriceCalculator.salePrice(Money.of(item.sellPrice), ctx.policy)
        return ProductPrice(item, result, sale, PriceCalculator.profit(sale, result.cost, ctx.policy))
    }

    suspend fun priceOf(item: ItemEntity): ProductPrice {
        val ctx = loadContext()
        return price(item, ctx, numericAttributes(ctx.attributeDefinitions)[item.id].orEmpty())
    }

    suspend fun priceAll(includeArchived: Boolean = false): Pair<PricingContext, List<ProductPrice>> {
        val ctx = loadContext()
        val attrs = numericAttributes(ctx.attributeDefinitions)
        val items = db.itemDao().getAll().filter { includeArchived || it.status == ItemStatus.ACTIVE }
        return ctx to items.map { price(it, ctx, attrs[it.id].orEmpty()) }
    }

    // ---- Bulk price changes ----

    private suspend fun itemsById(ids: Collection<Long>): Map<Long, ItemEntity> =
        ids.chunked(900).flatMap { db.itemDao().getByIds(it) }.associateBy { it.id }

    suspend fun preview(request: BulkRequest): BulkPreview {
        val ctx = loadContext()
        val attrs = numericAttributes(ctx.attributeDefinitions)
        val scope = request.scope
        val candidates = (scope.itemIds?.let { itemsById(it).values.toList() } ?: db.itemDao().getAll())
            .filter { scope.includeArchived || it.status == ItemStatus.ACTIVE }
            .filter { scope.categoryId == null || it.categoryId == scope.categoryId }
            .sortedBy { it.title }
        var unchanged = 0
        var invalid = 0
        val rows = ArrayList<BulkRow>()
        for (item in candidates) {
            val result = PriceCalculator.calculate(input(item, attrs[item.id].orEmpty()), ctx.policy, ctx.formulas, ctx.knownVariables)
            val old = Money.round(Money.of(item.sellPrice), ctx.policy.rounding.scale)
            val proposed = when (val op = request.operation) {
                BulkOperation.Recalculate -> result.finalPrice
                is BulkOperation.Adjust -> {
                    var p = PriceCalculator.adjust(old, op.adjustment)
                    p = if (request.applyRounding) ctx.policy.rounding.apply(p) else Money.round(p, ctx.policy.rounding.scale)
                    if (request.respectBounds) {
                        item.minPrice?.let { p = p.max(Money.of(it)) }
                        item.maxPrice?.let { p = p.min(Money.of(it)) }
                    }
                    p.max(BigDecimal.ZERO)
                }
            }
            if (!Money.isStorable(proposed)) {
                invalid++
                continue
            }
            if (proposed.compareTo(old) == 0) {
                unchanged++
                continue
            }
            val oldProfit = PriceCalculator.profit(PriceCalculator.salePrice(old, ctx.policy), result.cost, ctx.policy)
            val newProfit = PriceCalculator.profit(PriceCalculator.salePrice(proposed, ctx.policy), result.cost, ctx.policy)
            val warnings = if (request.operation is BulkOperation.Recalculate) {
                result.warnings
            } else {
                if (result.cost.fullCost.signum() > 0 && proposed < result.cost.fullCost) setOf(PricingWarning.BELOW_COST) else emptySet()
            }
            rows += BulkRow(
                itemId = item.id,
                title = item.title,
                oldPrice = old,
                newPrice = proposed,
                cost = result.cost.fullCost,
                stock = Money.of(item.stockQty),
                oldMarginPct = oldProfit.marginPct,
                newMarginPct = newProfit.marginPct,
                profitImpact = newProfit.revenue.subtract(oldProfit.revenue).multiply(Money.of(item.stockQty), Money.MC),
                warnings = warnings,
            )
        }
        return BulkPreview(request, rows, unchanged, invalid)
    }

    /**
     * Applies previewed rows atomically. Rows whose stored price changed since the preview are
     * skipped and reported as conflicts instead of being overwritten.
     */
    suspend fun apply(
        preview: BulkPreview,
        source: String,
        title: String,
        actor: Actor,
        note: String? = null,
        existingBatchId: Long? = null,
    ): ApplyResult = db.withTransaction {
        val now = System.currentTimeMillis()
        val batchId = existingBatchId ?: db.priceBatchDao().insert(
            PriceBatchEntity(source = source, title = title, note = note, actor = actor.name, createdAtEpochMs = now),
        )
        val scale = Money.scale
        val current = itemsById(preview.rows.map { it.itemId })
        val conflicts = ArrayList<String>()
        val history = ArrayList<PriceHistoryEntity>()
        for (row in preview.rows) {
            val item = current[row.itemId]
            if (item == null || Money.round(Money.of(item.sellPrice), scale).compareTo(row.oldPrice) != 0) {
                conflicts += row.title
                continue
            }
            db.itemDao().updateSellPrice(item.id, row.newPrice.toDouble(), now)
            history += PriceHistoryEntity(
                itemId = item.id,
                batchId = batchId,
                oldSellPrice = item.sellPrice,
                newSellPrice = row.newPrice.toDouble(),
                oldCost = row.cost.toDouble(),
                newCost = row.cost.toDouble(),
                source = source,
                note = note,
                actor = actor.name,
                changedAtEpochMs = now,
            )
        }
        db.priceHistoryDao().insertAll(history)
        val batch = db.priceBatchDao().getById(batchId)!!
        db.priceBatchDao().update(batch.copy(affectedCount = history.size, status = BatchStatus.APPLIED))
        audit.log(
            actor, AuditAction.PRICE_BATCH, AuditEntity.PRICE, batchId, "$title: ${history.size}",
            if (conflicts.isEmpty()) null else "conflicts: ${conflicts.joinToString()}",
        )
        ApplyResult(batchId, history.size, conflicts)
    }

    /** Proposes restoring each product's pre-batch price where the batch price is still in place. */
    suspend fun previewRollback(batchId: Long): BulkPreview {
        val ctx = loadContext()
        val rows = db.priceHistoryDao().getForBatch(batchId)
        val current = itemsById(rows.map { it.itemId })
        val conflicts = ArrayList<String>()
        val proposals = rows.mapNotNull { h ->
            val item = current[h.itemId] ?: return@mapNotNull null
            val now = Money.round(Money.of(item.sellPrice), ctx.policy.rounding.scale)
            if (now.compareTo(Money.round(Money.of(h.newSellPrice), ctx.policy.rounding.scale)) != 0) {
                conflicts += item.title
                return@mapNotNull null
            }
            val target = Money.round(Money.of(h.oldSellPrice ?: item.sellPrice), ctx.policy.rounding.scale)
            val cost = PriceCalculator.cost(input(item).cost, ctx.policy)
            val oldProfit = PriceCalculator.profit(PriceCalculator.salePrice(now, ctx.policy), cost, ctx.policy)
            val newProfit = PriceCalculator.profit(PriceCalculator.salePrice(target, ctx.policy), cost, ctx.policy)
            BulkRow(
                itemId = item.id,
                title = item.title,
                oldPrice = now,
                newPrice = target,
                cost = cost.fullCost,
                stock = Money.of(item.stockQty),
                oldMarginPct = oldProfit.marginPct,
                newMarginPct = newProfit.marginPct,
                profitImpact = newProfit.revenue.subtract(oldProfit.revenue).multiply(Money.of(item.stockQty), Money.MC),
                warnings = emptySet(),
            )
        }
        return BulkPreview(null, proposals, unchanged = 0, invalid = 0, conflicts = conflicts)
    }

    suspend fun rollback(batchId: Long, title: String, actor: Actor): ApplyResult? = db.withTransaction {
        val batch = db.priceBatchDao().getById(batchId) ?: return@withTransaction null
        if (batch.status != BatchStatus.APPLIED) return@withTransaction null
        val preview = previewRollback(batchId)
        val result = apply(preview, PriceSource.ROLLBACK, title, actor, note = "#$batchId")
        db.priceBatchDao().update(batch.copy(status = BatchStatus.ROLLED_BACK, rolledBackByBatchId = result.batchId))
        audit.log(actor, AuditAction.PRICE_ROLLBACK, AuditEntity.PRICE, batchId, "#$batchId → #${result.batchId}: ${result.applied}")
        result.copy(conflicts = result.conflicts + preview.conflicts)
    }

    fun observeBatches() = db.priceBatchDao().observeAll()
    suspend fun batch(id: Long) = db.priceBatchDao().getById(id)
    suspend fun batchRows(batchId: Long) = db.priceHistoryDao().getForBatch(batchId)
    fun observeHistory(itemId: Long) = db.priceHistoryDao().observeForItem(itemId)
    suspend fun historyInRange(from: Long, to: Long) = db.priceHistoryDao().getInRange(from, to)

    /** Records a single product's price/cost change (product editor, import). */
    suspend fun recordChange(
        itemId: Long,
        oldPrice: Double?,
        newPrice: Double,
        oldCost: Double?,
        newCost: Double?,
        source: String,
        actor: Actor,
        batchId: Long? = null,
    ) {
        db.priceHistoryDao().insert(
            PriceHistoryEntity(
                itemId = itemId,
                batchId = batchId,
                oldSellPrice = oldPrice,
                newSellPrice = newPrice,
                oldCost = oldCost,
                newCost = newCost,
                source = source,
                actor = actor.name,
            ),
        )
    }

    // ---- Scheduling ----

    fun decode(payload: String): BulkRequest? = BulkRequestCodec.decode(payload)

    suspend fun schedule(request: BulkRequest, atEpochMs: Long, title: String, actor: Actor): Long = db.withTransaction {
        val id = db.priceBatchDao().insert(
            PriceBatchEntity(
                source = PriceSource.SCHEDULED,
                title = title,
                actor = actor.name,
                status = BatchStatus.SCHEDULED,
                payload = BulkRequestCodec.encode(request),
                scheduledAtEpochMs = atEpochMs,
            ),
        )
        audit.log(actor, AuditAction.PRICE_SCHEDULE, AuditEntity.PRICE, id, title, "at $atEpochMs")
        id
    }

    suspend fun cancelScheduled(batchId: Long, actor: Actor) = db.withTransaction {
        val batch = db.priceBatchDao().getById(batchId) ?: return@withTransaction
        if (batch.status != BatchStatus.SCHEDULED) return@withTransaction
        db.priceBatchDao().update(batch.copy(status = BatchStatus.CANCELLED))
        audit.log(actor, AuditAction.PRICE_SCHEDULE_CANCEL, AuditEntity.PRICE, batchId, batch.title)
    }

    suspend fun scheduledBatches() = db.priceBatchDao().getScheduled()

    /** Executes a due scheduled batch against current prices; returns null if it is no longer pending. */
    suspend fun runScheduled(batchId: Long): ApplyResult? {
        val batch = db.priceBatchDao().getById(batchId) ?: return null
        if (batch.status != BatchStatus.SCHEDULED) return null
        val request = batch.payload?.let(::decode)
        if (request == null) {
            db.priceBatchDao().update(batch.copy(status = BatchStatus.FAILED))
            return null
        }
        return apply(preview(request), PriceSource.SCHEDULED, batch.title, Actor.SYSTEM, existingBatchId = batchId)
    }
}
