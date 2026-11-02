package com.kalaban.farshidb2011.core

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.Locale

/**
 * App-wide money arithmetic. Amounts are computed as [BigDecimal] and stored as SQLite REAL, which is
 * lossless only while values keep at most 15 significant digits — see [maxStorable].
 */
object Money {
    const val DEFAULT_SCALE = 2
    const val MAX_SCALE = 3

    /** Precision for intermediate results; rounding happens only at explicitly defined points. */
    val MC: MathContext = MathContext.DECIMAL128
    val HUNDRED: BigDecimal = BigDecimal(100)

    @Volatile var scale: Int = DEFAULT_SCALE
        private set

    @Volatile var grouping: Boolean = true
        private set

    fun configure(scale: Int, grouping: Boolean) {
        this.scale = scale.coerceIn(0, MAX_SCALE)
        this.grouping = grouping
    }

    fun maxStorable(scale: Int = this.scale): BigDecimal =
        BigDecimal.TEN.pow(15 - scale.coerceIn(0, MAX_SCALE)).subtract(BigDecimal.ONE)

    fun isStorable(value: BigDecimal): Boolean = value.abs() <= maxStorable()

    fun of(value: Double): BigDecimal =
        if (value.isNaN() || value.isInfinite()) BigDecimal.ZERO else BigDecimal.valueOf(value)

    fun round(value: BigDecimal, scale: Int = this.scale): BigDecimal = value.setScale(scale, RoundingMode.HALF_UP)

    fun round(value: Double): Double = round(of(value)).toDouble()

    /** Display string honoring the configured scale and grouping, with Latin digits (the UI font renders Persian glyphs). */
    fun format(value: BigDecimal): String {
        val s = scale
        return String.format(Locale.US, if (grouping) "%,.${s}f" else "%.${s}f", value.setScale(s, RoundingMode.HALF_UP))
    }

    fun format(value: Double): String = format(of(value))

    /** Plain editable form without grouping or trailing zeros (round-trips through [Numbers.parseDecimal]). */
    fun input(value: BigDecimal): String = round(value).stripTrailingZeros().toPlainString()

    fun input(value: Double): String = input(of(value))

    fun percent(value: BigDecimal, decimals: Int = 1): String =
        String.format(Locale.US, "%.${decimals}f%%", value.setScale(decimals, RoundingMode.HALF_UP))
}
