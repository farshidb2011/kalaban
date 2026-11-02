package com.kalaban.farshidb2011.core

/**
 * Compatibility helpers kept for product edit flows.
 * Live sell-price math lives in [com.kalaban.farshidb2011.core.pricing.PriceCalculator].
 */
object PricingEngine {
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
}
