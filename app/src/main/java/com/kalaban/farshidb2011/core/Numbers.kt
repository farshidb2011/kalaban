package com.kalaban.farshidb2011.core

import java.math.BigDecimal

/** Parses numbers typed with Latin, Persian (۰-۹) or Arabic-Indic (٠-٩) digits. */
object Numbers {
    private val decimalPattern = Regex("^[-+]?(\\d+\\.?\\d*|\\.\\d+)$")

    /** Maps localized digits and separators to ASCII one-for-one, so character positions are preserved. */
    fun normalizeDigits(text: String): String {
        val out = StringBuilder(text.length)
        for (ch in text) {
            out.append(
                when (ch) {
                    in '\u06F0'..'\u06F9' -> '0' + (ch - '\u06F0')
                    in '\u0660'..'\u0669' -> '0' + (ch - '\u0660')
                    '\u066B' -> '.'
                    '\u066C', '\u060C' -> ','
                    '\u2212' -> '-'
                    else -> ch
                },
            )
        }
        return out.toString()
    }

    /** Accepts grouping separators and surrounding spaces; returns null for anything that is not a plain decimal. */
    fun parseDecimal(text: String?): BigDecimal? {
        if (text == null) return null
        val cleaned = normalizeDigits(text).trim()
            .replace(",", "")
            .replace(" ", "")
            .replace("\u00A0", "")
            .replace("_", "")
        if (!decimalPattern.matches(cleaned)) return null
        return runCatching { BigDecimal(cleaned) }.getOrNull()
    }

    fun parseInt(text: String?): Int? {
        val value = parseDecimal(text) ?: return null
        return runCatching { value.intValueExact() }.getOrNull()
    }
}

fun String.toAmountOrNull(): Double? = Numbers.parseDecimal(this)?.toDouble()
