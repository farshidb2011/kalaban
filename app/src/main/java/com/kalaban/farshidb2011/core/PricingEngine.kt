package com.kalaban.farshidb2011.core

/**
 * Price recalculation per RUNBOOK:
 * newPrice ≈ basePrice × (newRate / purchaseRate) × (1 + managerPercent / 100)
 *
 * Sale unit price also applies dollarChangePercent, then profit, then tax.
 * All amounts are raw doubles (no currency dependency in storage).
 */
object PricingEngine {
    fun recalculateSellPrice(
        purchasePrice: Double,
        purchaseDollarRate: Double,
        newDollarRate: Double,
        managerPercent: Double,
    ): Double {
        if (purchaseDollarRate <= 0.0) return Money.round(purchasePrice)
        val ratio = newDollarRate / purchaseDollarRate
        val managerFactor = 1.0 + (managerPercent.coerceAtLeast(0.0) / 100.0)
        return Money.round(purchasePrice * ratio * managerFactor)
    }

    /** Rate to keep on an edited item: its original purchase rate, unless a new purchase price was entered. */
    fun purchaseRateAfterEdit(
        previousPurchasePrice: Double,
        previousPurchaseRate: Double,
        newPurchasePrice: Double,
        currentDollarRate: Double,
    ): Double {
        val samePurchase = Money.round(previousPurchasePrice) == Money.round(newPurchasePrice)
        return if (samePurchase && previousPurchaseRate > 0.0) previousPurchaseRate else currentDollarRate
    }

    fun installmentPrice(cashPrice: Double, installmentDiffPercent: Double): Double {
        return Money.round(cashPrice * (1.0 + installmentDiffPercent.coerceAtLeast(0.0) / 100.0))
    }

    fun withTaxAndProfit(base: Double, taxPercent: Double, profitPercent: Double): Double {
        val withProfit = base * (1.0 + profitPercent.coerceAtLeast(0.0) / 100.0)
        return Money.round(withProfit * (1.0 + taxPercent.coerceAtLeast(0.0) / 100.0))
    }

    /** Final per-unit cash sell price for a transaction line. */
    fun saleUnitPrice(
        storedSellPrice: Double,
        dollarChangePercent: Double,
        taxPercent: Double,
        profitPercent: Double,
        installment: Boolean = false,
        installmentDiffPercent: Double = 0.0,
    ): Double {
        val withDollarChange = storedSellPrice * (1.0 + dollarChangePercent.coerceAtLeast(0.0) / 100.0)
        val cash = withTaxAndProfit(withDollarChange, taxPercent, profitPercent)
        return if (installment) installmentPrice(cash, installmentDiffPercent) else cash
    }
}
