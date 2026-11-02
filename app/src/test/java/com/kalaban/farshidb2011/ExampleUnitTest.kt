package com.kalaban.farshidb2011

import com.kalaban.farshidb2011.core.JalaliDate
import com.kalaban.farshidb2011.core.Money
import com.kalaban.farshidb2011.core.PricingEngine
import com.kalaban.farshidb2011.data.backup.KalabanBackupArchive
import com.kalaban.farshidb2011.data.prefs.SecurePrefs
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun purchaseRateAfterEdit_keepsOriginalRateUnlessPriceChanges() {
        // Editing title/stock: the stored price round-trips through the form, so the original rate stays
        assertEquals(50.0, PricingEngine.purchaseRateAfterEdit(33.33, 50.0, 99.99 / 3, 60.0), 0.0001)
        // Restocking at a new purchase price: today's rate applies
        assertEquals(60.0, PricingEngine.purchaseRateAfterEdit(33.33, 50.0, 40.0, 60.0), 0.0001)
        // Rows without a valid stored rate fall back to today's rate
        assertEquals(60.0, PricingEngine.purchaseRateAfterEdit(33.33, 0.0, 33.33, 60.0), 0.0001)
    }

    @Test
    fun money_roundsAndFormatsConsistently() {
        assertEquals(12.35, Money.round(12.345), 0.0001)
        assertEquals(0.0, Money.round(Double.NaN), 0.0001)
        assertEquals("12.50", Money.format(12.5))
    }

    @Test
    fun jalaliDate_validatesMonthLengthsAndRoundTrips() {
        assertNotNull(JalaliDate.parse("1403/06/31"))
        assertNotNull(JalaliDate.parse("1403/07/30"))
        assertNull(JalaliDate.parse("1403/07/31"))
        assertNull(JalaliDate.parse("1200/01/01"))
        assertNull(JalaliDate.parse("1403/13/01"))

        val original = JalaliDate.Date(1404, 8, 11)
        val epoch = original.toGregorianEpochMs()
        val roundTripped = JalaliDate.fromEpochMs(epoch)
        assertEquals(original, roundTripped)
    }

    @Test
    fun backupArchive_packsAndUnpacksWithImages() {
        val meta = """{"settings":{"id":1,"dollarRate":60.0}}"""
        val imgBytes = byteArrayOf(1, 2, 3, 4, 5)
        val packed = KalabanBackupArchive.pack(
            metaJson = meta,
            images = listOf(
                KalabanBackupArchive.ImageEntry(
                    itemId = 42L,
                    mime = "image/png",
                    bytes = imgBytes,
                ),
            ),
        )
        assertTrue(KalabanBackupArchive.isKalabanArchive(packed))
        val unpacked = KalabanBackupArchive.unpack(packed)
        assertEquals(meta, unpacked.metaJson)
        assertEquals(1, unpacked.images.size)
        assertEquals(42L, unpacked.images[0].itemId)
        assertEquals("image/png", unpacked.images[0].mime)
        assertArrayEquals(imgBytes, unpacked.images[0].bytes)
    }

    @Test
    fun securePrefs_hashesDeterministicWithSalt() {
        val h1 = SecurePrefs.hash("1234", "aabbcc")
        val h2 = SecurePrefs.hash("1234", "aabbcc")
        val h3 = SecurePrefs.hash("1234", "ddeeff")
        assertEquals(h1, h2)
        assertFalse(h1 == h3)
    }

    @Test
    fun packageName_matchesApplicationId() {
        assertEquals("com.kalaban.farshidb2011", BuildConfig.APPLICATION_ID)
    }
}
