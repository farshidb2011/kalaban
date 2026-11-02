package com.kalaban.farshidb2011.core.pricing

import com.kalaban.farshidb2011.core.Money
import com.kalaban.farshidb2011.core.formula.Expr
import com.kalaban.farshidb2011.core.formula.FormulaEngine
import com.kalaban.farshidb2011.core.formula.FormulaError
import com.kalaban.farshidb2011.core.formula.FormulaException
import java.math.BigDecimal
import java.math.BigDecimal.ONE
import java.math.BigDecimal.ZERO

/** Per-product pricing modes; values are persisted in `items.pricingMode`. */
object PricingModes {
    const val AUTO = "AUTO"
    const val FORMULA = "FORMULA"
    const val MARKUP = "MARKUP"
    const val MARGIN = "MARGIN"
    const val FIXED_PROFIT = "FIXED_PROFIT"
    const val MANUAL = "MANUAL"
    val all = listOf(AUTO, FORMULA, MARKUP, MARGIN, FIXED_PROFIT, MANUAL)
}

enum class PriceMethod { DOLLAR_MANAGER, FORMULA, MARKUP, MARGIN, FIXED_PROFIT, MANUAL }

enum class PriceBound { MIN, MAX }

enum class PricingWarning {
    INVALID_PURCHASE_RATE,
    MISSING_COST,
    INVALID_VALUE,
    FORMULA_MISSING,
    FORMULA_INACTIVE,
    FORMULA_ERROR,
    CONDITION_ERROR,
    BELOW_COST,
    OUTSIDE_BOUNDS,
}

data class PricingPolicy(
    val dollarRate: BigDecimal,
    val managerPct: BigDecimal = ZERO,
    val profitPct: BigDecimal = ZERO,
    val taxPct: BigDecimal = ZERO,
    val dollarChangePct: BigDecimal = ZERO,
    val installmentPct: BigDecimal = ZERO,
    val overheadPct: BigDecimal = ZERO,
    val rounding: RoundingPolicy = RoundingPolicy(),
)

data class ProductCostInput(
    val purchasePrice: BigDecimal,
    val purchaseRate: BigDecimal,
    val dollarLinked: Boolean = true,
    val transport: BigDecimal = ZERO,
    val packaging: BigDecimal = ZERO,
    val otherCost: BigDecimal = ZERO,
)

data class ProductPricingInput(
    val cost: ProductCostInput,
    val mode: String = PricingModes.AUTO,
    val value: BigDecimal? = null,
    val formulaId: Long? = null,
    val currentPrice: BigDecimal = ZERO,
    val minPrice: BigDecimal? = null,
    val maxPrice: BigDecimal? = null,
    val categoryId: Long? = null,
    val stock: BigDecimal = ZERO,
    val pieces: BigDecimal = ONE,
    val attributes: Map<String, BigDecimal> = emptyMap(),
)

/** A formula prepared for evaluation; [categoryId] null means it applies to every category. */
data class CompiledFormula(
    val id: Long,
    val name: String,
    val active: Boolean,
    val priority: Int,
    val categoryId: Long?,
    val expression: Expr?,
    val condition: Expr?,
    val compileError: FormulaError?,
    val minPrice: BigDecimal? = null,
    val maxPrice: BigDecimal? = null,
    val rounding: RoundingPolicy? = null,
)

data class CostBreakdown(
    val purchasePrice: BigDecimal,
    val ratio: BigDecimal,
    val adjustedPurchase: BigDecimal,
    val extraCosts: BigDecimal,
    val landedCost: BigDecimal,
    val overheadAmount: BigDecimal,
    val fullCost: BigDecimal,
    val rateValid: Boolean,
)

data class PriceResult(
    val cost: CostBreakdown,
    val method: PriceMethod,
    val formulaId: Long?,
    val formulaName: String?,
    val rawPrice: BigDecimal,
    val roundedPrice: BigDecimal,
    val finalPrice: BigDecimal,
    val bound: PriceBound?,
    val formulaError: FormulaError?,
    val warnings: Set<PricingWarning>,
) {
    val roundingAdjustment: BigDecimal get() = roundedPrice.subtract(rawPrice)
}

/** Counter price built from a base (stored) price; components add up exactly to [cashPrice]. */
data class SalePrice(
    val basePrice: BigDecimal,
    val dollarChangeAmount: BigDecimal,
    val profitAmount: BigDecimal,
    val discountPct: BigDecimal,
    val discountAmount: BigDecimal,
    val preTax: BigDecimal,
    val taxAmount: BigDecimal,
    val roundingAdjustment: BigDecimal,
    val cashPrice: BigDecimal,
    val installmentAmount: BigDecimal,
    val creditPrice: BigDecimal,
)

data class ProfitAnalysis(
    val revenue: BigDecimal,
    val grossProfit: BigDecimal,
    val netProfit: BigDecimal,
    val marginPct: BigDecimal?,
    val markupPct: BigDecimal?,
    val breakEvenPrice: BigDecimal,
    val breakEvenPayable: BigDecimal,
)

data class DiscountTier(val categoryId: Long?, val minQty: BigDecimal, val percent: BigDecimal)

sealed class PriceAdjustment {
    data class Percent(val pct: BigDecimal) : PriceAdjustment()
    data class Amount(val amount: BigDecimal) : PriceAdjustment()
    data class Factor(val factor: BigDecimal) : PriceAdjustment()
    data class SetPrice(val price: BigDecimal) : PriceAdjustment()
}

object PriceCalculator {
    /** Variables every formula may use; numeric custom attributes add `attr_<key>`. */
    val variableNames: List<String> = listOf(
        "cost", "full_cost", "purchase_price", "purchase_rate", "rate", "ratio",
        "transport", "packaging", "other_cost", "extra_cost", "overhead", "overhead_pct",
        "manager_pct", "profit_pct", "tax_pct", "dollar_change_pct", "installment_pct",
        "value", "current_price", "min_price", "max_price", "stock", "pieces", "dollar_linked", "category_id",
    )

    const val ATTRIBUTE_PREFIX = "attr_"

    private fun pct(p: BigDecimal): BigDecimal = p.max(ZERO).divide(Money.HUNDRED, Money.MC)

    fun cost(input: ProductCostInput, policy: PricingPolicy): CostBreakdown {
        val purchase = input.purchasePrice.max(ZERO)
        val rateValid = input.purchaseRate.signum() > 0
        val ratio = if (input.dollarLinked && rateValid && policy.dollarRate.signum() > 0) {
            policy.dollarRate.divide(input.purchaseRate, Money.MC)
        } else {
            ONE
        }
        val adjusted = purchase.multiply(ratio, Money.MC)
        val extras = input.transport.max(ZERO).add(input.packaging.max(ZERO)).add(input.otherCost.max(ZERO))
        val landed = adjusted.add(extras)
        val overhead = landed.multiply(pct(policy.overheadPct), Money.MC)
        return CostBreakdown(
            purchasePrice = purchase,
            ratio = ratio,
            adjustedPurchase = adjusted,
            extraCosts = extras,
            landedCost = landed,
            overheadAmount = overhead,
            fullCost = landed.add(overhead),
            rateValid = rateValid || !input.dollarLinked,
        )
    }

    fun variables(input: ProductPricingInput, policy: PricingPolicy, cost: CostBreakdown): Map<String, BigDecimal> {
        val vars = HashMap<String, BigDecimal>()
        vars["cost"] = cost.landedCost
        vars["full_cost"] = cost.fullCost
        vars["purchase_price"] = cost.purchasePrice
        vars["purchase_rate"] = input.cost.purchaseRate
        vars["rate"] = policy.dollarRate
        vars["ratio"] = cost.ratio
        vars["transport"] = input.cost.transport
        vars["packaging"] = input.cost.packaging
        vars["other_cost"] = input.cost.otherCost
        vars["extra_cost"] = cost.extraCosts
        vars["overhead"] = cost.overheadAmount
        vars["overhead_pct"] = policy.overheadPct
        vars["manager_pct"] = policy.managerPct
        vars["profit_pct"] = policy.profitPct
        vars["tax_pct"] = policy.taxPct
        vars["dollar_change_pct"] = policy.dollarChangePct
        vars["installment_pct"] = policy.installmentPct
        input.value?.let { vars["value"] = it }
        vars["current_price"] = input.currentPrice
        input.minPrice?.let { vars["min_price"] = it }
        input.maxPrice?.let { vars["max_price"] = it }
        vars["stock"] = input.stock
        vars["pieces"] = input.pieces
        vars["dollar_linked"] = if (input.cost.dollarLinked) ONE else ZERO
        vars["category_id"] = BigDecimal.valueOf(input.categoryId ?: 0L)
        input.attributes.forEach { (key, v) -> vars[ATTRIBUTE_PREFIX + key] = v }
        return vars
    }

    private fun legacy(cost: CostBreakdown, policy: PricingPolicy): BigDecimal =
        cost.fullCost.multiply(ONE.add(pct(policy.managerPct)), Money.MC)

    /**
     * Base (stored) sell price for one product. Without formulas and extra costs this is exactly the
     * original rule: purchase × (currentRate / purchaseRate) × (1 + manager%).
     */
    fun calculate(
        input: ProductPricingInput,
        policy: PricingPolicy,
        formulas: List<CompiledFormula> = emptyList(),
        knownVariables: Set<String> = emptySet(),
    ): PriceResult {
        val cost = cost(input.cost, policy)
        val warnings = linkedSetOf<PricingWarning>()
        if (!cost.rateValid) warnings += PricingWarning.INVALID_PURCHASE_RATE
        if (cost.fullCost.signum() == 0) warnings += PricingWarning.MISSING_COST
        val vars by lazy { variables(input, policy, cost) }
        val known by lazy { knownVariables + vars.keys }

        var method = PriceMethod.DOLLAR_MANAGER
        var used: CompiledFormula? = null
        var formulaError: FormulaError? = null

        fun tryFormula(f: CompiledFormula): BigDecimal? {
            if (f.expression == null) {
                formulaError = f.compileError
                warnings += PricingWarning.FORMULA_ERROR
                return null
            }
            return try {
                FormulaEngine.evaluateNumber(f.expression, vars, known).also {
                    method = PriceMethod.FORMULA
                    used = f
                }
            } catch (e: FormulaException) {
                formulaError = e.error
                warnings += PricingWarning.FORMULA_ERROR
                null
            }
        }

        val value = input.value
        val raw: BigDecimal = when (input.mode) {
            PricingModes.MANUAL -> {
                method = PriceMethod.MANUAL
                input.currentPrice
            }
            PricingModes.MARKUP -> if (value != null && value.signum() >= 0) {
                method = PriceMethod.MARKUP
                cost.fullCost.multiply(ONE.add(pct(value)), Money.MC)
            } else {
                warnings += PricingWarning.INVALID_VALUE
                legacy(cost, policy)
            }
            PricingModes.MARGIN -> if (value != null && value.signum() >= 0 && value < Money.HUNDRED) {
                method = PriceMethod.MARGIN
                cost.fullCost.divide(ONE.subtract(pct(value)), Money.MC)
            } else {
                warnings += PricingWarning.INVALID_VALUE
                legacy(cost, policy)
            }
            PricingModes.FIXED_PROFIT -> if (value != null) {
                method = PriceMethod.FIXED_PROFIT
                cost.fullCost.add(value)
            } else {
                warnings += PricingWarning.INVALID_VALUE
                legacy(cost, policy)
            }
            PricingModes.FORMULA -> {
                val f = formulas.firstOrNull { it.id == input.formulaId }
                when {
                    f == null -> {
                        warnings += PricingWarning.FORMULA_MISSING
                        legacy(cost, policy)
                    }
                    !f.active -> {
                        warnings += PricingWarning.FORMULA_INACTIVE
                        legacy(cost, policy)
                    }
                    else -> tryFormula(f) ?: legacy(cost, policy)
                }
            }
            else -> {
                val candidates = formulas
                    .filter { it.active && (it.categoryId == null || it.categoryId == input.categoryId) }
                    .sortedWith(compareByDescending<CompiledFormula> { it.priority }.thenBy { it.id })
                var picked: BigDecimal? = null
                for (f in candidates) {
                    val matches = when {
                        f.compileError != null -> {
                            warnings += PricingWarning.FORMULA_ERROR
                            formulaError = f.compileError
                            false
                        }
                        f.condition == null -> true
                        else -> try {
                            FormulaEngine.evaluateCondition(f.condition, vars, known)
                        } catch (e: FormulaException) {
                            warnings += PricingWarning.CONDITION_ERROR
                            formulaError = e.error
                            false
                        }
                    }
                    if (matches) {
                        picked = tryFormula(f)
                        break
                    }
                }
                picked ?: legacy(cost, policy)
            }
        }

        val formula = used
        val rounded = if (method == PriceMethod.MANUAL) {
            Money.round(raw, policy.rounding.scale)
        } else {
            (formula?.rounding ?: policy.rounding).apply(raw)
        }
        var final = rounded
        var bound: PriceBound? = null
        if (method != PriceMethod.MANUAL) {
            fun floor(limit: BigDecimal?) {
                if (limit != null && final < limit) {
                    final = limit.setScale(policy.rounding.scale, java.math.RoundingMode.HALF_UP)
                    bound = PriceBound.MIN
                }
            }
            fun ceiling(limit: BigDecimal?) {
                if (limit != null && final > limit) {
                    final = limit.setScale(policy.rounding.scale, java.math.RoundingMode.HALF_UP)
                    bound = PriceBound.MAX
                }
            }
            floor(formula?.minPrice)
            ceiling(formula?.maxPrice)
            floor(input.minPrice)
            ceiling(input.maxPrice)
        } else if ((input.minPrice != null && final < input.minPrice) || (input.maxPrice != null && final > input.maxPrice)) {
            warnings += PricingWarning.OUTSIDE_BOUNDS
        }
        if (cost.fullCost.signum() > 0 && final < cost.fullCost) warnings += PricingWarning.BELOW_COST

        return PriceResult(
            cost = cost,
            method = method,
            formulaId = formula?.id,
            formulaName = formula?.name,
            rawPrice = raw,
            roundedPrice = rounded,
            finalPrice = final,
            bound = bound,
            formulaError = formulaError,
            warnings = warnings,
        )
    }

    /** Counter price: base × (1 + dollar change%) × (1 + profit%) × (1 − discount%) × (1 + tax%), rounded once. */
    fun salePrice(basePrice: BigDecimal, policy: PricingPolicy, discountPct: BigDecimal = ZERO): SalePrice {
        val withDollarChange = basePrice.multiply(ONE.add(pct(policy.dollarChangePct)), Money.MC)
        val withProfit = withDollarChange.multiply(ONE.add(pct(policy.profitPct)), Money.MC)
        val discount = discountPct.max(ZERO).min(Money.HUNDRED)
        val preTax = withProfit.multiply(ONE.subtract(pct(discount)), Money.MC)
        val cash = policy.rounding.apply(preTax.multiply(ONE.add(pct(policy.taxPct)), Money.MC))
        val scale = policy.rounding.scale
        val preTaxRounded = Money.round(preTax, scale)
        val tax = Money.round(preTax.multiply(pct(policy.taxPct), Money.MC), scale)
        val credit = policy.rounding.apply(cash.multiply(ONE.add(pct(policy.installmentPct)), Money.MC))
        return SalePrice(
            basePrice = basePrice,
            dollarChangeAmount = Money.round(withDollarChange.subtract(basePrice), scale),
            profitAmount = Money.round(withProfit.subtract(withDollarChange), scale),
            discountPct = discount,
            discountAmount = Money.round(withProfit.subtract(preTax), scale),
            preTax = preTaxRounded,
            taxAmount = tax,
            roundingAdjustment = cash.subtract(preTaxRounded).subtract(tax),
            cashPrice = cash,
            installmentAmount = credit.subtract(cash),
            creditPrice = credit,
        )
    }

    /** Profit of selling at [sale] given the product's cost; revenue excludes tax but keeps rounding gains. */
    fun profit(sale: SalePrice, cost: CostBreakdown, policy: PricingPolicy): ProfitAnalysis {
        val revenue = sale.cashPrice.subtract(sale.taxAmount)
        val net = revenue.subtract(cost.fullCost)
        return ProfitAnalysis(
            revenue = revenue,
            grossProfit = revenue.subtract(cost.landedCost),
            netProfit = net,
            marginPct = if (revenue.signum() > 0) net.multiply(Money.HUNDRED).divide(revenue, Money.MC) else null,
            markupPct = if (cost.fullCost.signum() > 0) net.multiply(Money.HUNDRED).divide(cost.fullCost, Money.MC) else null,
            breakEvenPrice = cost.fullCost,
            breakEvenPayable = cost.fullCost.multiply(ONE.add(pct(policy.taxPct)), Money.MC),
        )
    }

    fun analyze(result: PriceResult, policy: PricingPolicy): ProfitAnalysis =
        profit(salePrice(result.finalPrice, policy), result.cost, policy)

    fun markupToMargin(markupPct: BigDecimal): BigDecimal =
        markupPct.multiply(Money.HUNDRED).divide(Money.HUNDRED.add(markupPct), Money.MC)

    fun marginToMarkup(marginPct: BigDecimal): BigDecimal? {
        val keep = Money.HUNDRED.subtract(marginPct)
        return if (keep.signum() <= 0) null else marginPct.multiply(Money.HUNDRED).divide(keep, Money.MC)
    }

    /** Quantity tier for a sale line: category tiers win over store-wide tiers; the highest reached threshold applies. */
    fun discountFor(qty: BigDecimal, categoryId: Long?, tiers: List<DiscountTier>): BigDecimal {
        val categoryTiers = tiers.filter { it.categoryId != null && it.categoryId == categoryId }
        val pool = categoryTiers.ifEmpty { tiers.filter { it.categoryId == null } }
        return pool.filter { qty >= it.minQty }.maxByOrNull { it.minQty }?.percent ?: ZERO
    }

    fun adjust(current: BigDecimal, adjustment: PriceAdjustment): BigDecimal = when (adjustment) {
        is PriceAdjustment.Percent -> current.multiply(ONE.add(adjustment.pct.divide(Money.HUNDRED, Money.MC)), Money.MC)
        is PriceAdjustment.Amount -> current.add(adjustment.amount)
        is PriceAdjustment.Factor -> current.multiply(adjustment.factor, Money.MC)
        is PriceAdjustment.SetPrice -> adjustment.price
    }
}
