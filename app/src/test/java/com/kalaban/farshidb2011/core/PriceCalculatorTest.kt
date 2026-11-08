package com.kalaban.farshidb2011.core

import com.kalaban.farshidb2011.core.formula.FormulaEngine
import com.kalaban.farshidb2011.core.formula.ValueType
import com.kalaban.farshidb2011.core.pricing.CompiledFormula
import com.kalaban.farshidb2011.core.pricing.DiscountTier
import com.kalaban.farshidb2011.core.pricing.PriceAdjustment
import com.kalaban.farshidb2011.core.pricing.PriceBound
import com.kalaban.farshidb2011.core.pricing.PriceCalculator
import com.kalaban.farshidb2011.core.pricing.PriceMethod
import com.kalaban.farshidb2011.core.pricing.PriceRounding
import com.kalaban.farshidb2011.core.pricing.PricingModes
import com.kalaban.farshidb2011.core.pricing.PricingPolicy
import com.kalaban.farshidb2011.core.pricing.PricingWarning
import com.kalaban.farshidb2011.core.pricing.ProductCostInput
import com.kalaban.farshidb2011.core.pricing.ProductPricingInput
import com.kalaban.farshidb2011.core.pricing.RoundingPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class PriceCalculatorTest {
    private fun d(v: String) = BigDecimal(v)
    private fun assertDec(expected: String, actual: BigDecimal?) =
        assertEquals("expected $expected but was $actual", 0, d(expected).compareTo(actual))

    private val policy = PricingPolicy(dollarRate = d("20"), managerPct = d("10"))

    private fun product(
        purchase: String = "100",
        purchaseRate: String = "10",
        mode: String = PricingModes.AUTO,
        value: String? = null,
        categoryId: Long? = null,
        minPrice: String? = null,
        maxPrice: String? = null,
        current: String = "0",
        dollarLinked: Boolean = true,
        formulaId: Long? = null,
    ) = ProductPricingInput(
        cost = ProductCostInput(d(purchase), d(purchaseRate), dollarLinked),
        mode = mode,
        value = value?.let(::d),
        formulaId = formulaId,
        currentPrice = d(current),
        minPrice = minPrice?.let(::d),
        maxPrice = maxPrice?.let(::d),
        categoryId = categoryId,
    )

    private fun formula(
        id: Long,
        src: String,
        priority: Int = 0,
        categoryId: Long? = null,
        condition: String? = null,
        active: Boolean = true,
    ): CompiledFormula {
        val names = PriceCalculator.variableNames.toSet()
        return CompiledFormula(
            id = id,
            name = "f$id",
            active = active,
            priority = priority,
            categoryId = categoryId,
            expression = FormulaEngine.compile(src, ValueType.NUMBER, names),
            condition = condition?.let { FormulaEngine.compile(it, ValueType.CONDITION, names) },
            compileError = null,
        )
    }

    @Test
    fun defaultRuleMatchesOriginalDollarFormula() {
        val r = PriceCalculator.calculate(product(), policy)
        assertEquals(PriceMethod.DOLLAR_MANAGER, r.method)
        assertDec("220.00", r.finalPrice)
        assertDec("2", r.cost.ratio)
    }

    @Test
    fun counterPriceMatchesOriginalSaleFormula() {
        val p = PricingPolicy(dollarRate = d("1"), dollarChangePct = d("10"), profitPct = d("20"), taxPct = d("10"), installmentPct = d("10"))
        val s = PriceCalculator.salePrice(d("100"), p)
        assertDec("145.20", s.cashPrice)
        assertDec("159.72", s.creditPrice)
        assertDec("132.00", s.preTax)
        assertDec("13.20", s.taxAmount)
        assertDec(s.cashPrice.toPlainString(), s.preTax.add(s.taxAmount).add(s.roundingAdjustment))
    }

    @Test
    fun markupAndMarginAreDistinct() {
        val p = PricingPolicy(dollarRate = d("1"))
        val markup = PriceCalculator.calculate(product(purchase = "1000", purchaseRate = "1", mode = PricingModes.MARKUP, value = "25"), p)
        val margin = PriceCalculator.calculate(product(purchase = "1000", purchaseRate = "1", mode = PricingModes.MARGIN, value = "25"), p)
        assertDec("1250.00", markup.finalPrice)
        assertDec("1333.33", margin.finalPrice)
        val analysis = PriceCalculator.analyze(markup, p)
        assertDec("250", analysis.netProfit)
        assertDec("20", analysis.marginPct)
        assertDec("25", analysis.markupPct)
        assertDec("20", PriceCalculator.markupToMargin(d("25")))
        assertDec("25", PriceCalculator.marginToMarkup(d("20")))
        assertNull(PriceCalculator.marginToMarkup(d("100")))
    }

    @Test
    fun invalidStrategyValuesFallBackWithWarning() {
        val r = PriceCalculator.calculate(product(mode = PricingModes.MARGIN, value = "100"), policy)
        assertEquals(PriceMethod.DOLLAR_MANAGER, r.method)
        assertTrue(PricingWarning.INVALID_VALUE in r.warnings)
    }

    @Test
    fun roundingStepsAndModes() {
        val raw = d("1234567")
        assertDec("1235000", RoundingPolicy(0, PriceRounding.NEAREST, d("1000")).apply(raw))
        assertDec("1235000", RoundingPolicy(0, PriceRounding.UP, d("1000")).apply(raw))
        assertDec("1234000", RoundingPolicy(0, PriceRounding.DOWN, d("1000")).apply(raw))
        assertDec("2.12", RoundingPolicy(2, PriceRounding.BANKERS).apply(d("2.125")))
        assertDec("2.13", RoundingPolicy(2, PriceRounding.NEAREST).apply(d("2.125")))
        assertTrue(RoundingPolicy.isValidStep(d("1000"), 0))
        assertTrue(!RoundingPolicy.isValidStep(d("0.005"), 2))
    }

    @Test
    fun boundsClampAutomaticPricesButOnlyWarnForManual() {
        val low = PriceCalculator.calculate(product(minPrice = "300"), policy)
        assertDec("300", low.finalPrice)
        assertEquals(PriceBound.MIN, low.bound)
        val high = PriceCalculator.calculate(product(maxPrice = "150"), policy)
        assertDec("150", high.finalPrice)
        assertEquals(PriceBound.MAX, high.bound)
        val manual = PriceCalculator.calculate(
            product(mode = PricingModes.MANUAL, current = "50", minPrice = "150", purchaseRate = "20"),
            policy,
        )
        assertDec("50.00", manual.finalPrice)
        assertTrue(PricingWarning.OUTSIDE_BOUNDS in manual.warnings)
        assertTrue(PricingWarning.BELOW_COST in manual.warnings)
    }

    @Test
    fun manualDollarLinkedScalesSellNotPurchase() {
        // purchase 100 at rate 10; current rate 20 → sell 80 scales to 160; purchase stays 100 in cost breakdown
        val input = product(
            purchase = "100",
            purchaseRate = "10",
            mode = PricingModes.MANUAL,
            current = "80",
            dollarLinked = true,
        )
        val p = PricingPolicy(dollarRate = d("20"))
        val r = PriceCalculator.calculate(input, p)
        assertEquals(PriceMethod.MANUAL, r.method)
        assertDec("100", r.cost.purchasePrice)
        assertDec("200", r.cost.adjustedPurchase)
        assertDec("160.00", r.finalPrice)
        val unlinked = PriceCalculator.calculate(input.copy(cost = input.cost.copy(dollarLinked = false)), p)
        assertDec("80.00", unlinked.finalPrice)
        assertDec("100", unlinked.cost.adjustedPurchase)
    }

    @Test
    fun formulasFollowPriorityCategoryAndCondition() {
        val formulas = listOf(
            formula(1, "cost * 2", priority = 1),
            formula(2, "cost * 3", priority = 5, categoryId = 7),
            formula(3, "cost * 4", priority = 9, condition = "cost > 1000000"),
            formula(4, "cost * 5", priority = 10, active = false),
        )
        val general = PriceCalculator.calculate(product(), policy, formulas)
        assertEquals(PriceMethod.FORMULA, general.method)
        assertEquals(1L, general.formulaId)
        assertDec("400", general.finalPrice)
        val inCategory = PriceCalculator.calculate(product(categoryId = 7), policy, formulas)
        assertEquals(2L, inCategory.formulaId)
        assertDec("600", inCategory.finalPrice)
        val explicitInactive = PriceCalculator.calculate(product(mode = PricingModes.FORMULA, formulaId = 4), policy, formulas)
        assertEquals(PriceMethod.DOLLAR_MANAGER, explicitInactive.method)
        assertTrue(PricingWarning.FORMULA_INACTIVE in explicitInactive.warnings)
    }

    @Test
    fun failingFormulaFallsBackToDefaultRule() {
        val formulas = listOf(formula(1, "cost / (stock - stock)"))
        val r = PriceCalculator.calculate(product(), policy, formulas)
        assertEquals(PriceMethod.DOLLAR_MANAGER, r.method)
        assertDec("220.00", r.finalPrice)
        assertTrue(PricingWarning.FORMULA_ERROR in r.warnings)
    }

    @Test
    fun extraCostsOverheadAndDollarLink() {
        val p = PricingPolicy(dollarRate = d("2"), overheadPct = d("10"))
        val input = product(purchase = "100", purchaseRate = "1").let {
            it.copy(cost = it.cost.copy(transport = d("10"), packaging = d("5"), otherCost = d("5")))
        }
        val r = PriceCalculator.calculate(input, p)
        assertDec("220", r.cost.landedCost)
        assertDec("242", r.cost.fullCost)
        val local = PriceCalculator.calculate(input.copy(cost = input.cost.copy(dollarLinked = false)), p)
        assertDec("1", local.cost.ratio)
        assertDec("132", local.cost.fullCost)
        val badRate = PriceCalculator.calculate(product(purchaseRate = "0"), policy)
        assertTrue(PricingWarning.INVALID_PURCHASE_RATE in badRate.warnings)
        assertDec("110.00", badRate.finalPrice)
    }

    @Test
    fun exactDecimalsAndLargeAmounts() {
        val input = product(purchase = "0", purchaseRate = "1").let {
            it.copy(cost = it.cost.copy(transport = d("0.1"), packaging = d("0.2")))
        }
        assertDec("0.3", PriceCalculator.cost(input.cost, PricingPolicy(dollarRate = d("1"))).extraCosts)
        val big = PriceCalculator.calculate(
            product(purchase = "999999999999", purchaseRate = "2"),
            PricingPolicy(dollarRate = d("3"), rounding = RoundingPolicy(0)),
        )
        // 999,999,999,999 × 1.5 = 1,499,999,999,998.5 → HALF_UP at scale 0
        assertDec("1499999999999", big.finalPrice)
    }

    @Test
    fun breakEvenAndTax() {
        val p = PricingPolicy(dollarRate = d("1"), taxPct = d("10"))
        val r = PriceCalculator.calculate(product(purchase = "1000", purchaseRate = "1", mode = PricingModes.FIXED_PROFIT, value = "-50"), p)
        assertDec("950", r.finalPrice)
        assertTrue(PricingWarning.BELOW_COST in r.warnings)
        val a = PriceCalculator.analyze(r, p)
        assertDec("-50", a.netProfit)
        assertDec("1100", a.breakEvenPayable)
    }

    @Test
    fun quantityTiersPreferCategoryAndHighestThreshold() {
        val tiers = listOf(
            DiscountTier(null, d("10"), d("5")),
            DiscountTier(null, d("50"), d("10")),
            DiscountTier(3, d("20"), d("7")),
        )
        assertDec("10", PriceCalculator.discountFor(d("60"), null, tiers))
        assertDec("5", PriceCalculator.discountFor(d("12"), 1, tiers))
        assertDec("0", PriceCalculator.discountFor(d("12"), 3, tiers))
        assertDec("7", PriceCalculator.discountFor(d("25"), 3, tiers))
        val s = PriceCalculator.salePrice(d("200"), PricingPolicy(dollarRate = d("1")), d("10"))
        assertDec("180.00", s.cashPrice)
        assertDec("20.00", s.discountAmount)
    }

    @Test
    fun bulkAdjustments() {
        assertDec("110", PriceCalculator.adjust(d("100"), PriceAdjustment.Percent(d("10"))))
        assertDec("90", PriceCalculator.adjust(d("100"), PriceAdjustment.Percent(d("-10"))))
        assertDec("125", PriceCalculator.adjust(d("100"), PriceAdjustment.Amount(d("25"))))
        assertDec("150", PriceCalculator.adjust(d("100"), PriceAdjustment.Factor(d("1.5"))))
        assertDec("42", PriceCalculator.adjust(d("100"), PriceAdjustment.SetPrice(d("42"))))
    }
}
