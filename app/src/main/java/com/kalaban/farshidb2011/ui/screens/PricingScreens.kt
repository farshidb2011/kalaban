package com.kalaban.farshidb2011.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Functions
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kalaban.farshidb2011.R
import com.kalaban.farshidb2011.core.JalaliDate
import com.kalaban.farshidb2011.core.Money
import com.kalaban.farshidb2011.core.Numbers
import com.kalaban.farshidb2011.core.formula.FormulaEngine
import com.kalaban.farshidb2011.core.pricing.PriceAdjustment
import com.kalaban.farshidb2011.core.pricing.PriceRounding
import com.kalaban.farshidb2011.data.local.entity.BatchStatus
import com.kalaban.farshidb2011.data.local.entity.DiscountRuleEntity
import com.kalaban.farshidb2011.data.local.entity.PriceSource
import com.kalaban.farshidb2011.data.repository.BulkOperation
import com.kalaban.farshidb2011.data.repository.BulkPreview
import com.kalaban.farshidb2011.domain.Permission
import com.kalaban.farshidb2011.ui.AppViewModel
import com.kalaban.farshidb2011.ui.PricingViewModel
import com.kalaban.farshidb2011.ui.components.ChoiceChips
import com.kalaban.farshidb2011.ui.components.ConfirmDialog
import com.kalaban.farshidb2011.ui.components.DropdownField
import com.kalaban.farshidb2011.ui.components.ExpandableSection
import com.kalaban.farshidb2011.ui.components.InfoBanner
import com.kalaban.farshidb2011.ui.components.JalaliDateField
import com.kalaban.farshidb2011.ui.components.KeyValueRow
import com.kalaban.farshidb2011.ui.components.LoadingBox
import com.kalaban.farshidb2011.ui.components.MoneyText
import com.kalaban.farshidb2011.ui.components.NumberField
import com.kalaban.farshidb2011.ui.components.RetailCard
import com.kalaban.farshidb2011.ui.components.RetailEmptyState
import com.kalaban.farshidb2011.ui.components.RetailOutlinedButton
import com.kalaban.farshidb2011.ui.components.RetailPrimaryButton
import com.kalaban.farshidb2011.ui.components.RetailTextField
import com.kalaban.farshidb2011.ui.components.RetailTopAppBar
import com.kalaban.farshidb2011.ui.components.SectionHeader
import com.kalaban.farshidb2011.ui.components.Spacing
import com.kalaban.farshidb2011.ui.components.StatusChip
import com.kalaban.farshidb2011.ui.components.Tone
import com.kalaban.farshidb2011.ui.components.retailContentWindowInsets
import com.kalaban.farshidb2011.ui.components.retailSwitchColors
import com.kalaban.farshidb2011.ui.components.retailListItemColors
import com.kalaban.farshidb2011.ui.theme.semantic
import java.math.BigDecimal
import java.util.Calendar

// ---------------------------------------------------------------- Formula list

@Composable
fun FormulasScreen(
    vm: PricingViewModel,
    appVm: AppViewModel,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onRecalculate: () -> Unit,
) {
    val formulas by vm.formulas.collectAsStateWithLifecycle()
    val categories by appVm.categories.collectAsStateWithLifecycle()
    val settings by appVm.settings.collectAsStateWithLifecycle()
    val fa = settings.languageTag.ifBlank { "fa" }.startsWith("fa")
    val canEdit = appVm.can(Permission.MANAGE_FORMULAS)
    var confirmDelete by remember { mutableStateOf<Long?>(null) }
    val copySuffix = stringResource(R.string.copy_suffix)

    Scaffold(
        topBar = { RetailTopAppBar(stringResource(R.string.pricing_formulas), onBack) },
        floatingActionButton = {
            if (canEdit) {
                FloatingActionButton(
                    onClick = { onEdit(0) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) { Icon(Icons.Outlined.Add, stringResource(R.string.new_formula)) }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        contentWindowInsets = retailContentWindowInsets(),
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(start = Spacing.l, end = Spacing.l, top = Spacing.l, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            item { InfoBanner(stringResource(R.string.formulas_intro), Tone.INFO) }
            if (appVm.can(Permission.MANAGE_PRICES)) {
                item {
                    InfoBanner(
                        stringResource(R.string.formulas_apply_hint),
                        Tone.NEUTRAL,
                        actionLabel = stringResource(R.string.recalculate_prices),
                        onAction = onRecalculate,
                    )
                }
            }
            if (formulas.isEmpty()) {
                item { RetailEmptyState(Icons.Outlined.Functions, stringResource(R.string.no_formulas)) }
            }
            items(formulas, key = { it.id }) { f ->
                RetailCard(Modifier.clickable(enabled = canEdit) { onEdit(f.id) }) {
                    Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(f.name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                            Switch(checked = f.active, onCheckedChange = { vm.setActive(f.id, it) }, enabled = canEdit, colors = retailSwitchColors())
                        }
                        Text(
                            f.expression,
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, textDirection = TextDirection.Ltr),
                            color = MaterialTheme.colorScheme.tertiary,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                        f.conditionExpr?.let {
                            Text(
                                "${stringResource(R.string.formula_condition)}: $it",
                                style = MaterialTheme.typography.bodySmall.copy(textDirection = TextDirection.Ltr),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                            StatusChip(stringResource(if (f.active) R.string.active else R.string.inactive), if (f.active) Tone.SUCCESS else Tone.NEUTRAL)
                            StatusChip(
                                categories.firstOrNull { it.id == f.categoryId }?.let { if (fa) it.nameFa else it.nameEn } ?: stringResource(R.string.all_categories),
                                Tone.INFO,
                            )
                            StatusChip("${stringResource(R.string.priority)}: ${f.priority}", Tone.NEUTRAL)
                            Spacer(Modifier.weight(1f))
                            if (canEdit) {
                                IconButton(onClick = { vm.duplicate(f.id, "${f.name} $copySuffix") }) {
                                    Icon(Icons.Outlined.ContentCopy, stringResource(R.string.duplicate), tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { confirmDelete = f.id }) {
                                    Icon(Icons.Outlined.Delete, stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    confirmDelete?.let { id ->
        ConfirmDialog(
            title = stringResource(R.string.delete_formula_title),
            text = stringResource(R.string.delete_formula_text),
            confirmLabel = stringResource(R.string.delete),
            destructive = true,
            onConfirm = {
                vm.deleteFormula(id)
                confirmDelete = null
            },
            onDismiss = { confirmDelete = null },
        )
    }
}

// ---------------------------------------------------------------- Formula editor

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FormulaEditorScreen(
    id: Long,
    vm: PricingViewModel,
    appVm: AppViewModel,
    onBack: () -> Unit,
) {
    val state by vm.editor.collectAsStateWithLifecycle()
    val variables by vm.variables.collectAsStateWithLifecycle()
    val test by vm.test.collectAsStateWithLifecycle()
    val categories by appVm.categories.collectAsStateWithLifecycle()
    val items by appVm.items.collectAsStateWithLifecycle()
    val settings by appVm.settings.collectAsStateWithLifecycle()
    val fa = settings.languageTag.ifBlank { "fa" }.startsWith("fa")
    var sampleId by rememberSaveable { mutableStateOf<Long?>(null) }
    var revertTarget by remember { mutableStateOf<Long?>(null) }
    var editingCondition by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(id) {
        vm.refreshVariables()
        vm.startFormula(id)
        appVm.setQuery("")
    }

    fun insert(token: String) = vm.updateFormula { s ->
        if (editingCondition) s.copy(condition = (s.condition.trimEnd() + " " + token).trimStart())
        else s.copy(expression = (s.expression.trimEnd() + " " + token).trimStart())
    }

    Scaffold(
        topBar = { RetailTopAppBar(stringResource(if (id == 0L) R.string.new_formula else R.string.edit_formula), onBack) },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        contentWindowInsets = retailContentWindowInsets(),
    ) { pad ->
        if (!state.loaded) {
            LoadingBox(Modifier.padding(pad))
            return@Scaffold
        }
        LazyColumn(
            Modifier.padding(pad).fillMaxSize().imePadding(),
            contentPadding = PaddingValues(Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            item {
                RetailCard {
                    Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        RetailTextField(
                            value = state.name,
                            onValueChange = { v -> vm.updateFormula { it.copy(name = v) } },
                            label = { Text(stringResource(R.string.formula_name) + " *") },
                            isError = state.nameMissing,
                        )
                        RetailTextField(
                            value = state.expression,
                            onValueChange = { v -> vm.updateFormula { it.copy(expression = v) } },
                            label = { Text(stringResource(R.string.formula_expression) + " *") },
                            singleLine = false,
                            maxLines = 6,
                            numericLtr = true,
                            isError = state.expressionError != null,
                            supportingText = state.expressionError?.let { formulaErrorText(it) }
                                ?: if (state.expression.isNotBlank()) stringResource(R.string.formula_valid) else stringResource(R.string.formula_expression_help),
                            modifier = Modifier.clickable { editingCondition = false },
                        )
                        RetailTextField(
                            value = state.condition,
                            onValueChange = { v ->
                                editingCondition = true
                                vm.updateFormula { it.copy(condition = v) }
                            },
                            label = { Text(stringResource(R.string.formula_condition_optional)) },
                            singleLine = false,
                            maxLines = 3,
                            numericLtr = true,
                            isError = state.conditionError != null,
                            supportingText = state.conditionError?.let { formulaErrorText(it) } ?: stringResource(R.string.formula_condition_help),
                        )
                        RetailTextField(
                            value = state.description,
                            onValueChange = { v -> vm.updateFormula { it.copy(description = v) } },
                            label = { Text(stringResource(R.string.description)) },
                            singleLine = false,
                            maxLines = 3,
                        )
                    }
                }
            }
            item {
                ExpandableSection(stringResource(R.string.formula_palette), subtitle = stringResource(R.string.formula_palette_help), initiallyExpanded = true) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.insert_into), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.padding(horizontal = Spacing.xs))
                        ChoiceChips(
                            options = listOf(false to stringResource(R.string.formula_expression), true to stringResource(R.string.formula_condition)),
                            selected = editingCondition,
                            onSelect = { editingCondition = it },
                        )
                    }
                    Text(stringResource(R.string.variables), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        variables.forEach { v -> AssistChip(onClick = { insert(v) }, label = { Text(v, style = MaterialTheme.typography.labelSmall) }) }
                    }
                    Text(stringResource(R.string.functions), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        FormulaEngine.functionNames.forEach { f -> AssistChip(onClick = { insert("$f(") }, label = { Text("$f()", style = MaterialTheme.typography.labelSmall) }) }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        listOf("+", "-", "*", "/", "^", "(", ")", ",", ">", "<", ">=", "<=", "==", "and", "or", "not").forEach { op ->
                            AssistChip(onClick = { insert(op) }, label = { Text(op) })
                        }
                    }
                    Text(stringResource(R.string.formula_examples), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                RetailCard {
                    Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        Text(stringResource(R.string.formula_scope), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        DropdownField(
                            label = stringResource(R.string.select_category),
                            options = listOf<Pair<Long?, String>>(null to stringResource(R.string.all_categories)) +
                                categories.map { it.id to (if (fa) it.nameFa else it.nameEn) },
                            selected = state.categoryId,
                            onSelect = { v -> vm.updateFormula { it.copy(categoryId = v) } },
                        )
                        RetailTextField(
                            value = state.priority,
                            onValueChange = { v -> vm.updateFormula { it.copy(priority = v) } },
                            label = { Text(stringResource(R.string.priority)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            numericLtr = true,
                            supportingText = stringResource(R.string.priority_help),
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.active), modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                            Switch(checked = state.active, onCheckedChange = { v -> vm.updateFormula { it.copy(active = v) } }, colors = retailSwitchColors())
                        }
                    }
                }
            }
            item {
                ExpandableSection(
                    stringResource(R.string.formula_limits),
                    subtitle = stringResource(R.string.formula_limits_help),
                    initiallyExpanded = state.minPrice.isNotBlank() || state.maxPrice.isNotBlank() || state.roundingMode.isNotBlank(),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        NumberField(state.minPrice, { v -> vm.updateFormula { it.copy(minPrice = v) } }, stringResource(R.string.min_price), Modifier.weight(1f))
                        NumberField(state.maxPrice, { v -> vm.updateFormula { it.copy(maxPrice = v) } }, stringResource(R.string.max_price), Modifier.weight(1f))
                    }
                    DropdownField(
                        label = stringResource(R.string.rounding_mode),
                        options = PricingViewModel.roundingOptions.map { it to roundingLabel(it) },
                        selected = state.roundingMode,
                        onSelect = { v -> vm.updateFormula { it.copy(roundingMode = v) } },
                    )
                    NumberField(state.roundingStep, { v -> vm.updateFormula { it.copy(roundingStep = v) } }, stringResource(R.string.rounding_step))
                }
            }
            item {
                RetailCard {
                    Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        Text(stringResource(R.string.formula_test), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        DropdownField(
                            label = stringResource(R.string.sample_product),
                            options = items.map<com.kalaban.farshidb2011.data.local.entity.ItemEntity, Pair<Long?, String>> { it.id to it.title },
                            selected = sampleId,
                            onSelect = { sampleId = it },
                        )
                        RetailOutlinedButton(
                            onClick = { items.firstOrNull { it.id == sampleId }?.let(vm::testFormula) },
                            enabled = sampleId != null && state.expression.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(R.string.run_test)) }
                        test?.let { t ->
                            KeyValueRow(stringResource(R.string.current_rule), t.current.formulaName ?: methodLabel(t.current.method))
                            KeyValueRow(stringResource(R.string.current_result), Money.format(t.current.finalPrice))
                            t.conditionMatched?.let {
                                KeyValueRow(stringResource(R.string.formula_condition), stringResource(if (it) R.string.condition_matched else R.string.condition_not_matched))
                            }
                            KeyValueRow(
                                stringResource(R.string.this_formula),
                                t.value?.let { Money.format(it) } ?: "—",
                                valueColor = if (t.error != null) MaterialTheme.colorScheme.error else MaterialTheme.semantic.success,
                                emphasize = true,
                            )
                            t.error?.let { Text(formulaErrorText(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                            if (t.comparisons.isNotEmpty()) {
                                Text(stringResource(R.string.compare_formulas), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                                t.comparisons.forEach { (name, value, error) ->
                                    KeyValueRow(name, value?.let { Money.format(it) } ?: (error?.let { formulaErrorText(it) } ?: "—"))
                                }
                            }
                            ExpandableSection(stringResource(R.string.variable_values)) {
                                t.variables.forEach { (k, v) -> KeyValueRow(k, Money.input(v)) }
                            }
                        }
                    }
                }
            }
            if (id != 0L) {
                item { FormulaVersions(vm, id) { revertTarget = it } }
            }
            item {
                RetailPrimaryButton(onClick = { vm.saveFormula { onBack() } }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text(stringResource(R.string.save))
                }
                TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.cancel)) }
            }
        }
    }
    revertTarget?.let { versionId ->
        ConfirmDialog(
            title = stringResource(R.string.restore_version_title),
            text = stringResource(R.string.restore_version_text),
            confirmLabel = stringResource(R.string.restore),
            onConfirm = {
                vm.revert(versionId)
                revertTarget = null
            },
            onDismiss = { revertTarget = null },
        )
    }
}

@Composable
private fun FormulaVersions(vm: PricingViewModel, formulaId: Long, onRestore: (Long) -> Unit) {
    val versions by vm.observeVersions(formulaId).collectAsState(initial = emptyList())
    if (versions.isEmpty()) return
    ExpandableSection(stringResource(R.string.formula_versions), subtitle = stringResource(R.string.versions_count, versions.size)) {
        versions.forEach { v ->
            ListItem(
                headlineContent = { Text("v${v.version} · ${JalaliDate.fromEpochMs(v.savedAtEpochMs).format()}", color = MaterialTheme.colorScheme.onSurface) },
                supportingContent = {
                    Text(v.expression, style = MaterialTheme.typography.bodySmall.copy(textDirection = TextDirection.Ltr), color = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                trailingContent = {
                    IconButton(onClick = { onRestore(v.id) }) { Icon(Icons.Outlined.Restore, stringResource(R.string.restore), tint = MaterialTheme.colorScheme.primary) }
                },
                colors = retailListItemColors(MaterialTheme.colorScheme.surface),
            )
        }
    }
}

@Composable
fun roundingLabel(mode: String): String = when (mode) {
    "" -> stringResource(R.string.rounding_global)
    PriceRounding.BANKERS.name -> stringResource(R.string.rounding_bankers)
    PriceRounding.UP.name -> stringResource(R.string.rounding_up)
    PriceRounding.DOWN.name -> stringResource(R.string.rounding_down)
    else -> stringResource(R.string.rounding_nearest)
}

// ---------------------------------------------------------------- Bulk price update

private enum class BulkOp { RECALC, PERCENT, AMOUNT, FACTOR, SET }

@Composable
fun BulkPriceScreen(vm: PricingViewModel, appVm: AppViewModel, onBack: () -> Unit, onHistory: () -> Unit) {
    val request by vm.request.collectAsStateWithLifecycle()
    val preview by vm.preview.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val categories by appVm.categories.collectAsStateWithLifecycle()
    val settings by appVm.settings.collectAsStateWithLifecycle()
    val fa = settings.languageTag.ifBlank { "fa" }.startsWith("fa")
    val showCosts = appVm.can(Permission.VIEW_COSTS)
    var value by rememberSaveable { mutableStateOf("") }
    var confirmApply by remember { mutableStateOf(false) }
    var scheduleOpen by remember { mutableStateOf(false) }

    val op = when (val o = request.operation) {
        BulkOperation.Recalculate -> BulkOp.RECALC
        is BulkOperation.Adjust -> when (o.adjustment) {
            is PriceAdjustment.Percent -> BulkOp.PERCENT
            is PriceAdjustment.Amount -> BulkOp.AMOUNT
            is PriceAdjustment.Factor -> BulkOp.FACTOR
            is PriceAdjustment.SetPrice -> BulkOp.SET
        }
    }
    fun setOp(newOp: BulkOp, text: String = value) {
        val v = Numbers.parseDecimal(text) ?: BigDecimal.ZERO
        vm.updateRequest {
            it.copy(
                operation = when (newOp) {
                    BulkOp.RECALC -> BulkOperation.Recalculate
                    BulkOp.PERCENT -> BulkOperation.Adjust(PriceAdjustment.Percent(v))
                    BulkOp.AMOUNT -> BulkOperation.Adjust(PriceAdjustment.Amount(v))
                    BulkOp.FACTOR -> BulkOperation.Adjust(PriceAdjustment.Factor(if (text.isBlank()) BigDecimal.ONE else v))
                    BulkOp.SET -> BulkOperation.Adjust(PriceAdjustment.SetPrice(v))
                },
            )
        }
    }
    val scopeLabel = when {
        request.scope.itemIds != null -> stringResource(R.string.selected_count, request.scope.itemIds!!.size)
        request.scope.categoryId != null -> categories.firstOrNull { it.id == request.scope.categoryId }?.let { if (fa) it.nameFa else it.nameEn }.orEmpty()
        else -> stringResource(R.string.all_products)
    }
    val title = when (op) {
        BulkOp.RECALC -> stringResource(R.string.batch_title_recalc, scopeLabel)
        BulkOp.PERCENT -> stringResource(R.string.batch_title_percent, value, scopeLabel)
        BulkOp.AMOUNT -> stringResource(R.string.batch_title_amount, value, scopeLabel)
        BulkOp.FACTOR -> stringResource(R.string.batch_title_factor, value, scopeLabel)
        BulkOp.SET -> stringResource(R.string.batch_title_set, value, scopeLabel)
    }

    Scaffold(
        topBar = {
            RetailTopAppBar(stringResource(R.string.bulk_price_update), onBack, actions = {
                IconButton(onClick = onHistory) { Icon(Icons.Outlined.History, stringResource(R.string.price_batches)) }
            })
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        contentWindowInsets = retailContentWindowInsets(),
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize().imePadding(),
            contentPadding = PaddingValues(Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            item {
                RetailCard {
                    Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        Text(stringResource(R.string.bulk_scope), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        if (request.scope.itemIds != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(scopeLabel, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                                TextButton(onClick = { vm.updateRequest { it.copy(scope = it.scope.copy(itemIds = null)) } }) {
                                    Text(stringResource(R.string.use_all_products))
                                }
                            }
                        } else {
                            DropdownField(
                                label = stringResource(R.string.select_category),
                                options = listOf<Pair<Long?, String>>(null to stringResource(R.string.all_products)) +
                                    categories.map { it.id to (if (fa) it.nameFa else it.nameEn) },
                                selected = request.scope.categoryId,
                                onSelect = { v -> vm.updateRequest { it.copy(scope = it.scope.copy(categoryId = v)) } },
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.include_archived), modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                            Switch(
                                checked = request.scope.includeArchived,
                                onCheckedChange = { v -> vm.updateRequest { it.copy(scope = it.scope.copy(includeArchived = v)) } },
                                colors = retailSwitchColors(),
                            )
                        }
                    }
                }
            }
            item {
                RetailCard {
                    Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        Text(stringResource(R.string.bulk_operation), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        ChoiceChips(
                            options = listOf(
                                BulkOp.RECALC to stringResource(R.string.op_recalculate),
                                BulkOp.PERCENT to stringResource(R.string.op_percent),
                                BulkOp.AMOUNT to stringResource(R.string.op_amount),
                                BulkOp.FACTOR to stringResource(R.string.op_factor),
                                BulkOp.SET to stringResource(R.string.op_set),
                            ),
                            selected = op,
                            onSelect = { setOp(it) },
                        )
                        Text(
                            stringResource(
                                when (op) {
                                    BulkOp.RECALC -> R.string.op_recalculate_help
                                    BulkOp.PERCENT -> R.string.op_percent_help
                                    BulkOp.AMOUNT -> R.string.op_amount_help
                                    BulkOp.FACTOR -> R.string.op_factor_help
                                    BulkOp.SET -> R.string.op_set_help
                                },
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (op != BulkOp.RECALC) {
                            NumberField(
                                value,
                                { v ->
                                    value = v
                                    setOp(op, v)
                                },
                                stringResource(R.string.value),
                                error = value.isNotBlank() && Numbers.parseDecimal(value) == null,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.apply_rounding), modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                                Switch(checked = request.applyRounding, onCheckedChange = { v -> vm.updateRequest { it.copy(applyRounding = v) } }, colors = retailSwitchColors())
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.respect_bounds), modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                                Switch(checked = request.respectBounds, onCheckedChange = { v -> vm.updateRequest { it.copy(respectBounds = v) } }, colors = retailSwitchColors())
                            }
                        }
                        RetailPrimaryButton(onClick = vm::runPreview, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.preview_changes))
                        }
                    }
                }
            }
            if (busy) item { LoadingBox() }
            preview?.let { p ->
                previewItems(p, showCosts, settings.currencyCode)
                if (p.affected > 0) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                            RetailPrimaryButton(onClick = { confirmApply = true }, enabled = !busy, modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.apply_changes, p.affected))
                            }
                            RetailOutlinedButton(onClick = { scheduleOpen = true }, enabled = !busy, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Outlined.Schedule, contentDescription = null)
                                Spacer(Modifier.padding(horizontal = 2.dp))
                                Text(stringResource(R.string.schedule))
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(48.dp)) }
        }
    }
    if (confirmApply) {
        ConfirmDialog(
            title = stringResource(R.string.apply_changes, preview?.affected ?: 0),
            text = stringResource(R.string.apply_changes_text),
            confirmLabel = stringResource(R.string.apply),
            onConfirm = {
                confirmApply = false
                vm.applyPreview(title, if (op == BulkOp.RECALC) PriceSource.RECALCULATION else PriceSource.BULK)
            },
            onDismiss = { confirmApply = false },
        )
    }
    if (scheduleOpen) {
        ScheduleDialog(onDismiss = { scheduleOpen = false }) { at ->
            scheduleOpen = false
            vm.schedule(at, title)
        }
    }
}

/** Summary plus per-product rows of a bulk preview (shared by bulk updates and rollbacks). */
fun androidx.compose.foundation.lazy.LazyListScope.previewItems(p: BulkPreview, showCosts: Boolean, currency: String) {
    item {
        RetailCard {
            Column(Modifier.padding(Spacing.l)) {
                Text(stringResource(R.string.impact_summary), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                KeyValueRow(stringResource(R.string.affected_products), p.affected.toString(), emphasize = true)
                KeyValueRow(stringResource(R.string.unchanged_products), p.unchanged.toString())
                if (p.invalid > 0) KeyValueRow(stringResource(R.string.invalid_products), p.invalid.toString(), valueColor = MaterialTheme.colorScheme.error)
                if (p.conflicts.isNotEmpty()) KeyValueRow(stringResource(R.string.conflicting_products), p.conflicts.size.toString(), valueColor = MaterialTheme.colorScheme.error)
                p.averageChangePct?.let { KeyValueRow(stringResource(R.string.average_change), Money.percent(it)) }
                KeyValueRow(stringResource(R.string.total_difference), Money.format(p.totalDifference) + " $currency")
                if (showCosts) {
                    KeyValueRow(
                        stringResource(R.string.profit_impact_stock),
                        Money.format(p.profitImpact),
                        valueColor = if (p.profitImpact.signum() < 0) MaterialTheme.colorScheme.error else MaterialTheme.semantic.success,
                    )
                    if (p.belowCost > 0) KeyValueRow(stringResource(R.string.below_cost_count), p.belowCost.toString(), valueColor = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
    if (p.affected == 0) {
        item { InfoBanner(stringResource(R.string.no_changes_needed), Tone.SUCCESS) }
        return
    }
    items(p.rows.take(300), key = { "row-${it.itemId}" }) { r ->
        ListItem(
            headlineContent = { Text(r.title, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface) },
            supportingContent = {
                Column {
                    Text(
                        "${Money.format(r.oldPrice)} → ${Money.format(r.newPrice)}" + (r.changePct?.let { "  (${if (it.signum() > 0) "+" else ""}${Money.percent(it)})" } ?: ""),
                        style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Ltr),
                        color = if (r.difference.signum() < 0) MaterialTheme.colorScheme.error else MaterialTheme.semantic.success,
                    )
                    if (showCosts) {
                        Text(
                            "${stringResource(R.string.margin)}: ${r.oldMarginPct?.let { Money.percent(it) } ?: "—"} → ${r.newMarginPct?.let { Money.percent(it) } ?: "—"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    r.warnings.forEach { Text("• " + warningLabel(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.semantic.warning) }
                }
            },
            colors = retailListItemColors(MaterialTheme.colorScheme.background),
        )
    }
    if (p.rows.size > 300) {
        item { Text(stringResource(R.string.more_rows, p.rows.size - 300), color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun ScheduleDialog(onDismiss: () -> Unit, onSchedule: (Long) -> Unit) {
    var date by remember { mutableStateOf(JalaliDate.today()) }
    var hour by remember { mutableStateOf("9") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(stringResource(R.string.schedule_change)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                JalaliDateField(stringResource(R.string.date), date, { date = it })
                NumberField(hour, { hour = it }, stringResource(R.string.hour_of_day), error = Numbers.parseInt(hour)?.let { it !in 0..23 } ?: true)
                Text(stringResource(R.string.schedule_help), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val h = Numbers.parseInt(hour)?.takeIf { it in 0..23 } ?: return@TextButton
                val noon = date.toGregorianEpochMs()
                val at = Calendar.getInstance().apply {
                    timeInMillis = noon
                    set(Calendar.HOUR_OF_DAY, h)
                    set(Calendar.MINUTE, 0)
                }.timeInMillis
                onSchedule(at)
            }) { Text(stringResource(R.string.schedule)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

// ---------------------------------------------------------------- Batch history

@Composable
fun PriceBatchesScreen(vm: PricingViewModel, appVm: AppViewModel, onBack: () -> Unit, onOpen: (Long) -> Unit) {
    val batches by vm.batches.collectAsStateWithLifecycle()
    var cancelTarget by remember { mutableStateOf<Long?>(null) }
    Scaffold(
        topBar = { RetailTopAppBar(stringResource(R.string.price_batches), onBack) },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        contentWindowInsets = retailContentWindowInsets(),
    ) { pad ->
        if (batches.isEmpty()) {
            RetailEmptyState(Icons.Outlined.History, stringResource(R.string.no_price_batches), Modifier.padding(pad))
            return@Scaffold
        }
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(Spacing.l)) {
            items(batches, key = { it.id }) { b ->
                RetailCard(Modifier.clickable(enabled = b.status != BatchStatus.SCHEDULED) { onOpen(b.id) }) {
                    ListItem(
                        headlineContent = { Text(b.title, color = MaterialTheme.colorScheme.onSurface) },
                        supportingContent = {
                            Column {
                                val whenText = if (b.status == BatchStatus.SCHEDULED && b.scheduledAtEpochMs != null) {
                                    stringResource(R.string.scheduled_for, JalaliDate.fromEpochMs(b.scheduledAtEpochMs).format())
                                } else {
                                    JalaliDate.fromEpochMs(b.createdAtEpochMs).format()
                                }
                                Text(
                                    "$whenText · ${priceSourceLabel(b.source)} · ${stringResource(R.string.products_count, b.affectedCount)}" +
                                        (b.actor?.let { " · ${actorLabel(it)}" } ?: ""),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                StatusChip(
                                    batchStatusLabel(b.status),
                                    when (b.status) {
                                        BatchStatus.APPLIED -> Tone.SUCCESS
                                        BatchStatus.SCHEDULED -> Tone.INFO
                                        BatchStatus.FAILED -> Tone.DANGER
                                        else -> Tone.NEUTRAL
                                    },
                                    Modifier.padding(top = 4.dp),
                                )
                            }
                        },
                        trailingContent = {
                            if (b.status == BatchStatus.SCHEDULED && appVm.can(Permission.MANAGE_PRICES)) {
                                TextButton(onClick = { cancelTarget = b.id }) { Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.error) }
                            }
                        },
                        colors = retailListItemColors(MaterialTheme.colorScheme.surface),
                    )
                }
            }
        }
    }
    cancelTarget?.let { id ->
        ConfirmDialog(
            title = stringResource(R.string.cancel_schedule_title),
            text = null,
            confirmLabel = stringResource(R.string.cancel_schedule),
            destructive = true,
            onConfirm = {
                vm.cancelScheduled(id)
                cancelTarget = null
            },
            onDismiss = { cancelTarget = null },
        )
    }
}

@Composable
fun PriceBatchScreen(id: Long, vm: PricingViewModel, appVm: AppViewModel, onBack: () -> Unit) {
    val batches by vm.batches.collectAsStateWithLifecycle()
    val rows by vm.batchRows.collectAsStateWithLifecycle()
    val preview by vm.preview.collectAsStateWithLifecycle()
    val settings by appVm.settings.collectAsStateWithLifecycle()
    val batch = batches.firstOrNull { it.id == id }
    var confirm by remember { mutableStateOf(false) }
    LaunchedEffect(id) {
        vm.clearPreview()
        vm.loadBatch(id)
    }
    val rollbackTitle = stringResource(R.string.rollback_title, id)
    Scaffold(
        topBar = { RetailTopAppBar(batch?.title ?: stringResource(R.string.price_batches), onBack) },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        contentWindowInsets = retailContentWindowInsets(),
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            batch?.let { b ->
                item {
                    RetailCard {
                        Column(Modifier.padding(Spacing.l)) {
                            KeyValueRow(stringResource(R.string.date), JalaliDate.fromEpochMs(b.createdAtEpochMs).format())
                            KeyValueRow(stringResource(R.string.source), priceSourceLabel(b.source))
                            KeyValueRow(stringResource(R.string.status), batchStatusLabel(b.status))
                            KeyValueRow(stringResource(R.string.affected_products), b.affectedCount.toString())
                            b.actor?.let { KeyValueRow(stringResource(R.string.user), actorLabel(it)) }
                        }
                    }
                }
                if (b.status == BatchStatus.APPLIED && appVm.can(Permission.MANAGE_PRICES) && preview == null) {
                    item {
                        RetailOutlinedButton(onClick = { vm.previewRollback(id) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Outlined.Restore, contentDescription = null)
                            Spacer(Modifier.padding(horizontal = 2.dp))
                            Text(stringResource(R.string.preview_rollback))
                        }
                    }
                }
            }
            val p = preview
            if (p != null) {
                item { SectionHeader(stringResource(R.string.rollback_preview)) }
                previewItems(p, appVm.can(Permission.VIEW_COSTS), settings.currencyCode)
                if (p.affected > 0) {
                    item {
                        RetailPrimaryButton(onClick = { confirm = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.rollback_apply, p.affected))
                        }
                    }
                }
            } else {
                item { SectionHeader(stringResource(R.string.changed_products)) }
                items(rows, key = { it.first.id }) { (h, title) ->
                    ListItem(
                        headlineContent = { Text(title, color = MaterialTheme.colorScheme.onSurface) },
                        supportingContent = {
                            Text(
                                (h.oldSellPrice?.let { Money.format(it) + " → " } ?: "") + Money.format(h.newSellPrice),
                                style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Ltr),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        colors = retailListItemColors(MaterialTheme.colorScheme.background),
                    )
                }
            }
        }
    }
    if (confirm) {
        ConfirmDialog(
            title = stringResource(R.string.rollback_apply, preview?.affected ?: 0),
            text = stringResource(R.string.rollback_text),
            confirmLabel = stringResource(R.string.restore),
            onConfirm = {
                confirm = false
                vm.rollback(id, rollbackTitle) { vm.loadBatch(id) }
            },
            onDismiss = { confirm = false },
        )
    }
}

// ---------------------------------------------------------------- Quantity discounts

@Composable
fun DiscountRulesScreen(vm: PricingViewModel, appVm: AppViewModel, onBack: () -> Unit) {
    val rules by vm.discountRules.collectAsStateWithLifecycle()
    val categories by appVm.categories.collectAsStateWithLifecycle()
    val settings by appVm.settings.collectAsStateWithLifecycle()
    val fa = settings.languageTag.ifBlank { "fa" }.startsWith("fa")
    var editing by remember { mutableStateOf<DiscountRuleEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<DiscountRuleEntity?>(null) }
    val canEdit = appVm.can(Permission.MANAGE_PRICES)
    Scaffold(
        topBar = { RetailTopAppBar(stringResource(R.string.quantity_discounts), onBack) },
        floatingActionButton = {
            if (canEdit) {
                FloatingActionButton(
                    onClick = { editing = DiscountRuleEntity(name = "", minQty = 10.0, discountPercent = 5.0) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) { Icon(Icons.Outlined.Add, stringResource(R.string.add)) }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        contentWindowInsets = retailContentWindowInsets(),
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            item { InfoBanner(stringResource(R.string.discounts_intro), Tone.INFO) }
            if (rules.isEmpty()) item { RetailEmptyState(Icons.Outlined.Percent, stringResource(R.string.no_discounts)) }
            items(rules, key = { it.id }) { r ->
                RetailCard(Modifier.clickable(enabled = canEdit) { editing = r }) {
                    ListItem(
                        headlineContent = { Text(r.name.ifBlank { "—" }, color = MaterialTheme.colorScheme.onSurface) },
                        supportingContent = {
                            Text(
                                stringResource(R.string.discount_rule_summary, Money.input(r.minQty), Money.input(r.discountPercent)) + " · " +
                                    (categories.firstOrNull { it.id == r.categoryId }?.let { if (fa) it.nameFa else it.nameEn } ?: stringResource(R.string.all_categories)),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(checked = r.active, onCheckedChange = { vm.saveDiscountRule(r.copy(active = it)) }, enabled = canEdit, colors = retailSwitchColors())
                                if (canEdit) {
                                    IconButton(onClick = { deleteTarget = r }) { Icon(Icons.Outlined.Delete, stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error) }
                                }
                            }
                        },
                        colors = retailListItemColors(MaterialTheme.colorScheme.surface),
                    )
                }
            }
        }
    }
    editing?.let { rule ->
        var name by remember(rule) { mutableStateOf(rule.name) }
        var qty by remember(rule) { mutableStateOf(Money.input(rule.minQty)) }
        var pct by remember(rule) { mutableStateOf(Money.input(rule.discountPercent)) }
        var category by remember(rule) { mutableStateOf(rule.categoryId) }
        AlertDialog(
            onDismissRequest = { editing = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(stringResource(R.string.quantity_discount)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    RetailTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.name)) })
                    DropdownField(
                        label = stringResource(R.string.select_category),
                        options = listOf<Pair<Long?, String>>(null to stringResource(R.string.all_categories)) + categories.map { it.id to (if (fa) it.nameFa else it.nameEn) },
                        selected = category,
                        onSelect = { category = it },
                    )
                    NumberField(qty, { qty = it }, stringResource(R.string.min_quantity), error = Numbers.parseDecimal(qty) == null)
                    NumberField(pct, { pct = it }, stringResource(R.string.discount_percent), error = Numbers.parseDecimal(pct) == null)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.saveDiscountRule(
                        rule.copy(
                            name = name.trim(),
                            categoryId = category,
                            minQty = Numbers.parseDecimal(qty)?.toDouble() ?: 0.0,
                            discountPercent = Numbers.parseDecimal(pct)?.toDouble() ?: 0.0,
                        ),
                    )
                    editing = null
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    deleteTarget?.let { r ->
        ConfirmDialog(
            title = stringResource(R.string.confirm_delete),
            text = r.name,
            confirmLabel = stringResource(R.string.delete),
            destructive = true,
            onConfirm = {
                vm.deleteDiscountRule(r)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null },
        )
    }
}