package com.kalaban.farshidb2011

import com.kalaban.farshidb2011.core.PricingEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun pricingEngine_recalculatesWithManagerPercent() {
        val result = PricingEngine.recalculateSellPrice(
            purchasePrice = 100.0,
            purchaseDollarRate = 10.0,
            newDollarRate = 20.0,
            managerPercent = 10.0,
        )
        assertEquals(220.0, result, 0.0001)
    }

    @Test
    fun saleUnitPrice_appliesDollarChangeProfitAndTax() {
        // 100 * 1.1 dollarChange = 110; *1.2 profit = 132; *1.1 tax = 145.2
        val result = PricingEngine.saleUnitPrice(
            storedSellPrice = 100.0,
            dollarChangePercent = 10.0,
            taxPercent = 10.0,
            profitPercent = 20.0,
        )
        assertEquals(145.2, result, 0.0001)
    }

    @Test
    fun packageName_matchesApplicationId() {
        assertEquals("com.kalaban.farshidb2011", BuildConfig.APPLICATION_ID)
    }
}
