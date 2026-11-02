package com.kalaban.farshidb2011.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.NewLabel
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PriceChange
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kalaban.farshidb2011.R
import com.kalaban.farshidb2011.core.JalaliDate
import com.kalaban.farshidb2011.core.Money
import com.kalaban.farshidb2011.data.repository.BulkOperation
import com.kalaban.farshidb2011.data.repository.BulkRequest
import com.kalaban.farshidb2011.domain.Actor
import com.kalaban.farshidb2011.domain.Permission
import com.kalaban.farshidb2011.ui.AppViewModel
import com.kalaban.farshidb2011.ui.PricingViewModel
import com.kalaban.farshidb2011.ui.ReportsViewModel
import com.kalaban.farshidb2011.ui.components.InfoBanner
import com.kalaban.farshidb2011.ui.components.MetricCard
import com.kalaban.farshidb2011.ui.components.RetailCard
import com.kalaban.farshidb2011.ui.components.SectionHeader
import com.kalaban.farshidb2011.ui.components.Spacing
import com.kalaban.farshidb2011.ui.components.Tone
import com.kalaban.farshidb2011.ui.navigation.Routes

@Composable
fun DashboardScreen(
    appVm: AppViewModel,
    reportsVm: ReportsViewModel,
    pricingVm: PricingViewModel,
    onNavigate: (String) -> Unit,
    onOpenProducts: (lowStock: Boolean) -> Unit,
    onOpenAnalysis: () -> Unit,
) {
    val snapshot by reportsVm.dashboard.collectAsStateWithLifecycle()
    val settings by appVm.settings.collectAsStateWithLifecycle()
    val config by appVm.config.collectAsStateWithLifecycle()
    val user by appVm.sessionUser.collectAsStateWithLifecycle()
    val pendingRecalc by appVm.pendingRecalc.collectAsStateWithLifecycle()
    val currency = settings.currencyCode
    LaunchedEffect(Unit) { reportsVm.refreshDashboard() }

    fun openRecalculation() {
        pricingVm.prepareBulk(BulkRequest(operation = BulkOperation.Recalculate))
        appVm.dismissRecalcPrompt()
        onNavigate(Routes.BULK_PRICE)
    }

    val ownerLabel = stringResource(R.string.actor_owner)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        item {
            Column {
                Text(
                    config.businessName.ifBlank { stringResource(R.string.app_name) },
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                val name = user?.name?.let { if (it == Actor.OWNER_NAME) ownerLabel else it }.orEmpty()
                Text(
                    stringResource(R.string.dashboard_greeting, name, JalaliDate.today().format()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (pendingRecalc && appVm.can(Permission.MANAGE_PRICES)) {
            item {
                InfoBanner(
                    stringResource(R.string.recalc_prompt_banner),
                    Tone.WARNING,
                    actionLabel = stringResource(R.string.review),
                    onAction = ::openRecalculation,
                )
            }
        } else if (snapshot.outdatedPrices > 0 && appVm.can(Permission.MANAGE_PRICES)) {
            item {
                InfoBanner(
                    stringResource(R.string.outdated_prices_banner, snapshot.outdatedPrices),
                    Tone.INFO,
                    actionLabel = stringResource(R.string.review),
                    onAction = ::openRecalculation,
                )
            }
        }
        if (snapshot.backupDue && appVm.can(Permission.EXPORT_DATA)) {
            item {
                InfoBanner(
                    stringResource(R.string.backup_due_banner),
                    Tone.WARNING,
                    actionLabel = stringResource(R.string.backup),
                    onAction = { onNavigate(Routes.BACKUP) },
                )
            }
        }
        item { SectionHeader(stringResource(R.string.dashboard_today)) }
        item {
            MetricRow(
                {
                    MetricCard(
                        stringResource(R.string.metric_today_sales, snapshot.todayCount),
                        Money.format(snapshot.todaySales),
                        Icons.Outlined.Payments,
                        Modifier.weight(1f),
                        onClick = if (appVm.can(Permission.RECORD_SALES)) ({ onNavigate(Routes.NEW_SALE) }) else null,
                    )
                },
                {
                    MetricCard(
                        stringResource(R.string.metric_month_sales),
                        Money.format(snapshot.monthSales),
                        Icons.Outlined.CalendarMonth,
                        Modifier.weight(1f),
                    )
                },
            )
        }
        item {
            MetricRow(
                {
                    if (appVm.can(Permission.VIEW_COSTS)) {
                        MetricCard(
                            stringResource(R.string.metric_month_profit),
                            Money.format(snapshot.monthProfit),
                            if (snapshot.monthProfit.signum() < 0) Icons.AutoMirrored.Outlined.TrendingDown else Icons.AutoMirrored.Outlined.TrendingUp,
                            Modifier.weight(1f),
                            tone = if (snapshot.monthProfit.signum() < 0) Tone.DANGER else Tone.SUCCESS,
                        )
                    } else {
                        MetricCard(
                            stringResource(R.string.metric_products),
                            snapshot.activeProducts.toString(),
                            Icons.Outlined.Inventory2,
                            Modifier.weight(1f),
                        )
                    }
                },
                {
                    MetricCard(
                        stringResource(R.string.metric_open_credits, snapshot.openCreditCount),
                        Money.format(snapshot.openCredits),
                        Icons.Outlined.AttachMoney,
                        Modifier.weight(1f),
                        tone = if (snapshot.overdueCredits > 0) Tone.WARNING else Tone.NEUTRAL,
                        onClick = if (appVm.can(Permission.MANAGE_CREDITS)) ({ onNavigate(Routes.CREDITS) }) else null,
                    )
                },
            )
        }
        item { SectionHeader(stringResource(R.string.dashboard_attention)) }
        item {
            MetricRow(
                {
                    MetricCard(
                        stringResource(R.string.metric_low_stock),
                        snapshot.lowStock.toString(),
                        Icons.Outlined.WarningAmber,
                        Modifier.weight(1f),
                        tone = if (snapshot.lowStock > 0) Tone.WARNING else Tone.NEUTRAL,
                        onClick = { onOpenProducts(true) },
                    )
                },
                {
                    MetricCard(
                        stringResource(R.string.metric_negative_profit),
                        snapshot.negativeProfit.toString(),
                        Icons.Outlined.ReportProblem,
                        Modifier.weight(1f),
                        tone = if (snapshot.negativeProfit > 0) Tone.DANGER else Tone.NEUTRAL,
                        onClick = if (appVm.can(Permission.VIEW_REPORTS)) onOpenAnalysis else null,
                    )
                },
            )
        }
        item {
            MetricRow(
                {
                    MetricCard(
                        stringResource(R.string.metric_low_margin),
                        if (config.minMarginPct.signum() > 0) snapshot.lowMargin.toString() else "—",
                        Icons.AutoMirrored.Outlined.TrendingDown,
                        Modifier.weight(1f),
                        tone = if (snapshot.lowMargin > 0) Tone.WARNING else Tone.NEUTRAL,
                        onClick = if (appVm.can(Permission.VIEW_REPORTS)) onOpenAnalysis else null,
                    )
                },
                {
                    MetricCard(
                        stringResource(R.string.metric_dollar_rate),
                        Money.format(snapshot.dollarRate),
                        Icons.Outlined.PriceChange,
                        Modifier.weight(1f),
                        onClick = if (appVm.can(Permission.MANAGE_PRICES)) ({ onNavigate(Routes.SETTINGS) }) else null,
                    )
                },
            )
        }
        item { SectionHeader(stringResource(R.string.quick_actions)) }
        item {
            RetailCard {
                QuickAction(Icons.Outlined.AddShoppingCart, stringResource(R.string.record_sale), appVm.can(Permission.RECORD_SALES)) {
                    onNavigate(Routes.NEW_SALE)
                }
                QuickAction(Icons.Outlined.NewLabel, stringResource(R.string.new_product), appVm.can(Permission.EDIT_PRODUCTS)) {
                    onNavigate(Routes.productEdit(0))
                }
                QuickAction(Icons.Outlined.PriceChange, stringResource(R.string.bulk_price_update), appVm.can(Permission.MANAGE_PRICES)) {
                    pricingVm.prepareBulk(BulkRequest())
                    onNavigate(Routes.BULK_PRICE)
                }
                QuickAction(Icons.Outlined.UploadFile, stringResource(R.string.import_products), appVm.can(Permission.IMPORT_DATA)) {
                    onNavigate(Routes.IMPORT)
                }
            }
        }
        if (snapshot.recentBatches.isNotEmpty() && appVm.can(Permission.MANAGE_PRICES)) {
            item {
                SectionHeader(stringResource(R.string.recent_price_changes))
                RetailCard(Modifier.clickable { onNavigate(Routes.PRICE_BATCHES) }) {
                    snapshot.recentBatches.forEach { batch ->
                        ListItem(
                            headlineContent = { Text(batch.title, color = MaterialTheme.colorScheme.onSurface) },
                            supportingContent = {
                                Text(
                                    "${JalaliDate.fromEpochMs(batch.createdAtEpochMs).format()} · ${stringResource(R.string.products_count, batch.affectedCount)}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                        )
                    }
                }
            }
        }
        if (snapshot.scheduledBatches > 0) {
            item {
                InfoBanner(
                    stringResource(R.string.scheduled_batches_banner, snapshot.scheduledBatches),
                    Tone.INFO,
                    actionLabel = stringResource(R.string.view),
                    onAction = { onNavigate(Routes.PRICE_BATCHES) },
                )
            }
        }
        item { Spacer(Modifier.padding(bottom = 24.dp)) }
    }
}

@Composable
private fun MetricRow(first: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit, second: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
        first()
        second()
    }
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    if (!enabled) return
    ListItem(
        headlineContent = { Text(label, color = MaterialTheme.colorScheme.onSurface) },
        leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
    )
}
