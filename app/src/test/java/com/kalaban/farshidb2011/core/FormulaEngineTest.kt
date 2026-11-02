package com.kalaban.farshidb2011.core

import com.kalaban.farshidb2011.core.formula.FormulaEngine
import com.kalaban.farshidb2011.core.formula.FormulaErrorCode
import com.kalaban.farshidb2011.core.formula.FormulaException
import com.kalaban.farshidb2011.core.formula.ValueType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.math.BigDecimal

class FormulaEngineTest {
    private val vars = mapOf(
        "cost" to BigDecimal("1000"),
        "manager_pct" to BigDecimal("10"),
        "stock" to BigDecimal("3"),
        "rate" to BigDecimal("1"),
        "zero" to BigDecimal.ZERO,
    )
    private val names = vars.keys + "missing"

    private fun eval(src: String): BigDecimal =
        FormulaEngine.evaluateNumber(FormulaEngine.compile(src, ValueType.NUMBER, names), vars, names)

    private fun cond(src: String): Boolean =
        FormulaEngine.evaluateCondition(FormulaEngine.compile(src, ValueType.CONDITION, names), vars, names)

    private fun assertNum(expected: String, src: String) =
        assertEquals("for $src", 0, BigDecimal(expected).compareTo(eval(src)))

    private fun assertError(code: FormulaErrorCode, src: String, type: ValueType = ValueType.NUMBER, position: Int? = null) {
        try {
            FormulaEngine.evaluateNumber(FormulaEngine.compile(src, type, names), vars, names)
            fail("expected $code for $src")
        } catch (e: FormulaException) {
            assertEquals("for $src", code, e.error.code)
            if (position != null) assertEquals("position for $src", position, e.error.position)
        }
    }

    @Test
    fun precedenceAndAssociativity() {
        assertNum("14", "2 + 3 * 4")
        assertNum("20", "(2 + 3) * 4")
        assertNum("-4", "-2^2")
        assertNum("512", "2^3^2")
        assertNum("2.5", "10 / 4")
        assertNum("0.25", "2^-2")
        assertNum("1", "10 - 4 - 5")
    }

    @Test
    fun decimalArithmeticIsExact() {
        assertTrue(cond("0.1 + 0.2 == 0.3"))
        assertNum("0.3333333333333333333333333333333333", "1 / 3")
    }

    @Test
    fun variablesAndFunctions() {
        assertNum("1100", "cost * (1 + manager_pct / 100)")
        assertNum("1235", "round(1234.5)")
        assertNum("1234.57", "round(1234.5678, 2)")
        assertNum("1235000", "round(1234567, -3)")
        assertNum("2000", "ceilto(1234, 1000)")
        assertNum("1000", "floorto(1999, 1000)")
        assertNum("2000", "roundto(1500, 1000)")
        assertNum("3", "clamp(5, 1, 3)")
        assertNum("30", "pct(200, 15)")
        assertNum("230", "addpct(200, 15)")
        assertNum("170", "subpct(200, 15)")
        assertNum("1000", "margin(750, 25)")
        assertNum("1", "min(3, 1, 2)")
        assertNum("3", "max(3, 1, 2)")
        assertNum("5", "abs(-5)")
        assertNum("2", "ceil(1.1)")
        assertNum("1", "floor(1.9)")
    }

    @Test
    fun conditionalsAreLazyAndLogicWorks() {
        assertNum("0", "if(zero > 0, 100 / zero, 0)")
        assertTrue(cond("cost > 10 and not (stock < 5) or rate == 1"))
        assertFalse(cond("cost < 10 && stock > 5"))
        assertTrue(cond("!(stock == 4)"))
        assertNum("1210", "if(stock >= 3, addpct(cost, 21), cost)")
    }

    @Test
    fun persianDigitsAndOperators() {
        assertNum("7.5", "۲ × ۳ + ۱٫۵")
        assertNum("4", "۸ ÷ ۲")
    }

    @Test
    fun reportsErrorsWithPositions() {
        assertError(FormulaErrorCode.EMPTY, "  ")
        assertError(FormulaErrorCode.UNEXPECTED_END, "2 +")
        assertError(FormulaErrorCode.UNBALANCED_PARENTHESES, "(2 + 3")
        assertError(FormulaErrorCode.UNEXPECTED_CHARACTER, "2 $ 3", position = 2)
        assertError(FormulaErrorCode.UNEXPECTED_TOKEN, "2 3")
        assertError(FormulaErrorCode.UNKNOWN_VARIABLE, "price + 1", position = 0)
        assertError(FormulaErrorCode.UNKNOWN_FUNCTION, "sqrt(4)")
        assertError(FormulaErrorCode.WRONG_ARGUMENT_COUNT, "min()")
        assertError(FormulaErrorCode.EXPECTED_CONDITION, "if(1, 2, 3)")
        assertError(FormulaErrorCode.EXPECTED_NUMBER, "cost > 1")
        assertError(FormulaErrorCode.EXPECTED_CONDITION, "cost + 1", ValueType.CONDITION)
        assertError(FormulaErrorCode.DIVISION_BY_ZERO, "1 / (cost - cost)")
        assertError(FormulaErrorCode.INVALID_ARGUMENT, "10 ^ 11")
        assertError(FormulaErrorCode.INVALID_ARGUMENT, "2 ^ 1.5")
        assertError(FormulaErrorCode.VALUE_TOO_LARGE, "10^10 * 10^10")
        assertError(FormulaErrorCode.INVALID_ARGUMENT, "margin(100, 100)")
        assertError(FormulaErrorCode.INVALID_ARGUMENT, "roundto(100, 0)")
        assertError(FormulaErrorCode.MISSING_VALUE, "missing * 2")
        assertError(FormulaErrorCode.TOO_COMPLEX, "(".repeat(100) + "1" + ")".repeat(100))
        assertError(FormulaErrorCode.TOO_LONG, "1+".repeat(1001) + "1")
    }

    @Test
    fun validateReturnsNullForValidFormula() {
        assertNull(FormulaEngine.validate("ceilto(margin(cost, 25), 1000)", ValueType.NUMBER, names))
        assertEquals(
            FormulaErrorCode.UNKNOWN_VARIABLE,
            FormulaEngine.validate("cost * markup", ValueType.NUMBER, names)?.code,
        )
    }
}
