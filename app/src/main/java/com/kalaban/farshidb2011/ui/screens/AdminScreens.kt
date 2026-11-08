package com.kalaban.farshidb2011.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Functions
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.ImportExport
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.ManageHistory
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material.icons.outlined.PriceChange
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kalaban.farshidb2011.R
import com.kalaban.farshidb2011.core.JalaliDate
import com.kalaban.farshidb2011.core.Money
import com.kalaban.farshidb2011.core.Numbers
import com.kalaban.farshidb2011.core.pricing.PriceRounding
import com.kalaban.farshidb2011.core.pricing.RoundingPolicy
import com.kalaban.farshidb2011.data.local.entity.AttributeDefinitionEntity
import com.kalaban.farshidb2011.data.local.entity.AttributeType
import com.kalaban.farshidb2011.data.local.entity.RoleEntity
import com.kalaban.farshidb2011.data.local.entity.SettingsEntity
import com.kalaban.farshidb2011.data.local.entity.UserEntity
import com.kalaban.farshidb2011.data.repository.AuditAction
import com.kalaban.farshidb2011.data.repository.DisplayPriceMode
import com.kalaban.farshidb2011.data.repository.UserSaveError
import com.kalaban.farshidb2011.domain.BuiltInRoles
import com.kalaban.farshidb2011.domain.Permission
import com.kalaban.farshidb2011.ui.AdminViewModel
import com.kalaban.farshidb2011.ui.AppViewModel
import com.kalaban.farshidb2011.ui.components.ChoiceChips
import com.kalaban.farshidb2011.ui.components.ConfirmDialog
import com.kalaban.farshidb2011.ui.components.DropdownField
import com.kalaban.farshidb2011.ui.components.HelpIconButton
import com.kalaban.farshidb2011.ui.components.InfoBanner
import com.kalaban.farshidb2011.ui.components.KeyValueRow
import com.kalaban.farshidb2011.ui.components.NumberField
import com.kalaban.farshidb2011.ui.components.RetailCard
import com.kalaban.farshidb2011.ui.components.RetailDivider
import com.kalaban.farshidb2011.ui.components.RetailEmptyState
import com.kalaban.farshidb2011.ui.components.RetailOutlinedButton
import com.kalaban.farshidb2011.ui.components.RetailPrimaryButton
import com.kalaban.farshidb2011.ui.components.RetailTextField
import com.kalaban.farshidb2011.ui.components.RetailTopAppBar
import com.kalaban.farshidb2011.ui.components.SearchField
import com.kalaban.farshidb2011.ui.components.SectionHeader
import com.kalaban.farshidb2011.ui.components.Spacing
import com.kalaban.farshidb2011.ui.components.StatusChip
import com.kalaban.farshidb2011.ui.components.Tone
import com.kalaban.farshidb2011.ui.components.retailContentWindowInsets
import com.kalaban.farshidb2011.ui.components.retailSwitchColors
import com.kalaban.farshidb2011.ui.components.retailListItemColors
import com.kalaban.farshidb2011.ui.navigation.Routes
import java.text.DateFormat
import java.util.Date

// ---------------------------------------------------------------- Management hub

private data class HubEntry(val label: Int, val subtitle: Int, val icon: ImageVector, val route: String, val visible: Boolean)

@Composable
fun ManagementScreen(appVm: AppViewModel, onNavigate: (String) -> Unit, onSignOut: () -> Unit) {
    val user by appVm.sessionUser.collectAsStateWithLifecycle()
    val owner = user?.isOwner == true
    val sections = listOf(
        R.string.section_pricing to listOf(
            HubEntry(R.string.pricing_rules, R.string.pricing_rules_sub, Icons.Outlined.Tune, Routes.SETTINGS, appVm.can(Permission.MANAGE_PRICES)),
            HubEntry(R.string.pricing_formulas, R.string.pricing_formulas_sub, Icons.Outlined.Functions, Routes.FORMULAS, appVm.can(Permission.MANAGE_FORMULAS)),
            HubEntry(R.string.bulk_price_update, R.string.bulk_price_update_sub, Icons.Outlined.PriceChange, Routes.BULK_PRICE, appVm.can(Permission.MANAGE_PRICES)),
            HubEntry(R.string.price_batches, R.string.price_batches_sub, Icons.Outlined.ManageHistory, Routes.PRICE_BATCHES, appVm.can(Permission.MANAGE_PRICES)),
            HubEntry(R.string.quantity_discounts, R.string.quantity_discounts_sub, Icons.Outlined.Percent, Routes.DISCOUNTS, appVm.can(Permission.MANAGE_PRICES)),
            HubEntry(R.string.dollar_history, R.string.dollar_history_sub, Icons.AutoMirrored.Outlined.ShowChart, Routes.DOLLAR_HISTORY, appVm.can(Permission.MANAGE_PRICES)),
        ),
        R.string.section_catalog to listOf(
            HubEntry(R.string.categories, R.string.categories_sub, Icons.Outlined.Category, Routes.CATEGORIES, appVm.can(Permission.MANAGE_CATALOG)),
            HubEntry(R.string.units, R.string.units_sub, Icons.Outlined.Straighten, Routes.UNITS, appVm.can(Permission.MANAGE_CATALOG)),
            HubEntry(R.string.attributes, R.string.attributes_sub, Icons.AutoMirrored.Outlined.Label, Routes.ATTRIBUTES, appVm.can(Permission.MANAGE_CATALOG)),
            HubEntry(R.string.low_stock, R.string.low_stock_sub, Icons.Outlined.WarningAmber, Routes.LOW_STOCK, appVm.can(Permission.VIEW_PRODUCTS)),
        ),
        R.string.section_sales to listOf(
            HubEntry(R.string.credits, R.string.credits_sub, Icons.Outlined.Payments, Routes.CREDITS, appVm.can(Permission.MANAGE_CREDITS)),
            HubEntry(R.string.reminders, R.string.reminders_sub, Icons.Outlined.Notifications, Routes.REMINDERS, appVm.can(Permission.MANAGE_CREDITS)),
        ),
        R.string.section_data to listOf(
            HubEntry(R.string.import_export, R.string.import_export_sub, Icons.Outlined.ImportExport, Routes.IMPORT, appVm.can(Permission.IMPORT_DATA) || appVm.can(Permission.EXPORT_DATA)),
            HubEntry(R.string.backup, R.string.backup_sub, Icons.Outlined.Backup, Routes.BACKUP, appVm.can(Permission.EXPORT_DATA) || appVm.can(Permission.IMPORT_DATA)),
        ),
        R.string.section_admin to listOf(
            HubEntry(R.string.general_settings, R.string.general_settings_sub, Icons.Outlined.Settings, Routes.GENERAL_SETTINGS, appVm.can(Permission.MANAGE_SETTINGS)),
            HubEntry(R.string.security, R.string.security_sub, Icons.Outlined.Security, Routes.SECURITY, owner || appVm.can(Permission.MANAGE_SETTINGS)),
            HubEntry(R.string.users, R.string.users_sub, Icons.Outlined.Group, Routes.USERS, appVm.can(Permission.MANAGE_USERS)),
            HubEntry(R.string.roles, R.string.roles_sub, Icons.Outlined.Badge, Routes.ROLES, appVm.can(Permission.MANAGE_USERS)),
            HubEntry(R.string.notification_settings, R.string.notification_settings_sub, Icons.Outlined.NotificationsActive, Routes.NOTIFICATIONS, appVm.can(Permission.MANAGE_SETTINGS)),
            HubEntry(R.string.audit_log, R.string.audit_log_sub, Icons.Outlined.History, Routes.AUDIT, appVm.can(Permission.VIEW_AUDIT)),
        ),
        R.string.section_help to listOf(
            HubEntry(R.string.help_center, R.string.help_center_sub, Icons.AutoMirrored.Outlined.HelpOutline, Routes.HELP, true),
            HubEntry(R.string.about, R.string.about_sub, Icons.Outlined.Info, Routes.ABOUT, true),
        ),
    )
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        user?.let { u ->
            item {
                RetailCard {
                    ListItem(
                        headlineContent = { Text(actorLabel(u.name), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text(stringResource(R.string.signed_in_as), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        leadingContent = { Icon(Icons.Outlined.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        trailingContent = {
                            TextButton(onClick = onSignOut) {
                                Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null)
                                Spacer(Modifier.padding(horizontal = 2.dp))
                                Text(stringResource(R.string.sign_out))
                            }
                        },
                        colors = retailListItemColors(MaterialTheme.colorScheme.surface),
                    )
                }
            }
        }
        sections.forEach { (title, entries) ->
            val visible = entries.filter { it.visible }
            if (visible.isNotEmpty()) {
                item(key = "s-$title") {
                    SectionHeader(stringResource(title))
                    RetailCard {
                        visible.forEachIndexed { i, e ->
                            ListItem(
                                headlineContent = { Text(stringResource(e.label), color = MaterialTheme.colorScheme.onSurface) },
                                supportingContent = { Text(stringResource(e.subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                                leadingContent = { Icon(e.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                modifier = Modifier.clickable { onNavigate(e.route) },
                                colors = retailListItemColors(MaterialTheme.colorScheme.surface),
                            )
                            if (i < visible.lastIndex) RetailDivider()
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(32.dp)) }
    }
}

// ---------------------------------------------------------------- Pricing rules

private fun emptyOr(v: Double): String = if (v == 0.0) "" else Money.input(v)

@Composable
fun PricingRulesScreen(appVm: AppViewModel, adminVm: AdminViewModel, onBack: () -> Unit, onRecalculate: () -> Unit) {
    val s by appVm.settings.collectAsStateWithLifecycle()
    val config by appVm.config.collectAsStateWithLifecycle()
    val pendingRecalc by appVm.pendingRecalc.collectAsStateWithLifecycle()
    var dollar by rememberSaveable { mutableStateOf("") }
    var tax by rememberSaveable { mutableStateOf("") }
    var profit by rememberSaveable { mutableStateOf("") }
    var manager by rememberSaveable { mutableStateOf("") }
    var dollarChange by rememberSaveable { mutableStateOf("") }
    var installment by rememberSaveable { mutableStateOf("") }
    var dirty by rememberSaveable { mutableStateOf(false) }
    var step by rememberSaveable { mutableStateOf("") }
    var overhead by rememberSaveable { mutableStateOf("") }
    var minMargin by rememberSaveable { mutableStateOf("") }
    var configLoaded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(s) {
        if (dirty) return@LaunchedEffect
        dollar = if (s.dollarRate == 0.0) "" else Money.input(s.dollarRate)
        tax = emptyOr(s.taxPercent)
        profit = emptyOr(s.profitPercent)
        manager = emptyOr(s.managerPercent)
        dollarChange = emptyOr(s.dollarChangePercent)
        installment = emptyOr(s.installmentDiffPercent)
    }
    LaunchedEffect(config) {
        if (configLoaded) return@LaunchedEffect
        step = config.roundingStep?.let { Money.input(it) }.orEmpty()
        overhead = if (config.overheadPct.signum() == 0) "" else Money.input(config.overheadPct)
        minMargin = if (config.minMarginPct.signum() == 0) "" else Money.input(config.minMarginPct)
        configLoaded = true
    }
    val numbers = listOf(dollar, tax, profit, manager, dollarChange, installment)
    val invalid = numbers.any { it.isNotBlank() && Numbers.parseDecimal(it)?.let { v -> v.signum() < 0 } != false }
    val dollarInvalid = Numbers.parseDecimal(dollar)?.let { it.signum() <= 0 } ?: true

    Scaffold(
        topBar = { RetailTopAppBar(stringResource(R.string.pricing_rules), onBack) },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        contentWindowInsets = retailContentWindowInsets(),
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize().imePadding(), contentPadding = PaddingValues(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
            item {
                RetailCard {
                    Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Text(stringResource(R.string.rates_and_percentages), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        RuleRow(stringResource(R.string.dollar_rate), stringResource(R.string.help_dollar_rate), dollar, dollarInvalid) { dirty = true; dollar = it }
                        RuleRow(stringResource(R.string.manager_percent), stringResource(R.string.help_manager_percent), manager) { dirty = true; manager = it }
                        RuleRow(stringResource(R.string.dollar_change_percent), stringResource(R.string.help_dollar_change_percent), dollarChange) { dirty = true; dollarChange = it }
                        RuleRow(stringResource(R.string.profit_percent), stringResource(R.string.help_profit_percent), profit) { dirty = true; profit = it }
                        RuleRow(stringResource(R.string.tax_percent), stringResource(R.string.help_tax_percent), tax) { dirty = true; tax = it }
                        RuleRow(stringResource(R.string.installment_diff_percent), stringResource(R.string.help_installment_diff_percent), installment) { dirty = true; installment = it }
                        if (invalid) Text(stringResource(R.string.error_negative), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        Text(
                            stringResource(R.string.dollar_save_help),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = Spacing.xs),
                        )
                        fun rulesCopy(): SettingsEntity {
                            fun v(t: String) = Numbers.parseDecimal(t)?.toDouble() ?: 0.0
                            return s.copy(
                                dollarRate = Numbers.parseDecimal(dollar)?.toDouble() ?: s.dollarRate,
                                taxPercent = v(tax),
                                profitPercent = v(profit),
                                managerPercent = v(manager),
                                dollarChangePercent = v(dollarChange),
                                installmentDiffPercent = v(installment),
                            )
                        }
                        RetailPrimaryButton(
                            onClick = {
                                appVm.savePricingRules(rulesCopy(), offerRecalc = false) { dirty = false }
                            },
                            enabled = !invalid && !dollarInvalid,
                            modifier = Modifier.fillMaxWidth().padding(top = Spacing.s),
                        ) { Text(stringResource(R.string.save_rate_only)) }
                        RetailOutlinedButton(
                            onClick = {
                                appVm.savePricingRules(rulesCopy(), offerRecalc = true) { dirty = false }
                            },
                            enabled = !invalid && !dollarInvalid,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(R.string.save_and_review_prices)) }
                    }
                }
            }
            item {
                RetailCard {
                    Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        Text(stringResource(R.string.rounding_and_costs), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        DropdownField(
                            label = stringResource(R.string.rounding_mode),
                            options = PriceRounding.entries.map { it to roundingLabel(it.name) },
                            selected = config.roundingMode,
                            onSelect = { mode -> adminVm.updateConfig(Permission.MANAGE_PRICES) { it.copy(roundingMode = mode) } },
                        )
                        val stepValue = Numbers.parseDecimal(step)
                        val stepInvalid = step.isNotBlank() && !RoundingPolicy.isValidStep(stepValue, config.moneyDecimals)
                        NumberField(step, { step = it }, stringResource(R.string.rounding_step), error = stepInvalid, supporting = stringResource(R.string.rounding_step_help))
                        NumberField(overhead, { overhead = it }, stringResource(R.string.overhead_percent), supporting = stringResource(R.string.overhead_help))
                        NumberField(minMargin, { minMargin = it }, stringResource(R.string.min_margin_percent), supporting = stringResource(R.string.min_margin_help))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.recalc_prompt_setting), modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                            Switch(
                                checked = config.recalcPrompt,
                                onCheckedChange = { v -> adminVm.updateConfig(Permission.MANAGE_PRICES) { it.copy(recalcPrompt = v) } },
                                colors = retailSwitchColors(),
                            )
                        }
                        RetailOutlinedButton(
                            onClick = {
                                adminVm.updateConfig(Permission.MANAGE_PRICES) {
                                    it.copy(
                                        roundingStep = stepValue?.takeIf { s -> s.signum() > 0 },
                                        overheadPct = Numbers.parseDecimal(overhead)?.max(java.math.BigDecimal.ZERO) ?: java.math.BigDecimal.ZERO,
                                        minMarginPct = Numbers.parseDecimal(minMargin)?.max(java.math.BigDecimal.ZERO) ?: java.math.BigDecimal.ZERO,
                                    )
                                }
                            },
                            enabled = !stepInvalid,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(R.string.save_rounding)) }
                    }
                }
            }
            item {
                InfoBanner(stringResource(R.string.recalculate_hint), Tone.NEUTRAL, actionLabel = stringResource(R.string.recalculate_prices), onAction = onRecalculate)
            }
        }
    }
    if (pendingRecalc) {
        ConfirmDialog(
            title = stringResource(R.string.recalc_prompt_title),
            text = stringResource(R.string.recalc_prompt_text),
            confirmLabel = stringResource(R.string.review),
            onConfirm = {
                appVm.dismissRecalcPrompt()
                onRecalculate()
            },
            onDismiss = appVm::dismissRecalcPrompt,
        )
    }
}

@Composable
private fun RuleRow(label: String, help: String, value: String, error: Boolean = false, onChange: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        NumberField(value, onChange, label, Modifier.weight(1f).padding(vertical = 2.dp), error = error)
        HelpIconButton(help)
    }
}

// ---------------------------------------------------------------- General settings

@Composable
fun GeneralSettingsScreen(appVm: AppViewModel, adminVm: AdminViewModel, onBack: () -> Unit) {
    val s by appVm.settings.collectAsStateWithLifecycle()
    val config by adminVm.config.collectAsStateWithLifecycle()
    val themePref by appVm.themeMode.collectAsStateWithLifecycle()
    var name by rememberSaveable { mutableStateOf<String?>(null) }
    var phone by rememberSaveable { mutableStateOf<String?>(null) }
    var address by rememberSaveable { mutableStateOf<String?>(null) }
    var note by rememberSaveable { mutableStateOf<String?>(null) }
    var currency by rememberSaveable { mutableStateOf<String?>(null) }
    val theme = themePref ?: if (s.displayDarkMode) "dark" else "light"

    Scaffold(
        topBar = { RetailTopAppBar(stringResource(R.string.general_settings), onBack) },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        contentWindowInsets = retailContentWindowInsets(),
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize().imePadding(), contentPadding = PaddingValues(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
            item {
                RetailCard {
                    Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        Text(stringResource(R.string.business_info), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        RetailTextField(value = name ?: config.businessName, onValueChange = { name = it }, label = { Text(stringResource(R.string.business_name)) })
                        RetailTextField(value = phone ?: config.businessPhone, onValueChange = { phone = it }, label = { Text(stringResource(R.string.business_phone)) }, numericLtr = true)
                        RetailTextField(value = address ?: config.businessAddress, onValueChange = { address = it }, label = { Text(stringResource(R.string.business_address)) }, singleLine = false, maxLines = 3)
                        RetailTextField(value = note ?: config.businessNote, onValueChange = { note = it }, label = { Text(stringResource(R.string.business_note)) }, supportingText = stringResource(R.string.business_note_help))
                        RetailPrimaryButton(
                            onClick = {
                                adminVm.updateConfig {
                                    it.copy(
                                        businessName = (name ?: it.businessName).trim(),
                                        businessPhone = (phone ?: it.businessPhone).trim(),
                                        businessAddress = (address ?: it.businessAddress).trim(),
                                        businessNote = (note ?: it.businessNote).trim(),
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(R.string.save)) }
                    }
                }
            }
            item {
                RetailCard {
                    Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        Text(stringResource(R.string.number_format), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RetailTextField(
                                value = currency ?: s.currencyCode,
                                onValueChange = { currency = it },
                                label = { Text(stringResource(R.string.currency)) },
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = {
                                val code = (currency ?: s.currencyCode).trim().ifBlank { "IRR" }
                                appVm.patchSettings { it.copy(currencyCode = code) }
                            }) { Text(stringResource(R.string.save)) }
                        }
                        Text(stringResource(R.string.decimal_places), color = MaterialTheme.colorScheme.onSurface)
                        ChoiceChips(
                            options = (0..3).map { it to it.toString() },
                            selected = config.moneyDecimals,
                            onSelect = { d -> adminVm.updateConfig { it.copy(moneyDecimals = d) } },
                        )
                        Text(stringResource(R.string.decimal_places_help), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.thousands_separator), color = MaterialTheme.colorScheme.onSurface)
                                Text(
                                    stringResource(R.string.thousands_separator_help),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(checked = config.grouping, onCheckedChange = { v -> adminVm.updateConfig { it.copy(grouping = v) } }, colors = retailSwitchColors())
                        }
                        KeyValueRow(stringResource(R.string.format_example), Money.format(java.math.BigDecimal("1234567.891")))
                    }
                }
            }
            item {
                RetailCard {
                    Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        Text(stringResource(R.string.appearance_language), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        Text(stringResource(R.string.theme_mode), color = MaterialTheme.colorScheme.onSurface)
                        ChoiceChips(
                            options = listOf("system" to stringResource(R.string.theme_system), "light" to stringResource(R.string.theme_light), "dark" to stringResource(R.string.theme_dark)),
                            selected = theme,
                            onSelect = appVm::setThemeMode,
                        )
                        Text(stringResource(R.string.language), color = MaterialTheme.colorScheme.onSurface)
                        ChoiceChips(
                            options = listOf("fa" to stringResource(R.string.lang_fa), "en" to stringResource(R.string.lang_en)),
                            selected = if (s.languageTag.startsWith("en")) "en" else "fa",
                            onSelect = { tag -> appVm.patchSettings { it.copy(languageTag = tag) } },
                        )
                    }
                }
            }
            item {
                RetailCard {
                    Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        Text(stringResource(R.string.display_panel_settings), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        Text(stringResource(R.string.display_price_mode), color = MaterialTheme.colorScheme.onSurface)
                        ChoiceChips(
                            options = listOf(DisplayPriceMode.BASE to stringResource(R.string.display_price_base), DisplayPriceMode.CASH to stringResource(R.string.display_price_cash)),
                            selected = config.displayPriceMode,
                            onSelect = { m -> adminVm.updateConfig { it.copy(displayPriceMode = m) } },
                        )
                        Text(stringResource(R.string.display_price_mode_help), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.display_show_stock), modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                            Switch(checked = config.displayShowStock, onCheckedChange = { v -> adminVm.updateConfig { it.copy(displayShowStock = v) } }, colors = retailSwitchColors())
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.display_show_images), modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                            Switch(checked = config.displayShowImages, onCheckedChange = { v -> adminVm.updateConfig { it.copy(displayShowImages = v) } }, colors = retailSwitchColors())
                        }
                    }
                }
            }
            item {
                RetailCard {
                    Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        Text(stringResource(R.string.csv_settings), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        ChoiceChips(
                            options = listOf(',' to stringResource(R.string.delimiter_comma), ';' to stringResource(R.string.delimiter_semicolon), '\t' to stringResource(R.string.delimiter_tab)),
                            selected = config.csvDelimiter,
                            onSelect = { d -> adminVm.updateConfig { it.copy(csvDelimiter = d) } },
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Security

@Composable
fun SecuritySettingsScreen(appVm: AppViewModel, adminVm: AdminViewModel, onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val s by appVm.settings.collectAsStateWithLifecycle()
    val user by appVm.sessionUser.collectAsStateWithLifecycle()
    var current by rememberSaveable { mutableStateOf("") }
    var new by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    val biometricAvailable = remember {
        androidx.biometric.BiometricManager.from(context)
            .canAuthenticate(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK) == androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
    }
    Scaffold(
        topBar = { RetailTopAppBar(stringResource(R.string.security), onBack) },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        contentWindowInsets = retailContentWindowInsets(),
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize().imePadding(), contentPadding = PaddingValues(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
            if (user?.isOwner == true) {
                item {
                    RetailCard {
                        Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                            Text(stringResource(R.string.change_owner_pin), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                            listOf(
                                Triple(current, R.string.current_pin) { v: String -> current = v },
                                Triple(new, R.string.set_new_pin) { v: String -> new = v },
                                Triple(confirm, R.string.confirm_pin) { v: String -> confirm = v },
                            ).forEach { (value, label, set) ->
                                RetailTextField(
                                    value = value,
                                    onValueChange = set,
                                    label = { Text(stringResource(label)) },
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                    numericLtr = true,
                                )
                            }
                            val mismatch = confirm.isNotEmpty() && confirm != new
                            if (mismatch) Text(stringResource(R.string.pin_mismatch), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            RetailPrimaryButton(
                                onClick = {
                                    adminVm.changeOwnerPin(current, new) {
                                        current = ""
                                        new = ""
                                        confirm = ""
                                    }
                                },
                                enabled = current.isNotEmpty() && new.length >= 4 && new == confirm,
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text(stringResource(R.string.change_pin)) }
                        }
                    }
                }
            }
            item {
                RetailCard {
                    Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.biometric), color = MaterialTheme.colorScheme.onSurface)
                                Text(stringResource(R.string.biometric_owner_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = s.biometricUnlockEnabled && biometricAvailable,
                                enabled = biometricAvailable,
                                onCheckedChange = { v -> appVm.patchSettings { it.copy(biometricUnlockEnabled = v) } },
                                colors = retailSwitchColors(),
                            )
                        }
                        if (!biometricAvailable) {
                            Text(stringResource(R.string.biometric_unavailable), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item {
                InfoBanner(stringResource(R.string.security_lockout_note), Tone.INFO)
            }
            if (appVm.can(Permission.MANAGE_USERS)) {
                item {
                    RetailCard {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.users), color = MaterialTheme.colorScheme.onSurface) },
                            supportingContent = { Text(stringResource(R.string.users_sub), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            leadingContent = { Icon(Icons.Outlined.Group, null, tint = MaterialTheme.colorScheme.primary) },
                            modifier = Modifier.clickable { onNavigate(Routes.USERS) },
                            colors = retailListItemColors(MaterialTheme.colorScheme.surface),
                        )
                        RetailDivider()
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.roles), color = MaterialTheme.colorScheme.onSurface) },
                            supportingContent = { Text(stringResource(R.string.roles_sub), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            leadingContent = { Icon(Icons.Outlined.Badge, null, tint = MaterialTheme.colorScheme.primary) },
                            modifier = Modifier.clickable { onNavigate(Routes.ROLES) },
                            colors = retailListItemColors(MaterialTheme.colorScheme.surface),
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Notifications

@Composable
fun NotificationSettingsScreen(adminVm: AdminViewModel, onBack: () -> Unit) {
    val config by adminVm.config.collectAsStateWithLifecycle()
    var days by rememberSaveable { mutableStateOf<String?>(null) }
    Scaffold(
        topBar = { RetailTopAppBar(stringResource(R.string.notification_settings), onBack) },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        contentWindowInsets = retailContentWindowInsets(),
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
            item { InfoBanner(stringResource(R.string.notifications_intro), Tone.INFO) }
            item {
                RetailCard {
                    Column(Modifier.padding(Spacing.l)) {
                        listOf(
                            Triple(R.string.notify_credit, config.notifyCreditReminders) { v: Boolean -> adminVm.updateConfig { it.copy(notifyCreditReminders = v) }; Unit },
                            Triple(R.string.notify_low_stock, config.notifyLowStock) { v: Boolean -> adminVm.updateConfig { it.copy(notifyLowStock = v) }; Unit },
                            Triple(R.string.notify_negative_profit, config.notifyNegativeProfit) { v: Boolean -> adminVm.updateConfig { it.copy(notifyNegativeProfit = v) }; Unit },
                            Triple(R.string.notify_low_margin, config.notifyLowMargin) { v: Boolean -> adminVm.updateConfig { it.copy(notifyLowMargin = v) }; Unit },
                            Triple(R.string.notify_scheduled, config.notifyScheduledPrices) { v: Boolean -> adminVm.updateConfig { it.copy(notifyScheduledPrices = v) }; Unit },
                        ).forEach { (label, checked, set) ->
                            Row(Modifier.heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(label), modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                                Switch(checked = checked, onCheckedChange = set, colors = retailSwitchColors())
                            }
                        }
                    }
                }
            }
            item {
                RetailCard {
                    Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        val value = days ?: if (config.backupReminderDays == 0) "" else config.backupReminderDays.toString()
                        NumberField(value, { days = it }, stringResource(R.string.backup_reminder_days), supporting = stringResource(R.string.backup_reminder_help))
                        val lastBackup = if (config.lastBackupAt > 0) JalaliDate.fromEpochMs(config.lastBackupAt).format() else stringResource(R.string.never)
                        KeyValueRow(stringResource(R.string.last_backup), lastBackup)
                        RetailOutlinedButton(
                            onClick = { adminVm.updateConfig { it.copy(backupReminderDays = Numbers.parseInt(value)?.coerceIn(0, 365) ?: 0) } },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(R.string.save)) }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Users

@Composable
fun UsersScreen(adminVm: AdminViewModel, settings: SettingsEntity, onBack: () -> Unit) {
    val users by adminVm.users.collectAsStateWithLifecycle()
    val roles by adminVm.roles.collectAsStateWithLifecycle()
    val errors by adminVm.userErrors.collectAsStateWithLifecycle()
    val fa = settings.languageTag.ifBlank { "fa" }.startsWith("fa")
    var editing by remember { mutableStateOf<UserEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<UserEntity?>(null) }
    Scaffold(
        topBar = { RetailTopAppBar(stringResource(R.string.users), onBack) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    adminVm.clearUserErrors()
                    editing = UserEntity(name = "", roleId = roles.firstOrNull { it.roleKey == BuiltInRoles.CASHIER }?.id ?: roles.firstOrNull()?.id ?: 0, pinHash = "", pinSalt = "")
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) { Icon(Icons.Outlined.Add, stringResource(R.string.add_user)) }
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        contentWindowInsets = retailContentWindowInsets(),
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            item { InfoBanner(stringResource(R.string.users_intro), Tone.INFO) }
            if (users.isEmpty()) item { RetailEmptyState(Icons.Outlined.Group, stringResource(R.string.no_users)) }
            items(users, key = { it.id }) { u ->
                val role = roles.firstOrNull { it.id == u.roleId }
                RetailCard(Modifier.clickable {
                    adminVm.clearUserErrors()
                    editing = u
                }) {
                    ListItem(
                        headlineContent = { Text(u.name, color = MaterialTheme.colorScheme.onSurface) },
                        supportingContent = {
                            Column {
                                Text(role?.let { if (fa) it.nameFa else it.nameEn }.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    stringResource(R.string.last_login) + ": " + (u.lastLoginAtEpochMs?.let { JalaliDate.fromEpochMs(it).format() } ?: stringResource(R.string.never)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                StatusChip(stringResource(if (u.active) R.string.active else R.string.inactive), if (u.active) Tone.SUCCESS else Tone.NEUTRAL)
                                IconButton(onClick = { deleteTarget = u }) { Icon(Icons.Outlined.Delete, stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error) }
                            }
                        },
                        colors = retailListItemColors(MaterialTheme.colorScheme.surface),
                    )
                }
            }
        }
    }
    editing?.let { u ->
        var name by remember(u) { mutableStateOf(u.name) }
        var roleId by remember(u) { mutableStateOf<Long?>(u.roleId.takeIf { it != 0L }) }
        var pin by remember(u) { mutableStateOf("") }
        var active by remember(u) { mutableStateOf(u.active) }
        AlertDialog(
            onDismissRequest = { editing = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(stringResource(if (u.id == 0L) R.string.add_user else R.string.edit_user)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    RetailTextField(
                        value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.name)) },
                        isError = UserSaveError.NAME_REQUIRED in errors || UserSaveError.NAME_TAKEN in errors,
                        supportingText = if (UserSaveError.NAME_TAKEN in errors) stringResource(R.string.error_name_taken) else null,
                    )
                    DropdownField(
                        label = stringResource(R.string.role),
                        options = roles.map<RoleEntity, Pair<Long?, String>> { it.id to (if (fa) it.nameFa else it.nameEn) },
                        selected = roleId,
                        onSelect = { roleId = it },
                        isError = UserSaveError.ROLE_MISSING in errors,
                    )
                    RetailTextField(
                        value = pin,
                        onValueChange = { pin = it },
                        label = { Text(stringResource(if (u.id == 0L) R.string.pin else R.string.new_pin_optional)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        numericLtr = true,
                        isError = UserSaveError.PIN_TOO_SHORT in errors || UserSaveError.PIN_IN_USE in errors,
                        supportingText = when {
                            UserSaveError.PIN_IN_USE in errors -> stringResource(R.string.error_pin_in_use)
                            UserSaveError.PIN_TOO_SHORT in errors -> stringResource(R.string.pin_too_short)
                            else -> null
                        },
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.active), modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                        Switch(checked = active, onCheckedChange = { active = it }, colors = retailSwitchColors())
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { adminVm.saveUser(u.id, name, roleId, pin.ifBlank { null }, active) { editing = null } }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    deleteTarget?.let { u ->
        ConfirmDialog(
            title = stringResource(R.string.delete_user_title, u.name),
            text = null,
            confirmLabel = stringResource(R.string.delete),
            destructive = true,
            onConfirm = {
                adminVm.deleteUser(u.id)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null },
        )
    }
}

// ---------------------------------------------------------------- Roles

@Composable
fun RolesScreen(adminVm: AdminViewModel, settings: SettingsEntity, onBack: () -> Unit) {
    val roles by adminVm.roles.collectAsStateWithLifecycle()
    val fa = settings.languageTag.ifBlank { "fa" }.startsWith("fa")
    var editing by remember { mutableStateOf<RoleEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<RoleEntity?>(null) }
    Scaffold(
        topBar = { RetailTopAppBar(stringResource(R.string.roles), onBack) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { editing = RoleEntity(roleKey = "", nameFa = "", nameEn = "", permissions = Permission.VIEW_PRODUCTS.name) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) { Icon(Icons.Outlined.Add, stringResource(R.string.add_role)) }
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        contentWindowInsets = retailContentWindowInsets(),
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            items(roles, key = { it.id }) { r ->
                val count = if (r.roleKey == BuiltInRoles.ADMIN) Permission.entries.size else Permission.parse(r.permissions).size
                RetailCard(Modifier.clickable { editing = r }) {
                    ListItem(
                        headlineContent = { Text(if (fa) r.nameFa else r.nameEn, color = MaterialTheme.colorScheme.onSurface) },
                        supportingContent = { Text(stringResource(R.string.permissions_count, count, Permission.entries.size), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (r.builtIn) StatusChip(stringResource(R.string.built_in), Tone.INFO)
                                else IconButton(onClick = { deleteTarget = r }) { Icon(Icons.Outlined.Delete, stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error) }
                            }
                        },
                        colors = retailListItemColors(MaterialTheme.colorScheme.surface),
                    )
                }
            }
        }
    }
    editing?.let { r ->
        val locked = r.roleKey == BuiltInRoles.ADMIN
        var nameFa by remember(r) { mutableStateOf(r.nameFa) }
        var nameEn by remember(r) { mutableStateOf(r.nameEn) }
        var perms by remember(r) { mutableStateOf(if (locked) Permission.entries.toSet() else Permission.parse(r.permissions)) }
        AlertDialog(
            onDismissRequest = { editing = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(stringResource(if (r.id == 0L) R.string.add_role else R.string.edit_role)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    if (locked) InfoBanner(stringResource(R.string.admin_role_locked), Tone.INFO)
                    RetailTextField(value = nameFa, onValueChange = { nameFa = it }, label = { Text(stringResource(R.string.name_fa)) }, enabled = !locked)
                    RetailTextField(value = nameEn, onValueChange = { nameEn = it }, label = { Text(stringResource(R.string.name_en)) }, enabled = !locked)
                    Spacer(Modifier.height(Spacing.s))
                    Permission.entries.forEach { p ->
                        Row(
                            Modifier.fillMaxWidth().clickable(enabled = !locked) { perms = if (p in perms) perms - p else perms + p },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = p in perms, onCheckedChange = null, enabled = !locked)
                            Text(permissionLabel(p), color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            },
            confirmButton = {
                if (!locked) {
                    TextButton(onClick = { adminVm.saveRole(r.id, nameFa, nameEn, perms) { editing = null } }) { Text(stringResource(R.string.save)) }
                } else {
                    TextButton(onClick = { editing = null }) { Text(stringResource(R.string.ok)) }
                }
            },
            dismissButton = if (!locked) ({ TextButton(onClick = { editing = null }) { Text(stringResource(R.string.cancel)) } }) else null,
        )
    }
    deleteTarget?.let { r ->
        ConfirmDialog(
            title = stringResource(R.string.delete_role_title),
            text = if (fa) r.nameFa else r.nameEn,
            confirmLabel = stringResource(R.string.delete),
            destructive = true,
            onConfirm = {
                adminVm.deleteRole(r.id)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null },
        )
    }
}

// ---------------------------------------------------------------- Audit log

@Composable
fun AuditLogScreen(adminVm: AdminViewModel, appVm: AppViewModel, onBack: () -> Unit) {
    val entries by adminVm.audit.collectAsStateWithLifecycle()
    val filter by adminVm.auditFilter.collectAsStateWithLifecycle()
    val headers = listOf(
        stringResource(R.string.date), stringResource(R.string.user), stringResource(R.string.action),
        stringResource(R.string.entity), "id", stringResource(R.string.summary), stringResource(R.string.details), stringResource(R.string.success),
    )
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) adminVm.exportAudit(uri, headers)
    }
    val formatter = remember { DateFormat.getTimeInstance(DateFormat.SHORT) }
    val actions = listOf(
        "" to stringResource(R.string.all_actions),
        AuditAction.LOGIN to stringResource(R.string.audit_login),
        AuditAction.LOGIN_FAILED to stringResource(R.string.audit_login_failed),
        AuditAction.PRODUCT_UPDATE to stringResource(R.string.audit_product_update),
        AuditAction.PRODUCT_CREATE to stringResource(R.string.audit_product_create),
        AuditAction.PRODUCT_DELETE to stringResource(R.string.audit_product_delete),
        AuditAction.PRICE_BATCH to stringResource(R.string.audit_price_batch),
        AuditAction.PRICE_ROLLBACK to stringResource(R.string.audit_price_rollback),
        AuditAction.FORMULA_SAVE to stringResource(R.string.audit_formula_save),
        AuditAction.SETTINGS_UPDATE to stringResource(R.string.audit_settings),
        AuditAction.CONFIG_UPDATE to stringResource(R.string.audit_config),
        AuditAction.SALE_CREATE to stringResource(R.string.audit_sale),
        AuditAction.DATA_IMPORT to stringResource(R.string.audit_import),
        AuditAction.DATA_RESTORE to stringResource(R.string.audit_restore),
        AuditAction.USER_SAVE to stringResource(R.string.audit_user_save),
    )
    Scaffold(
        topBar = {
            RetailTopAppBar(stringResource(R.string.audit_log), onBack, actions = {
                if (appVm.can(Permission.EXPORT_DATA)) {
                    IconButton(onClick = { export.launch("kalaban-audit.csv") }) { Icon(Icons.Outlined.FileDownload, stringResource(R.string.export_csv)) }
                }
            })
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        contentWindowInsets = retailContentWindowInsets(),
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            item { SearchField(filter.query, { q -> adminVm.setAuditFilter { it.copy(query = q) } }, stringResource(R.string.search)) }
            item {
                DropdownField(
                    label = stringResource(R.string.action),
                    options = actions,
                    selected = filter.action,
                    onSelect = { a -> adminVm.setAuditFilter { it.copy(action = a) } },
                )
            }
            item { Text(stringResource(R.string.audit_protected_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (entries.isEmpty()) item { RetailEmptyState(Icons.Outlined.History, stringResource(R.string.no_audit_entries)) }
            items(entries, key = { it.id }) { e ->
                var expanded by remember { mutableStateOf(false) }
                RetailCard(Modifier.clickable { expanded = !expanded }) {
                    Column(Modifier.padding(Spacing.m)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(auditActionLabel(e.action), style = MaterialTheme.typography.titleSmall, color = if (e.success) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                            Text(
                                "${JalaliDate.fromEpochMs(e.atEpochMs).format()} ${formatter.format(Date(e.atEpochMs))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text("${actorLabel(e.actorName)} · ${e.summary}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = if (expanded) 10 else 1, overflow = TextOverflow.Ellipsis)
                        if (expanded && !e.details.isNullOrBlank()) {
                            Text(e.details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Custom attributes

@Composable
fun AttributesScreen(adminVm: AdminViewModel, settings: SettingsEntity, onBack: () -> Unit) {
    val definitions by adminVm.attributes.collectAsStateWithLifecycle()
    val fa = settings.languageTag.ifBlank { "fa" }.startsWith("fa")
    var editing by remember { mutableStateOf<AttributeDefinitionEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<AttributeDefinitionEntity?>(null) }
    Scaffold(
        topBar = { RetailTopAppBar(stringResource(R.string.attributes), onBack) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { editing = AttributeDefinitionEntity(attrKey = "", labelFa = "", labelEn = "", sortOrder = definitions.size) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) { Icon(Icons.Outlined.Add, stringResource(R.string.add_attribute)) }
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        contentWindowInsets = retailContentWindowInsets(),
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            item { InfoBanner(stringResource(R.string.attributes_intro), Tone.INFO) }
            if (definitions.isEmpty()) item { RetailEmptyState(Icons.AutoMirrored.Outlined.Label, stringResource(R.string.no_attributes)) }
            items(definitions, key = { it.id }) { d ->
                RetailCard(Modifier.clickable { editing = d }) {
                    ListItem(
                        headlineContent = { Text(if (fa) d.labelFa else d.labelEn, color = MaterialTheme.colorScheme.onSurface) },
                        supportingContent = {
                            Text(
                                "${d.attrKey} · ${attributeTypeLabel(d.type)}" + if (d.type == AttributeType.NUMBER) " · attr_${d.attrKey}" else "",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (d.required) StatusChip(stringResource(R.string.required), Tone.WARNING)
                                if (!d.active) StatusChip(stringResource(R.string.inactive), Tone.NEUTRAL)
                                IconButton(onClick = { deleteTarget = d }) { Icon(Icons.Outlined.Delete, stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error) }
                            }
                        },
                        colors = retailListItemColors(MaterialTheme.colorScheme.surface),
                    )
                }
            }
        }
    }
    editing?.let { d ->
        var key by remember(d) { mutableStateOf(d.attrKey) }
        var labelFa by remember(d) { mutableStateOf(d.labelFa) }
        var labelEn by remember(d) { mutableStateOf(d.labelEn) }
        var type by remember(d) { mutableStateOf(d.type) }
        var options by remember(d) { mutableStateOf(d.options.orEmpty()) }
        var required by remember(d) { mutableStateOf(d.required) }
        var active by remember(d) { mutableStateOf(d.active) }
        AlertDialog(
            onDismissRequest = { editing = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(stringResource(if (d.id == 0L) R.string.add_attribute else R.string.edit_attribute)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    RetailTextField(value = key, onValueChange = { key = it }, label = { Text(stringResource(R.string.attribute_key)) }, numericLtr = true, supportingText = stringResource(R.string.attribute_key_help), enabled = d.id == 0L)
                    RetailTextField(value = labelFa, onValueChange = { labelFa = it }, label = { Text(stringResource(R.string.name_fa)) })
                    RetailTextField(value = labelEn, onValueChange = { labelEn = it }, label = { Text(stringResource(R.string.name_en)) })
                    DropdownField(
                        label = stringResource(R.string.attribute_type),
                        options = AttributeType.all.map { it to attributeTypeLabel(it) },
                        selected = type,
                        onSelect = { type = it },
                    )
                    if (type == AttributeType.CHOICE) {
                        RetailTextField(value = options, onValueChange = { options = it }, label = { Text(stringResource(R.string.attribute_options)) }, supportingText = stringResource(R.string.attribute_options_help))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.required), modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                        Switch(checked = required, onCheckedChange = { required = it }, colors = retailSwitchColors())
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.active), modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                        Switch(checked = active, onCheckedChange = { active = it }, colors = retailSwitchColors())
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    adminVm.saveAttribute(d.copy(attrKey = key, labelFa = labelFa, labelEn = labelEn, type = type, options = options, required = required, active = active)) { editing = null }
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    deleteTarget?.let { d ->
        ConfirmDialog(
            title = stringResource(R.string.delete_attribute_title),
            text = stringResource(R.string.delete_attribute_text),
            confirmLabel = stringResource(R.string.delete),
            destructive = true,
            onConfirm = {
                adminVm.deleteAttribute(d)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null },
        )
    }
}

@Composable
private fun attributeTypeLabel(type: String): String = stringResource(
    when (type) {
        AttributeType.NUMBER -> R.string.attr_type_number
        AttributeType.BOOLEAN -> R.string.attr_type_boolean
        AttributeType.CHOICE -> R.string.attr_type_choice
        else -> R.string.attr_type_text
    },
)