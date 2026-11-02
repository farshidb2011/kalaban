package com.kalaban.farshidb2011.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.kalaban.farshidb2011.core.Money
import com.kalaban.farshidb2011.core.Numbers

/**
 * Shows thousand separators in decimal fields while keeping the underlying value plain
 * (no commas) so [Numbers.parseDecimal] and cursor edits stay simple.
 */
object DecimalGroupingTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val original = text.text
        val grouped = Numbers.groupThousands(original)
        if (grouped == original) {
            return TransformedText(text, OffsetMapping.Identity)
        }
        return TransformedText(AnnotatedString(grouped), DecimalGroupingOffsetMapping(original, grouped))
    }
}

private class DecimalGroupingOffsetMapping(
    private val original: String,
    private val transformed: String,
) : OffsetMapping {
    private val originalToTransformed: IntArray
    private val transformedToOriginal: IntArray

    init {
        originalToTransformed = IntArray(original.length + 1)
        transformedToOriginal = IntArray(transformed.length + 1)
        var o = 0
        var t = 0
        while (o <= original.length && t <= transformed.length) {
            originalToTransformed[o] = t
            transformedToOriginal[t] = o
            if (o == original.length || t == transformed.length) {
                // Fill any remaining transformed slots (trailing commas shouldn't happen).
                while (t < transformed.length) {
                    t++
                    transformedToOriginal[t] = o
                }
                while (o < original.length) {
                    o++
                    originalToTransformed[o] = t
                }
                break
            }
            when {
                transformed[t] == ',' && (o >= original.length || original[o] != ',') -> {
                    t++
                    transformedToOriginal[t] = o
                }
                else -> {
                    o++
                    t++
                }
            }
        }
        originalToTransformed[original.length] = transformed.length
        transformedToOriginal[transformed.length] = original.length
    }

    override fun originalToTransformed(offset: Int): Int =
        originalToTransformed[offset.coerceIn(0, original.length)]

    override fun transformedToOriginal(offset: Int): Int =
        transformedToOriginal[offset.coerceIn(0, transformed.length)]
}

@Composable
fun rememberDecimalGroupingTransformation(enabled: Boolean = Money.grouping): VisualTransformation {
    return remember(enabled) {
        if (enabled) DecimalGroupingTransformation else VisualTransformation.None
    }
}
