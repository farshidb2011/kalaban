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

    /**
     * Keeps an editable decimal skeleton: optional leading `-`, digits, at most one `.`.
     * Strips grouping separators so the field value stays plain for parsing.
     */
    fun sanitizeDecimalInput(text: String): String {
        val normalized = normalizeDigits(text)
            .replace(",", "")
            .replace(" ", "")
            .replace("\u00A0", "")
            .replace("_", "")
        if (normalized.isEmpty()) return ""
        val out = StringBuilder(normalized.length)
        var sawDot = false
        var sawDigit = false
        for ((index, ch) in normalized.withIndex()) {
            when (ch) {
                '-', '+' -> if (index == 0 && out.isEmpty()) out.append('-')
                '.' -> if (!sawDot) {
                    out.append('.')
                    sawDot = true
                }
                in '0'..'9' -> {
                    out.append(ch)
                    sawDigit = true
                }
            }
        }
        // Lone "-" or "." while typing is kept so the user can continue.
        if (!sawDigit && out.isEmpty()) return ""
        return out.toString()
    }

    /** Inserts `,` thousand separators into the integer part; preserves `-`, `.`, and a trailing decimal point. */
    fun groupThousands(plain: String): String {
        if (plain.isEmpty()) return plain
        val negative = plain.startsWith("-")
        val body = if (negative) plain.substring(1) else plain
        val dot = body.indexOf('.')
        val intRaw = if (dot >= 0) body.substring(0, dot) else body
        val frac = if (dot >= 0) body.substring(dot) else "" // includes the dot
        if (intRaw.isEmpty()) {
            return (if (negative) "-" else "") + frac
        }
        val grouped = buildString(intRaw.length + intRaw.length / 3) {
            var count = 0
            for (i in intRaw.lastIndex downTo 0) {
                if (count > 0 && count % 3 == 0) insert(0, ',')
                insert(0, intRaw[i])
                count++
            }
        }
        return (if (negative) "-" else "") + grouped + frac
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
