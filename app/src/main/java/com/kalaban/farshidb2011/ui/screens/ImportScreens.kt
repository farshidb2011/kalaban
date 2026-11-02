package com.kalaban.farshidb2011.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.Icon
import com.kalaban.farshidb2011.R
import com.kalaban.farshidb2011.data.repository.DuplicatePolicy
import com.kalaban.farshidb2011.data.repository.ImportField
import com.kalaban.farshidb2011.data.repository.ImportIssue
import com.kalaban.farshidb2011.data.repository.ImportRow
import com.kalaban.farshidb2011.data.repository.ImportRowStatus
import com.kalaban.farshidb2011.domain.Permission
import com.kalaban.farshidb2011.ui.AppViewModel
import com.kalaban.farshidb2011.ui.ImportStep
import com.kalaban.farshidb2011.ui.ImportViewModel
import com.kalaban.farshidb2011.ui.components.ChoiceChips
import com.kalaban.farshidb2011.ui.components.ConfirmDialog
import com.kalaban.farshidb2011.ui.components.DropdownField
import com.kalaban.farshidb2011.ui.components.InfoBanner
import com.kalaban.farshidb2011.ui.components.KeyValueRow
import com.kalaban.farshidb2011.ui.components.LoadingBox
import com.kalaban.farshidb2011.ui.components.RetailCard
import com.kalaban.farshidb2011.ui.components.RetailOutlinedButton
import com.kalaban.farshidb2011.ui.components.RetailPrimaryButton
import com.kalaban.farshidb2011.ui.components.RetailTopAppBar
import com.kalaban.farshidb2011.ui.components.SectionHeader
import com.kalaban.farshidb2011.ui.components.Spacing
import com.kalaban.farshidb2011.ui.components.StatusChip
import com.kalaban.farshidb2011.ui.components.Tone
import com.kalaban.farshidb2011.ui.components.retailContentWindowInsets
import com.kalaban.farshidb2011.ui.components.retailSwitchColors
import com.kalaban.farshidb2011.ui.components.retailListItemColors

@Composable
fun ImportScreen(vm: ImportViewModel, appVm: AppViewModel, onBack: () -> Unit, onOpenBatches: () -> Unit) {
    val step by vm.step.collectAsStateWithLifecycle()
    val parsed by vm.parsed.collectAsStateWithLifecycle()
    val options by vm.options.collectAsStateWithLifecycle()
    val preview by vm.preview.collectAsStateWithLifecycle()
    val result by vm.result.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val units by appVm.units.collectAsStateWithLifecycle()
    val settings by appVm.settings.collectAsStateWithLifecycle()
    val fa = settings.languageTag.ifBlank { "fa" }.startsWith("fa")
    var confirm by remember { mutableStateOf(false) }

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) vm.load(uri) }
    val exportProducts = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri -> if (uri != null) vm.exportProducts(uri) }
    val template = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri -> if (uri != null) vm.writeTemplate(uri) }
    val issueLabels = ImportIssue.entries.associateWith { importIssueText(it) }
    val fieldLabels = ImportField.entries.associateWith { importFieldLabel(it) }
    val errors = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            vm.exportErrors(uri) { row -> row.issues.joinToString("; ") { (issue, field) -> issueLabels.getValue(issue) + (field?.let { " (${fieldLabels.getValue(it)})" } ?: "") } }
        }
    }
    val importTitle = stringResource(R.string.import_batch_title)

    Scaffold(
        topBar = { RetailTopAppBar(stringResource(R.string.import_export), onBack = if (step == ImportStep.PICK || step == ImportStep.DONE) onBack else vm::back) },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        contentWindowInsets = retailContentWindowInsets(),
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
            if (busy) item { LoadingBox() }
            when (step) {
                ImportStep.PICK -> {
                    if (appVm.can(Permission.IMPORT_DATA)) {
                        item {
                            RetailCard {
                                Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                                    Text(stringResource(R.string.import_products), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                                    Text(stringResource(R.string.import_intro), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    RetailPrimaryButton(onClick = { pick.launch(arrayOf("text/*", "application/csv", "application/vnd.ms-excel", "*/*")) }, modifier = Modifier.fillMaxWidth()) {
                                        Icon(Icons.Outlined.UploadFile, contentDescription = null)
                                        Spacer(Modifier.padding(horizontal = 2.dp))
                                        Text(stringResource(R.string.choose_csv))
                                    }
                                    RetailOutlinedButton(onClick = { template.launch("kalaban-import-template.csv") }, modifier = Modifier.fillMaxWidth()) {
                                        Icon(Icons.Outlined.Description, contentDescription = null)
                                        Spacer(Modifier.padding(horizontal = 2.dp))
                                        Text(stringResource(R.string.download_template))
                                    }
                                }
                            }
                        }
                        item { InfoBanner(stringResource(R.string.excel_note), Tone.INFO) }
                    }
                    if (appVm.can(Permission.EXPORT_DATA)) {
                        item {
                            RetailCard {
                                Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                                    Text(stringResource(R.string.export_products), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                                    Text(stringResource(R.string.export_products_help), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    RetailOutlinedButton(onClick = { exportProducts.launch("kalaban-products.csv") }, modifier = Modifier.fillMaxWidth()) {
                                        Icon(Icons.Outlined.FileDownload, contentDescription = null)
                                        Spacer(Modifier.padding(horizontal = 2.dp))
                                        Text(stringResource(R.string.export_csv))
                                    }
                                }
                            }
                        }
                    }
                }
                ImportStep.MAP -> {
                    val header = parsed?.header.orEmpty()
                    item {
                        InfoBanner(stringResource(R.string.import_file_summary, parsed?.rows?.size ?: 0, header.size), Tone.INFO)
                    }
                    item { SectionHeader(stringResource(R.string.column_mapping)) }
                    item {
                        RetailCard {
                            Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                                val columnOptions = listOf<Pair<Int?, String>>(null to stringResource(R.string.not_mapped)) + header.mapIndexed { i, h -> i to "${i + 1}. $h" }
                                ImportField.entries.forEach { field ->
                                    DropdownField(
                                        label = fieldLabels.getValue(field) + if (field.required) " *" else "",
                                        options = columnOptions,
                                        selected = options.mapping[field],
                                        onSelect = { vm.setMapping(field, it) },
                                    )
                                }
                            }
                        }
                    }
                    item {
                        RetailCard {
                            Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                                Text(stringResource(R.string.duplicates_policy), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                                ChoiceChips(
                                    options = listOf(
                                        DuplicatePolicy.SKIP to stringResource(R.string.dup_skip),
                                        DuplicatePolicy.UPDATE to stringResource(R.string.dup_update),
                                        DuplicatePolicy.CREATE to stringResource(R.string.dup_create),
                                    ),
                                    selected = options.duplicates,
                                    onSelect = vm::setDuplicates,
                                )
                                Text(stringResource(R.string.duplicates_help), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(stringResource(R.string.create_missing_categories), modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                                    Switch(checked = options.createMissingCategories, onCheckedChange = { v -> vm.setOptions { it.copy(createMissingCategories = v) } }, colors = retailSwitchColors())
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(stringResource(R.string.create_missing_units), modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                                    Switch(checked = options.createMissingUnits, onCheckedChange = { v -> vm.setOptions { it.copy(createMissingUnits = v) } }, colors = retailSwitchColors())
                                }
                                DropdownField(
                                    label = stringResource(R.string.default_unit),
                                    options = units.map<com.kalaban.farshidb2011.data.local.entity.UnitEntity, Pair<Long?, String>> { it.id to (if (fa) it.nameFa else it.nameEn) },
                                    selected = options.defaultUnitId,
                                    onSelect = { v -> vm.setOptions { it.copy(defaultUnitId = v) } },
                                )
                            }
                        }
                    }
                    item {
                        RetailPrimaryButton(onClick = vm::buildPreview, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.validate_preview)) }
                    }
                }
                ImportStep.PREVIEW -> {
                    val p = preview ?: return@LazyColumn
                    item {
                        RetailCard {
                            Column(Modifier.padding(Spacing.l)) {
                                Text(stringResource(R.string.import_preview), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                                KeyValueRow(stringResource(R.string.rows_new), p.count(ImportRowStatus.NEW).toString())
                                KeyValueRow(stringResource(R.string.rows_update), p.count(ImportRowStatus.UPDATE).toString())
                                KeyValueRow(stringResource(R.string.rows_skip), p.count(ImportRowStatus.SKIP).toString())
                                KeyValueRow(stringResource(R.string.rows_error), p.count(ImportRowStatus.ERROR).toString(), valueColor = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    if (p.count(ImportRowStatus.ERROR) > 0) {
                        item {
                            InfoBanner(
                                stringResource(R.string.import_errors_note),
                                Tone.WARNING,
                                actionLabel = stringResource(R.string.export_errors),
                                onAction = { errors.launch("kalaban-import-errors.csv") },
                            )
                        }
                    }
                    items(p.rows.take(300), key = { "r-${it.line}" }) { row -> ImportRowItem(row, issueLabels, fieldLabels) }
                    item {
                        val importable = p.count(ImportRowStatus.NEW) + p.count(ImportRowStatus.UPDATE)
                        RetailPrimaryButton(onClick = { confirm = true }, enabled = importable > 0 && !busy, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.import_rows, importable))
                        }
                        TextButton(onClick = vm::back, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.back_to_mapping)) }
                    }
                }
                ImportStep.DONE -> {
                    val r = result
                    if (r != null) {
                        item {
                            InfoBanner(stringResource(R.string.import_done, r.created, r.updated, r.skipped, r.failed), if (r.failed > 0) Tone.WARNING else Tone.SUCCESS)
                        }
                        if (r.batchId != null) {
                            item {
                                RetailOutlinedButton(onClick = onOpenBatches, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.view_price_batches)) }
                            }
                        }
                        item {
                            RetailPrimaryButton(onClick = vm::reset, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.import_another)) }
                        }
                    }
                }
            }
        }
    }
    if (confirm) {
        val p = preview
        ConfirmDialog(
            title = stringResource(R.string.import_rows, (p?.count(ImportRowStatus.NEW) ?: 0) + (p?.count(ImportRowStatus.UPDATE) ?: 0)),
            text = stringResource(R.string.import_confirm_text),
            confirmLabel = stringResource(R.string.import_data),
            onConfirm = {
                confirm = false
                vm.runImport(importTitle)
            },
            onDismiss = { confirm = false },
        )
    }
}

@Composable
private fun ImportRowItem(row: ImportRow, issueLabels: Map<ImportIssue, String>, fieldLabels: Map<ImportField, String>) {
    val (label, tone) = when (row.status) {
        ImportRowStatus.NEW -> stringResource(R.string.status_new) to Tone.SUCCESS
        ImportRowStatus.UPDATE -> stringResource(R.string.status_update) to Tone.INFO
        ImportRowStatus.SKIP -> stringResource(R.string.status_skip) to Tone.NEUTRAL
        ImportRowStatus.ERROR -> stringResource(R.string.status_error) to Tone.DANGER
    }
    ListItem(
        headlineContent = { Text("${row.line}. ${row.title.ifBlank { "—" }}", color = MaterialTheme.colorScheme.onSurface) },
        supportingContent = {
            if (row.issues.isNotEmpty()) {
                Text(
                    row.issues.joinToString("\n") { (issue, field) -> "• " + issueLabels.getValue(issue) + (field?.let { " (${fieldLabels.getValue(it)})" } ?: "") },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        trailingContent = { StatusChip(label, tone) },
        colors = retailListItemColors(MaterialTheme.colorScheme.background),
    )
}

@Composable
fun importFieldLabel(f: ImportField): String = stringResource(
    when (f) {
        ImportField.TITLE -> R.string.title
        ImportField.SKU -> R.string.sku
        ImportField.BARCODE -> R.string.barcode
        ImportField.CATEGORY -> R.string.select_category
        ImportField.UNIT -> R.string.unit
        ImportField.PIECES -> R.string.pieces_per_unit
        ImportField.PURCHASE_PRICE -> R.string.pack_total_price
        ImportField.PURCHASE_RATE -> R.string.purchase_dollar_rate
        ImportField.SELL_PRICE -> R.string.manual_price
        ImportField.STOCK -> R.string.stock_in_units
        ImportField.LOW_STOCK -> R.string.low_stock_threshold
        ImportField.MIN_PRICE -> R.string.min_price
        ImportField.MAX_PRICE -> R.string.max_price
        ImportField.TRANSPORT -> R.string.transport_cost
        ImportField.PACKAGING -> R.string.packaging_cost
        ImportField.OTHER_COST -> R.string.other_cost
        ImportField.PRICING_MODE -> R.string.pricing_method
        ImportField.PRICING_VALUE -> R.string.pricing_value
        ImportField.BRAND -> R.string.brand
        ImportField.SUPPLIER -> R.string.supplier
        ImportField.TAGS -> R.string.tags
        ImportField.DESCRIPTION -> R.string.description
        ImportField.NOTES -> R.string.notes
    },
)

@Composable
private fun importIssueText(i: ImportIssue): String = stringResource(
    when (i) {
        ImportIssue.TITLE_MISSING -> R.string.issue_title_missing
        ImportIssue.INVALID_NUMBER -> R.string.issue_invalid_number
        ImportIssue.NEGATIVE_VALUE -> R.string.issue_negative
        ImportIssue.UNKNOWN_CATEGORY -> R.string.issue_unknown_category
        ImportIssue.UNKNOWN_UNIT -> R.string.issue_unknown_unit
        ImportIssue.DUPLICATE_IN_FILE -> R.string.issue_duplicate_in_file
        ImportIssue.BARCODE_CONFLICT -> R.string.issue_barcode_conflict
        ImportIssue.INVALID_PRICING_MODE -> R.string.issue_pricing_mode
        ImportIssue.MARGIN_INVALID -> R.string.error_margin_range
        ImportIssue.VALUE_TOO_LARGE -> R.string.error_too_large
    },
)
