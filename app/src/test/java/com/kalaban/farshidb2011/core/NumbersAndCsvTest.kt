package com.kalaban.farshidb2011.core

import com.kalaban.farshidb2011.core.csv.Csv
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class NumbersAndCsvTest {
    @Test
    fun parsesLocalizedNumbers() {
        assertEquals(0, BigDecimal("123.45").compareTo(Numbers.parseDecimal("۱۲۳٫۴۵")))
        assertEquals(0, BigDecimal("1250000").compareTo(Numbers.parseDecimal("1,250,000")))
        assertEquals(0, BigDecimal("1250").compareTo(Numbers.parseDecimal("۱٬۲۵۰")))
        assertEquals(0, BigDecimal("-12.5").compareTo(Numbers.parseDecimal(" -12.5 ")))
        assertEquals(0, BigDecimal("42").compareTo(Numbers.parseDecimal("٤٢")))
        assertNull(Numbers.parseDecimal("abc"))
        assertNull(Numbers.parseDecimal(""))
        assertNull(Numbers.parseDecimal("1.2.3"))
        assertEquals(7, Numbers.parseInt("۷"))
        assertNull(Numbers.parseInt("7.5"))
        assertEquals(12000.0, "۱۲۰۰۰".toAmountOrNull()!!, 0.0)
    }

    @Test
    fun moneyFormattingAndEditableForm() {
        Money.configure(2, true)
        assertEquals("1,250,000.50", Money.format(BigDecimal("1250000.5")))
        assertEquals("1250000.5", Money.input(1250000.5))
        Money.configure(0, false)
        assertEquals("1250001", Money.format(BigDecimal("1250000.5")))
        Money.configure(Money.DEFAULT_SCALE, true)
        assertEquals(0, BigDecimal("9999999999999").compareTo(Money.maxStorable(2)))
    }

    @Test
    fun sanitizesAndGroupsDecimalInput() {
        assertEquals("1250000.5", Numbers.sanitizeDecimalInput("1,250,000.5"))
        assertEquals("1250000.5", Numbers.sanitizeDecimalInput("۱٬۲۵۰٬۰۰۰٫۵"))
        assertEquals("1250000.", Numbers.sanitizeDecimalInput("1250000."))
        assertEquals("-12.5", Numbers.sanitizeDecimalInput("-12.5"))
        assertEquals("1,250,000.5", Numbers.groupThousands("1250000.5"))
        assertEquals("-1,250,000", Numbers.groupThousands("-1250000"))
        assertEquals("0.5", Numbers.groupThousands("0.5"))
        assertEquals("1,250,000.", Numbers.groupThousands("1250000."))
        assertEquals("", Numbers.groupThousands(""))
    }

    @Test
    fun csvParsesQuotesNewlinesAndBom() {
        val text = Csv.BOM + "title,price,note\r\n\"Silk, red\",1200,\"says \"\"hi\"\"\"\r\nWool,900,\"line1\nline2\"\r\n\r\n"
        val rows = Csv.parse(text)
        assertEquals(3, rows.size)
        assertEquals(listOf("Silk, red", "1200", "says \"hi\""), rows[1])
        assertEquals("line1\nline2", rows[2][2])
    }

    @Test
    fun csvDetectsDelimiterAndRoundTrips() {
        assertEquals(';', Csv.detectDelimiter("a;b;c\n1;2;3"))
        assertEquals('\t', Csv.detectDelimiter("a\tb\n1\t2"))
        val rows = listOf(listOf("name", "note"), listOf("A;B", "x\"y"), listOf(" padded", ""))
        assertEquals(rows, Csv.parse(Csv.write(rows, ';'), ';'))
    }

    @Test
    fun csvNeutralizesFormulaInjection() {
        assertEquals("'=HYPERLINK(1)", Csv.text("=HYPERLINK(1)"))
        assertEquals("'+cmd", Csv.text("+cmd"))
        assertEquals("plain", Csv.text("plain"))
        assertEquals("", Csv.text(null))
    }
}
