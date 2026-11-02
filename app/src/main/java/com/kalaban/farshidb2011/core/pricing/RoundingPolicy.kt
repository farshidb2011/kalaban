package com.kalaban.farshidb2011.core.pricing

import com.kalaban.farshidb2011.core.Money
import java.math.BigDecimal
import java.math.RoundingMode

/** Rounding modes offered to administrators; names are persisted, so do not rename entries. */
enum class PriceRounding(val mode: RoundingMode) {
    NEAREST(RoundingMode.HALF_UP),
    BANKERS(RoundingMode.HALF_EVEN),
    UP(RoundingMode.CEILING),
    DOWN(RoundingMode.FLOOR),
    ;

    companion object {
        fun parse(name: String?): PriceRounding = entries.firstOrNull { it.name == name } ?: NEAREST
    }
}

/**
 * Final-price rounding: optionally snap to a multiple of [step] (e.g. 1000 rials) using [rounding],
 * then fix the result to [scale] decimal places.
 */
data class RoundingPolicy(
    val scale: Int = Money.DEFAULT_SCALE,
    val rounding: PriceRounding = PriceRounding.NEAREST,
    val step: BigDecimal? = null,
) {
    fun apply(value: BigDecimal): BigDecimal {
        val s = step
        if (s == null || s.signum() <= 0) return value.setScale(scale, rounding.mode)
        return value.divide(s, 0, rounding.mode).multiply(s).setScale(scale, RoundingMode.HALF_UP)
    }

    companion object {
        /** Validates a step entered by the administrator against the money scale. */
        fun isValidStep(step: BigDecimal?, scale: Int): Boolean =
            step == null || (step.signum() > 0 && step.stripTrailingZeros().scale() <= scale)
    }
}
