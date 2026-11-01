package com.kalaban.farshidb2011.core

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

/** App-wide money rounding and display — always 2 decimal places. */
object Money {
    private const val SCALE = 2
    private val rounding = RoundingMode.HALF_UP

    fun round(value: Double): Double {
        if (value.isNaN() || value.isInfinite()) return 0.0
        return BigDecimal.valueOf(value).setScale(SCALE, rounding).toDouble()
    }

    /** Locale-stable display string with exactly two fraction digits (e.g. `12.50`). */
    fun format(value: Double): String =
        String.format(Locale.US, "%.${SCALE}f", round(value))
}
