package com.kalaban.farshidb2011.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kalaban.farshidb2011.R
import com.kalaban.farshidb2011.core.JalaliDate
import com.kalaban.farshidb2011.core.Money
import com.kalaban.farshidb2011.ui.theme.semantic
import io.github.alirezajavan.shamsipicker.model.ShamsiDate
import io.github.alirezajavan.shamsipicker.model.ShamsiDatePickerConfig
import io.github.alirezajavan.shamsipicker.model.ShamsiDateRangePickerConfig
import io.github.alirezajavan.shamsipicker.ui.ShamsiDatePickerDialog
import io.github.alirezajavan.shamsipicker.ui.ShamsiDateRangePickerDialog
import java.math.BigDecimal

object Spacing {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 24.dp
}

enum class Tone { NEUTRAL, SUCCESS, WARNING, DANGER, INFO }

@Composable
fun toneColors(tone: Tone): Pair<Color, Color> {
    val s = MaterialTheme.semantic
    val c = MaterialTheme.colorScheme
    return when (tone) {
        Tone.NEUTRAL -> c.surfaceVariant to c.onSurfaceVariant
        Tone.SUCCESS -> s.successContainer to s.onSuccessContainer
        Tone.WARNING -> s.warningContainer to s.onWarningContainer
        Tone.DANGER -> c.errorContainer to c.onErrorContainer
        Tone.INFO -> s.infoContainer to s.onInfoContainer
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = Spacing.s, bottom = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        action?.invoke()
    }
}

/** KPI tile: icon, short label and a prominent value. */
@Composable
fun MetricCard(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tone: Tone = Tone.NEUTRAL,
    onClick: (() -> Unit)? = null,
) {
    val (container, content) = toneColors(tone)
    Surface(
        modifier = modifier
            .heightIn(min = 96.dp)
            .clip(RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        color = if (tone == Tone.NEUTRAL) MaterialTheme.colorScheme.surface else container,
        contentColor = if (tone == Tone.NEUTRAL) MaterialTheme.colorScheme.onSurface else content,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
    ) {
        Column(Modifier.padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                value,
                style = MaterialTheme.typography.titleLarge.copy(textDirection = TextDirection.Content),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun StatusChip(text: String, tone: Tone, modifier: Modifier = Modifier) {
    val (container, content) = toneColors(tone)
    Surface(color = container, contentColor = content, shape = RoundedCornerShape(50), modifier = modifier) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = Spacing.s, vertical = 2.dp),
            maxLines = 1,
        )
    }
}

@Composable
fun InfoBanner(
    text: String,
    tone: Tone,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val (container, content) = toneColors(tone)
    val icon = when (tone) {
        Tone.SUCCESS -> Icons.Outlined.CheckCircle
        Tone.WARNING -> Icons.Outlined.WarningAmber
        Tone.DANGER -> Icons.Outlined.ErrorOutline
        else -> Icons.Outlined.Info
    }
    Surface(color = container, contentColor = content, shape = RoundedCornerShape(14.dp), modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(Spacing.m), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(Spacing.s))
            Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            if (actionLabel != null && onAction != null) {
                TextButton(onClick = onAction) { Text(actionLabel, color = content, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String?,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = scheme.surface,
        titleContentColor = scheme.onSurface,
        textContentColor = scheme.onSurfaceVariant,
        title = { Text(title) },
        text = text?.let { { Text(it) } },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = if (destructive) scheme.error else scheme.primary, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = scheme.primary) }
        },
    )
}

/** Amount with currency; [signed] colors positives as profit and negatives as loss. */
@Composable
fun MoneyText(
    value: BigDecimal,
    modifier: Modifier = Modifier,
    currency: String? = null,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    signed: Boolean = false,
    color: Color = Color.Unspecified,
) {
    val tint = when {
        color != Color.Unspecified -> color
        !signed -> MaterialTheme.colorScheme.onSurface
        value.signum() < 0 -> MaterialTheme.colorScheme.error
        value.signum() > 0 -> MaterialTheme.semantic.success
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text = Money.format(value) + (currency?.let { " $it" } ?: ""),
        style = style.copy(textDirection = TextDirection.Ltr),
        color = tint,
        modifier = modifier,
        maxLines = 1,
    )
}

@Composable
fun KeyValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    emphasize: Boolean = false,
) {
    Row(modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            value,
            style = (if (emphasize) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium)
                .copy(textDirection = TextDirection.Content),
            fontWeight = if (emphasize) FontWeight.SemiBold else FontWeight.Normal,
            color = valueColor,
            textAlign = TextAlign.End,
        )
    }
}

/** Simple dependency-free bar chart; [labels] are drawn under the first, middle and last bars. */
@Composable
fun BarChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    labels: List<String> = emptyList(),
    color: Color = MaterialTheme.colorScheme.tertiary,
    description: String = "",
) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    Column(modifier.semantics { contentDescription = description }) {
        Canvas(Modifier.fillMaxWidth().height(140.dp)) {
            if (values.isEmpty()) return@Canvas
            val max = values.maxOrNull()?.takeIf { it > 0f } ?: 1f
            val gap = 4.dp.toPx()
            val barWidth = ((size.width - gap * (values.size - 1)) / values.size).coerceAtLeast(2f)
            values.forEachIndexed { i, v ->
                val x = i * (barWidth + gap)
                drawRoundRect(track, Offset(x, 0f), Size(barWidth, size.height), CornerRadius(6f, 6f))
                val h = (v.coerceAtLeast(0f) / max) * size.height
                drawRoundRect(color, Offset(x, size.height - h), Size(barWidth, h), CornerRadius(6f, 6f))
            }
        }
        if (labels.size >= 2) {
            Row(Modifier.fillMaxWidth().padding(top = Spacing.xs)) {
                Text(labels.first(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                if (labels.size > 2) {
                    Text(labels[labels.size / 2], style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.weight(1f))
                }
                Text(labels.last(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    RetailTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        label = { Text(label) },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.clear_search))
                }
            }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    )
}

@Composable
fun ExpandableSection(
    title: String,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    RetailCard(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(Spacing.l),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                if (!subtitle.isNullOrBlank()) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(
                if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AnimatedVisibility(expanded) {
            Column(
                Modifier.padding(start = Spacing.l, end = Spacing.l, bottom = Spacing.l),
                verticalArrangement = Arrangement.spacedBy(Spacing.s),
                content = content,
            )
        }
    }
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(Spacing.xl), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

/** Decimal input with Latin-digit LTR alignment; [error] highlights invalid values. */
@Composable
fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: Boolean = false,
    supporting: String? = null,
    enabled: Boolean = true,
) {
    RetailTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        numericLtr = true,
        isError = error,
        supportingText = supporting,
        enabled = enabled,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> DropdownField(
    label: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    var open by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = !open }, modifier = modifier) {
        OutlinedTextField(
            value = options.firstOrNull { it.first == selected }?.second.orEmpty(),
            onValueChange = {},
            readOnly = true,
            isError = isError,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = open) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth(),
            shape = RetailFieldShape,
            colors = retailTextFieldColors(),
            textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (value, text) ->
                DropdownMenuItem(
                    text = { Text(text, color = MaterialTheme.colorScheme.onSurface) },
                    onClick = {
                        onSelect(value)
                        open = false
                    },
                )
            }
        }
    }
}

@Composable
fun <T> ChoiceChips(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    LazyRow(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
        items(options.size) { i ->
            val (value, label) = options[i]
            RetailFilterChip(selected = value == selected, label = label, onClick = { onSelect(value) })
        }
    }
}

/** Read-only field that opens the Shamsi (Jalali) date picker. */
@Composable
fun JalaliDateField(
    label: String,
    value: JalaliDate.Date?,
    onPick: (JalaliDate.Date) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        RetailTextField(
            value = value?.format().orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) },
            numericLtr = true,
        )
        Box(Modifier.matchParentSize().clickable { open = true })
    }
    if (open) {
        val initial = value ?: JalaliDate.today()
        ShamsiDatePickerDialog(
            { picked: ShamsiDate ->
                onPick(JalaliDate.Date(picked.year, picked.month, picked.day))
                open = false
            },
            { open = false },
            ShamsiDatePickerConfig(initialDate = ShamsiDate(initial.year, initial.month, initial.day)),
        )
    }
}

/** Opens the Shamsi range picker and reports the chosen inclusive Jalali range. */
@Composable
fun JalaliRangePicker(open: Boolean, onDismiss: () -> Unit, onPick: (JalaliDate.Date, JalaliDate.Date) -> Unit) {
    if (!open) return
    ShamsiDateRangePickerDialog(
        { range ->
            onPick(
                JalaliDate.Date(range.from.year, range.from.month, range.from.day),
                JalaliDate.Date(range.to.year, range.to.month, range.to.day),
            )
        },
        onDismiss,
        ShamsiDateRangePickerConfig(),
    )
}
