package com.kalaban.farshidb2011.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.kalaban.farshidb2011.R
import com.kalaban.farshidb2011.core.Money
import com.kalaban.farshidb2011.core.Numbers
import com.kalaban.farshidb2011.ui.theme.VazirFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val RetailFieldShape = RoundedCornerShape(14.dp)

@Composable
fun retailReadableOnSurface(): Color = MaterialTheme.colorScheme.onSurface

@Composable
fun retailReadableOnSurfaceVariant(): Color = MaterialTheme.colorScheme.onSurfaceVariant

/** Filled primary actions: always contrast against primary (white on ink in light). */
@Composable
fun retailPrimaryButtonColors() = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.primary,
    contentColor = MaterialTheme.colorScheme.onPrimary,
    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
    disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
)

@Composable
fun retailOutlinedButtonColors() = ButtonDefaults.outlinedButtonColors(
    contentColor = MaterialTheme.colorScheme.primary,
    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
)


/**
 * Scaffold content insets without IME (window SoftInput adjustResize handles keyboard).
 * [embedded] screens sit under a parent TopAppBar — skip top system insets to avoid a clipped gap.
 */
@Composable
fun retailContentWindowInsets(embedded: Boolean = false): WindowInsets {
    val base = WindowInsets.safeDrawing.exclude(WindowInsets.ime)
    return if (embedded) {
        base.only(WindowInsetsSides.Horizontal)
    } else {
        base
    }
}

/**
 * Finger-scrollable page body (no scrollbar track).
 * IME padding here is safe because [retailContentWindowInsets] excludes IME from Scaffold.
 */
@Composable
fun RetailScrollColumn(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
        content = content,
    )
}


/** Forces readable on-surface content regardless of leaked TopAppBar LocalContentColor. */
@Composable
fun RetailContent(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    CompositionLocalProvider(LocalContentColor provides scheme.onSurface) {
        Surface(
            modifier = modifier,
            color = Color.Transparent,
            contentColor = scheme.onSurface,
            content = content,
        )
    }
}

@Composable
fun RetailPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RetailFieldShape,
        colors = retailPrimaryButtonColors(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        content = {
            CompositionLocalProvider(
                LocalContentColor provides MaterialTheme.colorScheme.onPrimary,
                LocalTextStyle provides MaterialTheme.typography.labelLarge.copy(
                    color = Color.Unspecified,
                    fontFamily = VazirFontFamily,
                ),
            ) {
                content()
            }
        },
    )
}

@Composable
fun RetailOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RetailFieldShape,
        colors = retailOutlinedButtonColors(),
        border = BorderStroke(1.dp, scheme.primary),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        content = {
            CompositionLocalProvider(
                LocalContentColor provides scheme.primary,
                LocalTextStyle provides MaterialTheme.typography.labelLarge.copy(
                    color = Color.Unspecified,
                    fontFamily = VazirFontFamily,
                ),
            ) {
                content()
            }
        },
    )
}

@Composable
fun retailTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = retailReadableOnSurface(),
    unfocusedTextColor = retailReadableOnSurface(),
    disabledTextColor = retailReadableOnSurface().copy(alpha = 0.38f),
    errorTextColor = MaterialTheme.colorScheme.error,
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
    disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
    errorBorderColor = MaterialTheme.colorScheme.error,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    errorContainerColor = MaterialTheme.colorScheme.surface,
    focusedLabelColor = retailReadableOnSurfaceVariant(),
    unfocusedLabelColor = retailReadableOnSurfaceVariant(),
    disabledLabelColor = retailReadableOnSurfaceVariant().copy(alpha = 0.6f),
    focusedPlaceholderColor = retailReadableOnSurfaceVariant(),
    unfocusedPlaceholderColor = retailReadableOnSurfaceVariant(),
    focusedSupportingTextColor = retailReadableOnSurfaceVariant(),
    unfocusedSupportingTextColor = retailReadableOnSurfaceVariant(),
    focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
    unfocusedLeadingIconColor = retailReadableOnSurfaceVariant(),
)

/** List rows that never inherit TopAppBar white content color. */
@Composable
fun retailListItemColors(containerColor: Color = MaterialTheme.colorScheme.surface) =
    ListItemDefaults.colors(
        containerColor = containerColor,
        headlineColor = MaterialTheme.colorScheme.onSurface,
        supportingColor = MaterialTheme.colorScheme.onSurfaceVariant,
        leadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        trailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        overlineColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RetailTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    numericLtr: Boolean = false,
    /** When true (and settings grouping is on), show thousand separators while typing. */
    groupThousands: Boolean = false,
    isError: Boolean = false,
    supportingText: String? = null,
) {
    val onSurface = retailReadableOnSurface()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    val groupingTransform = rememberDecimalGroupingTransformation(enabled = groupThousands && Money.grouping)
    val effectiveTransform = if (groupThousands && Money.grouping && visualTransformation === VisualTransformation.None) {
        groupingTransform
    } else {
        visualTransformation
    }
    val fieldTextStyle = if (numericLtr) {
        TextStyle(
            fontFamily = VazirFontFamily,
            color = onSurface,
            textDirection = TextDirection.Ltr,
            textAlign = TextAlign.Start,
        )
    } else {
        TextStyle(fontFamily = VazirFontFamily, color = onSurface)
    }
    CompositionLocalProvider(LocalContentColor provides onSurface) {
        OutlinedTextField(
            value = value,
            onValueChange = { incoming ->
                onValueChange(if (groupThousands) Numbers.sanitizeDecimalInput(incoming) else incoming)
            },
            modifier = modifier
                .fillMaxWidth()
                .bringIntoViewRequester(bringIntoViewRequester)
                .onFocusEvent { state ->
                    if (state.isFocused) {
                        scope.launch {
                            // Wait for IME animation / window resize, then scroll field into view.
                            delay(280)
                            bringIntoViewRequester.bringIntoView()
                        }
                    }
                },
            enabled = enabled,
            readOnly = readOnly,
            textStyle = fieldTextStyle,
            label = label,
            placeholder = placeholder,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            visualTransformation = effectiveTransform,
            keyboardOptions = keyboardOptions,
            singleLine = singleLine,
            maxLines = maxLines,
            isError = isError,
            supportingText = supportingText?.let { text -> { Text(text) } },
            shape = RetailFieldShape,
            colors = retailTextFieldColors(),
        )
    }
}

@Composable
fun RetailCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        content = content,
    )
}

@Composable
fun RetailDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(horizontal = 4.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
    )
}


@Composable
fun RetailBottomTab(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fg = if (selected) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(22.dp)
                .height(3.dp)
                .clip(CircleShape)
                .background(if (selected) MaterialTheme.colorScheme.tertiary else Color.Transparent),
        )
    }
}

/**
 * Category / filter chip that never relies on Material3 FilterChip LocalContentColor.
 * FilterChip was painting selected labels white (TopAppBar onPrimary leak) on light cream.
 */
@Composable
fun RetailFilterChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val container = if (selected) scheme.secondaryContainer else scheme.surface
    val labelColor = if (selected) scheme.onSecondaryContainer else scheme.onSurface
    val borderColor = if (selected) scheme.tertiary else scheme.outline.copy(alpha = 0.45f)
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(container)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = labelColor,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RetailTopAppBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleLarge,
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        },
        actions = actions,
    )
}

@Composable
fun RetailEmptyState(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(72.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun retailSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
    checkedTrackColor = MaterialTheme.colorScheme.primary,
    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
    uncheckedBorderColor = MaterialTheme.colorScheme.outline,
)

@Composable
fun HelpIconButton(helpText: String) {
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = { open = true }, modifier = Modifier.size(48.dp)) {
        Icon(
            Icons.AutoMirrored.Outlined.HelpOutline,
            contentDescription = helpText,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (open) {
        val scheme = MaterialTheme.colorScheme
        AlertDialog(
            onDismissRequest = { open = false },
            containerColor = scheme.surface,
            titleContentColor = scheme.onSurface,
            textContentColor = scheme.onSurface,
            confirmButton = {
                TextButton(onClick = { open = false }) {
                    Text(
                        androidx.compose.ui.res.stringResource(R.string.ok),
                        color = scheme.primary,
                    )
                }
            },
            text = {
                CompositionLocalProvider(LocalContentColor provides scheme.onSurface) {
                    Text(helpText, color = scheme.onSurface)
                }
            },
        )
    }
}

