package com.kalaban.farshidb2011

import com.kalaban.farshidb2011.core.pricing.PriceAdjustment
import com.kalaban.farshidb2011.core.pricing.PriceRounding
import com.kalaban.farshidb2011.data.repository.AppConfig
import com.kalaban.farshidb2011.data.repository.BulkOperation
import com.kalaban.farshidb2011.data.repository.BulkRequest
import com.kalaban.farshidb2011.data.repository.BulkRequestCodec
import com.kalaban.farshidb2011.data.repository.BulkScope
import com.kalaban.farshidb2011.data.repository.ConfigKeys
import com.kalaban.farshidb2011.data.repository.DisplayPriceMode
import com.kalaban.farshidb2011.domain.Permission
import com.kalaban.farshidb2011.domain.SessionManager
import com.kalaban.farshidb2011.domain.SessionUser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class DomainConfigTest {
    @Test
    fun appConfig_roundTripsAndFallsBackOnCorruptValues() {
        val original = AppConfig(
            businessName = "فروشگاه نمونه",
            moneyDecimals = 0,
            grouping = false,
            roundingMode = PriceRounding.UP,
            roundingStep = BigDecimal("1000"),
            displayPriceMode = DisplayPriceMode.CASH,
            backupReminderDays = 14,
            recentSearches = listOf("sku-1", "میخ"),
            formulasSeeded = true,
        )
        val roundTripped = AppConfig.fromMap(original.toMap())
        assertEquals(original.businessName, roundTripped.businessName)
        assertEquals(original.moneyDecimals, roundTripped.moneyDecimals)
        assertEquals(original.grouping, roundTripped.grouping)
        assertEquals(original.roundingMode, roundTripped.roundingMode)
        assertEquals(0, original.roundingStep!!.compareTo(roundTripped.roundingStep))
        assertEquals(original.displayPriceMode, roundTripped.displayPriceMode)
        assertEquals(original.backupReminderDays, roundTripped.backupReminderDays)
        assertEquals(original.recentSearches, roundTripped.recentSearches)
        assertEquals(original.formulasSeeded, roundTripped.formulasSeeded)

        val corrupt = mapOf(
            ConfigKeys.MONEY_DECIMALS to "nope",
            ConfigKeys.GROUPING to "maybe",
            ConfigKeys.ROUNDING_MODE to "WEIRD",
            ConfigKeys.ROUNDING_STEP to "-5",
            ConfigKeys.DISPLAY_PRICE_MODE to "OTHER",
            ConfigKeys.BACKUP_REMINDER_DAYS to "9999",
            ConfigKeys.CSV_DELIMITER to "|",
        )
        val defaults = AppConfig()
        val recovered = AppConfig.fromMap(corrupt)
        assertEquals(defaults.moneyDecimals, recovered.moneyDecimals)
        assertEquals(defaults.grouping, recovered.grouping)
        assertEquals(PriceRounding.NEAREST, recovered.roundingMode)
        assertNull(recovered.roundingStep)
        assertEquals(DisplayPriceMode.BASE, recovered.displayPriceMode)
        assertEquals(365, recovered.backupReminderDays) // coerced into 0..365
        assertEquals(defaults.csvDelimiter, recovered.csvDelimiter)
    }

    @Test
    fun permission_parseEncode_roundTripsAndIgnoresUnknown() {
        val set = setOf(Permission.VIEW_PRODUCTS, Permission.RECORD_SALES, Permission.MANAGE_CREDITS)
        val encoded = Permission.encode(set)
        assertEquals("VIEW_PRODUCTS,RECORD_SALES,MANAGE_CREDITS", encoded)
        assertEquals(set, Permission.parse(encoded))
        assertEquals(setOf(Permission.VIEW_REPORTS), Permission.parse("VIEW_REPORTS,NOT_A_REAL_PERM, VIEW_REPORTS"))
        assertTrue(Permission.parse("").isEmpty())
    }

    @Test
    fun sessionManager_escalatesLockoutWithInjectedClock() {
        var now = 1_000_000L
        val session = SessionManager(clock = { now })
        repeat(SessionManager.MAX_ATTEMPTS - 1) { session.recordFailure() }
        assertEquals(0L, session.lockoutRemaining())

        session.recordFailure() // 5th → base lockout
        assertEquals(SessionManager.BASE_LOCKOUT_MS, session.lockoutRemaining())

        now += SessionManager.BASE_LOCKOUT_MS / 2
        assertEquals(SessionManager.BASE_LOCKOUT_MS / 2, session.lockoutRemaining())

        session.recordFailure() // step 1 → 2x base from this instant
        assertEquals(SessionManager.BASE_LOCKOUT_MS * 2, session.lockoutRemaining())

        session.signIn(SessionUser.OWNER)
        assertEquals(0L, session.lockoutRemaining())
        assertEquals(SessionUser.OWNER.id, session.user.value?.id)
    }

    @Test
    fun bulkRequestCodec_roundTripsOperations() {
        val requests = listOf(
            BulkRequest(BulkScope(categoryId = 3L, includeArchived = true), BulkOperation.Recalculate, applyRounding = false),
            BulkRequest(BulkScope(itemIds = setOf(1L, 2L)), BulkOperation.Adjust(PriceAdjustment.Percent(BigDecimal("5"))), respectBounds = false),
            BulkRequest(operation = BulkOperation.Adjust(PriceAdjustment.Amount(BigDecimal("1000")))),
            BulkRequest(operation = BulkOperation.Adjust(PriceAdjustment.Factor(BigDecimal("1.1")))),
            BulkRequest(operation = BulkOperation.Adjust(PriceAdjustment.SetPrice(BigDecimal("25000")))),
        )
        for (request in requests) {
            val decoded = BulkRequestCodec.decode(BulkRequestCodec.encode(request))!!
            assertEquals(request.scope.categoryId, decoded.scope.categoryId)
            assertEquals(request.scope.itemIds, decoded.scope.itemIds)
            assertEquals(request.scope.includeArchived, decoded.scope.includeArchived)
            assertEquals(request.applyRounding, decoded.applyRounding)
            assertEquals(request.respectBounds, decoded.respectBounds)
            assertEquals(request.operation, decoded.operation)
        }
        assertNull(BulkRequestCodec.decode("""{"op":"NOPE"}"""))
        assertNull(BulkRequestCodec.decode("not-json"))
    }
}
