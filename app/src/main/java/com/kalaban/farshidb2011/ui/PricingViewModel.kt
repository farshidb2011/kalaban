package com.kalaban.farshidb2011.ui

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.kalaban.farshidb2011.KalabanApp
import com.kalaban.farshidb2011.R
import com.kalaban.farshidb2011.core.Money
import com.kalaban.farshidb2011.core.Numbers
import com.kalaban.farshidb2011.core.formula.FormulaEngine
import com.kalaban.farshidb2011.core.formula.FormulaError
import com.kalaban.farshidb2011.core.formula.FormulaException
import com.kalaban.farshidb2011.core.formula.ValueType
import com.kalaban.farshidb2011.core.pricing.PriceCalculator
import com.kalaban.farshidb2011.core.pricing.PriceResult
import com.kalaban.farshidb2011.core.pricing.PriceRounding
import com.kalaban.farshidb2011.data.local.entity.DiscountRuleEntity
import com.kalaban.farshidb2011.data.local.entity.ItemEntity
import com.kalaban.farshidb2011.data.local.entity.PriceHistoryEntity
import com.kalaban.farshidb2011.data.local.entity.PriceSource
import com.kalaban.farshidb2011.data.local.entity.PricingFormulaEntity
import com.kalaban.farshidb2011.data.repository.AuditAction
import com.kalaban.farshidb2011.data.repository.AuditEntity
import com.kalaban.farshidb2011.data.repository.BulkPreview
import com.kalaban.farshidb2011.data.repository.BulkRequest
import com.kalaban.farshidb2011.domain.Permission
import com.kalaban.farshidb2011.notifications.ShopWorkers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.math.BigDecimal

data class FormulaEditorState(
    val id: Long = 0,
    val name: String = "",
    val expression: String = "",
    val condition: String = "",
    val description: String = "",
    val categoryId: Long? = null,
    val priority: String = "0",
    val active: Boolean = false,
    val minPrice: String = "",
    val maxPrice: String = "",
    val roundingStep: String = "",
    /** Empty string means "use the global rounding rule". */
    val roundingMode: String = "",
    val version: Int = 1,
    val expressionError: FormulaError? = null,
    val conditionError: FormulaError? = null,
    val nameMissing: Boolean = false,
    val loaded: Boolean = false,
)

/** Result of evaluating the edited formula against one product, next to every active formula. */
data class FormulaTest(
    val item: ItemEntity,
    val variables: List<Pair<String, BigDecimal>>,
    val value: BigDecimal?,
    val error: FormulaError?,
    val conditionMatched: Boolean?,
    val current: PriceResult,
    val comparisons: List<Triple<String, BigDecimal?, FormulaError?>>,
)

class PricingViewModel(application: Application) : GuardedViewModel(application) {
    private val pricing get() = app.pricing
    private val keepAlive = SharingStarted.Eagerly

    val formulas = pricing.observeFormulas().stateIn(viewModelScope, keepAlive, emptyList())
    val batches = pricing.observeBatches().stateIn(viewModelScope, keepAlive, emptyList())
    val discountRules = app.database.discountRuleDao().observeAll().stateIn(viewModelScope, keepAlive, emptyList())

    private val _variables = MutableStateFlow(PriceCalculator.variableNames)
    val variables: StateFlow<List<String>> = _variables.asStateFlow()

    private val _editor = MutableStateFlow(FormulaEditorState())
    val editor: StateFlow<FormulaEditorState> = _editor.asStateFlow()

    private val _test = MutableStateFlow<FormulaTest?>(null)
    val test: StateFlow<FormulaTest?> = _test.asStateFlow()

    private val _request = MutableStateFlow(BulkRequest())
    val request: StateFlow<BulkRequest> = _request.asStateFlow()

    private val _preview = MutableStateFlow<BulkPreview?>(null)
    val preview: StateFlow<BulkPreview?> = _preview.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _batchRows = MutableStateFlow<List<Pair<PriceHistoryEntity, String>>>(emptyList())
    val batchRows: StateFlow<List<Pair<PriceHistoryEntity, String>>> = _batchRows.asStateFlow()

    private var known: Set<String> = PriceCalculator.variableNames.toSet()

    fun refreshVariables() = launchSafely {
        known = pricing.knownVariables()
        _variables.value = known.sortedWith(compareBy({ it.startsWith(PriceCalculator.ATTRIBUTE_PREFIX) }, { PriceCalculator.variableNames.indexOf(it) }))
    }

    // ---- Formula editor ----

    private fun validate(s: FormulaEditorState): FormulaEditorState {
        val (expr, cond) = pricing.validate(s.expression, s.condition, known)
        return s.copy(expressionError = if (s.expression.isBlank()) null else expr, conditionError = cond)
    }

    fun startFormula(id: Long) = launchSafely {
        known = pricing.knownVariables()
        _test.value = null
        val f = if (id == 0L) null else pricing.formula(id)
        _editor.value = validate(
            if (f == null) {
                FormulaEditorState(loaded = true)
            } else {
                FormulaEditorState(
                    id = f.id,
                    name = f.name,
                    expression = f.expression,
                    condition = f.conditionExpr.orEmpty(),
                    description = f.description.orEmpty(),
                    categoryId = f.categoryId,
                    priority = f.priority.toString(),
                    active = f.active,
                    minPrice = f.minPrice?.let(Money::input).orEmpty(),
                    maxPrice = f.maxPrice?.let(Money::input).orEmpty(),
                    roundingStep = f.roundingStep?.let(Money::input).orEmpty(),
                    roundingMode = f.roundingMode.orEmpty(),
                    version = f.version,
                    loaded = true,
                )
            },
        )
    }

    fun updateFormula(transform: (FormulaEditorState) -> FormulaEditorState) =
        _editor.update { validate(transform(it)).copy(nameMissing = false) }

    fun saveFormula(onDone: (Long) -> Unit) = launchSafely {
        if (!allowed(Permission.MANAGE_FORMULAS)) return@launchSafely
        val s = validate(_editor.value)
        val expressionError = s.expressionError
            ?: FormulaEngine.validate(s.expression, ValueType.NUMBER, known)
        if (s.name.isBlank() || expressionError != null || s.conditionError != null) {
            _editor.value = s.copy(nameMissing = s.name.isBlank(), expressionError = expressionError)
            UiMessages.post(R.string.fix_highlighted_fields, error = true)
            return@launchSafely
        }
        val step = Numbers.parseDecimal(s.roundingStep)?.takeIf { it.signum() > 0 }
        val min = Numbers.parseDecimal(s.minPrice)
        val max = Numbers.parseDecimal(s.maxPrice)
        if (min != null && max != null && min > max) {
            UiMessages.post(R.string.error_min_above_max, error = true)
            return@launchSafely
        }
        val id = pricing.saveFormula(
            PricingFormulaEntity(
                id = s.id,
                name = s.name.trim(),
                expression = s.expression.trim(),
                conditionExpr = s.condition.trim().ifEmpty { null },
                description = s.description.trim().ifEmpty { null },
                categoryId = s.categoryId,
                priority = Numbers.parseInt(s.priority) ?: 0,
                active = s.active,
                minPrice = min?.toDouble(),
                maxPrice = max?.toDouble(),
                roundingStep = step?.toDouble(),
                roundingMode = s.roundingMode.ifEmpty { null },
            ),
            actor,
        )
        UiMessages.post(R.string.formula_saved)
        onDone(id)
    }

    fun setActive(id: Long, active: Boolean) = launchSafely {
        if (!allowed(Permission.MANAGE_FORMULAS)) return@launchSafely
        pricing.setFormulaActive(id, active, actor)
    }

    fun duplicate(id: Long, name: String) = launchSafely {
        if (!allowed(Permission.MANAGE_FORMULAS)) return@launchSafely
        pricing.duplicateFormula(id, name, actor)
        UiMessages.post(R.string.formula_duplicated)
    }

    fun deleteFormula(id: Long, onDone: () -> Unit = {}) = launchSafely {
        if (!allowed(Permission.MANAGE_FORMULAS)) return@launchSafely
        pricing.deleteFormula(id, actor)
        UiMessages.post(R.string.formula_deleted)
        onDone()
    }

    fun observeVersions(id: Long) = pricing.observeVersions(id)

    fun revert(versionId: Long) = launchSafely {
        if (!allowed(Permission.MANAGE_FORMULAS)) return@launchSafely
        pricing.revertFormula(versionId, actor)
        startFormula(_editor.value.id)
        UiMessages.post(R.string.formula_reverted)
    }

    /** Evaluates the formula being edited against [item] and compares it with every active formula. */
    fun testFormula(item: ItemEntity) = launchSafely {
        val s = _editor.value
        _test.value = withContext(Dispatchers.Default) {
            val ctx = pricing.loadContext()
            val attrs = pricing.numericAttributes(ctx.attributeDefinitions)[item.id].orEmpty()
            val input = pricing.input(item, attrs)
            val cost = PriceCalculator.cost(input.cost, ctx.policy)
            val vars = PriceCalculator.variables(input, ctx.policy, cost)
            var value: BigDecimal? = null
            var error: FormulaError? = null
            var matched: Boolean? = null
            try {
                val condition = s.condition.trim().takeIf { it.isNotEmpty() }
                if (condition != null) {
                    matched = FormulaEngine.evaluateCondition(FormulaEngine.compile(condition, ValueType.CONDITION, ctx.knownVariables), vars, ctx.knownVariables)
                }
                value = FormulaEngine.evaluateNumber(FormulaEngine.compile(s.expression, ValueType.NUMBER, ctx.knownVariables), vars, ctx.knownVariables)
            } catch (e: FormulaException) {
                error = e.error
            }
            val comparisons = ctx.formulas.filter { it.active && it.id != s.id }.map { f ->
                val expr = f.expression
                if (expr == null) {
                    Triple(f.name, null, f.compileError)
                } else {
                    try {
                        Triple(f.name, FormulaEngine.evaluateNumber(expr, vars, ctx.knownVariables), null)
                    } catch (e: FormulaException) {
                        Triple(f.name, null, e.error)
                    }
                }
            }
            FormulaTest(
                item = item,
                variables = vars.entries.sortedBy { it.key }.map { it.key to it.value },
                value = value,
                error = error,
                conditionMatched = matched,
                current = PriceCalculator.calculate(input, ctx.policy, ctx.formulas, ctx.knownVariables),
                comparisons = comparisons,
            )
        }
    }

    // ---- Bulk price changes ----

    fun prepareBulk(request: BulkRequest) {
        _request.value = request
        _preview.value = null
    }

    fun updateRequest(transform: (BulkRequest) -> BulkRequest) {
        _request.update(transform)
        _preview.value = null
    }

    fun runPreview() = launchSafely {
        if (!allowed(Permission.MANAGE_PRICES)) return@launchSafely
        _busy.value = true
        try {
            _preview.value = withContext(Dispatchers.Default) { pricing.preview(_request.value) }
        } finally {
            _busy.value = false
        }
    }

    fun applyPreview(title: String, source: String = PriceSource.BULK, onDone: () -> Unit = {}) = launchSafely {
        if (!allowed(Permission.MANAGE_PRICES)) return@launchSafely
        val p = _preview.value ?: return@launchSafely
        _busy.value = true
        try {
            val result = pricing.apply(p, source, title, actor)
            _preview.value = null
            if (result.conflicts.isEmpty()) {
                UiMessages.post(R.string.bulk_applied, result.applied)
            } else {
                UiMessages.post(R.string.bulk_applied_conflicts, result.applied, result.conflicts.size, error = true)
            }
            onDone()
        } finally {
            _busy.value = false
        }
    }

    fun schedule(atEpochMs: Long, title: String, onDone: () -> Unit = {}) = launchSafely {
        if (!allowed(Permission.MANAGE_PRICES)) return@launchSafely
        if (atEpochMs <= System.currentTimeMillis()) {
            UiMessages.post(R.string.schedule_in_past, error = true)
            return@launchSafely
        }
        val id = pricing.schedule(_request.value, atEpochMs, title, actor)
        ShopWorkers.schedulePriceBatch(getApplication(), id, atEpochMs)
        _preview.value = null
        UiMessages.post(R.string.bulk_scheduled)
        onDone()
    }

    fun loadBatch(id: Long) = launchSafely {
        val rows = pricing.batchRows(id)
        val titles = rows.map { it.itemId }.distinct().chunked(900)
            .flatMap { app.database.itemDao().getByIds(it) }
            .associate { it.id to it.title }
        _batchRows.value = rows.map { it to (titles[it.itemId] ?: "#${it.itemId}") }
    }

    fun previewRollback(batchId: Long) = launchSafely {
        if (!allowed(Permission.MANAGE_PRICES)) return@launchSafely
        _preview.value = pricing.previewRollback(batchId)
    }

    fun rollback(batchId: Long, title: String, onDone: () -> Unit = {}) = launchSafely {
        if (!allowed(Permission.MANAGE_PRICES)) return@launchSafely
        val result = pricing.rollback(batchId, title, actor)
        _preview.value = null
        if (result == null) {
            UiMessages.post(R.string.rollback_unavailable, error = true)
        } else {
            UiMessages.post(R.string.rollback_done, result.applied, result.conflicts.size)
            onDone()
        }
    }

    fun cancelScheduled(batchId: Long) = launchSafely {
        if (!allowed(Permission.MANAGE_PRICES)) return@launchSafely
        pricing.cancelScheduled(batchId, actor)
        ShopWorkers.cancelPriceBatch(getApplication(), batchId)
        UiMessages.post(R.string.schedule_cancelled)
    }

    fun clearPreview() {
        _preview.value = null
    }

    // ---- Quantity discounts ----

    fun saveDiscountRule(rule: DiscountRuleEntity) = launchSafely {
        if (!allowed(Permission.MANAGE_PRICES)) return@launchSafely
        if (rule.minQty <= 0.0 || rule.discountPercent <= 0.0 || rule.discountPercent >= 100.0) {
            UiMessages.post(R.string.discount_invalid, error = true)
            return@launchSafely
        }
        val dao = app.database.discountRuleDao()
        if (rule.id == 0L) dao.insert(rule) else dao.update(rule)
        app.audit.log(
            actor, AuditAction.CATALOG_UPDATE, AuditEntity.PRICE,
            rule.id.takeIf { it != 0L }, "discount: ${rule.name}", "≥${rule.minQty} → ${rule.discountPercent}%",
        )
        UiMessages.post(R.string.saved_ok)
    }

    fun deleteDiscountRule(rule: DiscountRuleEntity) = launchSafely {
        if (!allowed(Permission.MANAGE_PRICES)) return@launchSafely
        app.database.discountRuleDao().delete(rule.id)
        app.audit.log(
            actor, AuditAction.CATALOG_UPDATE, AuditEntity.PRICE,
            rule.id, "discount deleted: ${rule.name}",
        )
    }

    companion object {
        val roundingOptions = listOf("") + PriceRounding.entries.map { it.name }

        fun factory() = appViewModelFactory { PricingViewModel(KalabanApp.instance) }
    }
}
