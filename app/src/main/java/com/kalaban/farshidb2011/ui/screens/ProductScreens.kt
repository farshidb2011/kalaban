package com.kalaban.farshidb2011.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.PriceChange
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kalaban.farshidb2011.BuildConfig
import com.kalaban.farshidb2011.R
import com.kalaban.farshidb2011.core.JalaliDate
import com.kalaban.farshidb2011.core.Money
import com.kalaban.farshidb2011.core.Numbers
import com.kalaban.farshidb2011.core.pricing.PriceBound
import com.kalaban.farshidb2011.core.pricing.PriceMethod
import com.kalaban.farshidb2011.core.pricing.PricingModes
import com.kalaban.farshidb2011.core.pricing.PricingWarning
import com.kalaban.farshidb2011.data.local.entity.AttributeType
import com.kalaban.farshidb2011.data.local.entity.ItemEntity
import com.kalaban.farshidb2011.data.local.entity.ItemStatus
import com.kalaban.farshidb2011.data.repository.BulkOperation
import com.kalaban.farshidb2011.data.repository.BulkRequest
import com.kalaban.farshidb2011.data.repository.BulkScope
import com.kalaban.farshidb2011.data.repository.ProductError
import com.kalaban.farshidb2011.domain.Permission
import com.kalaban.farshidb2011.ui.AppViewModel
import com.kalaban.farshidb2011.ui.PricingViewModel
import com.kalaban.farshidb2011.ui.ProductSort
import com.kalaban.farshidb2011.ui.ProductsViewModel
import com.kalaban.farshidb2011.ui.components.ChoiceChips
import com.kalaban.farshidb2011.ui.components.ConfirmDialog
import com.kalaban.farshidb2011.ui.components.DropdownField
import com.kalaban.farshidb2011.ui.components.ExpandableSection
import com.kalaban.farshidb2011.ui.components.InfoBanner
import com.kalaban.farshidb2011.ui.components.KeyValueRow
import com.kalaban.farshidb2011.ui.components.LoadingBox
import com.kalaban.farshidb2011.ui.components.MoneyText
import com.kalaban.farshidb2011.ui.components.NumberField
import com.kalaban.farshidb2011.ui.components.RetailCard
import com.kalaban.farshidb2011.ui.components.RetailDivider
import com.kalaban.farshidb2011.ui.components.RetailEmptyState
import com.kalaban.farshidb2011.ui.components.RetailFilterChip
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
import com.kalaban.farshidb2011.ui.theme.semantic
import java.io.File
import java.math.BigDecimal

// ---------------------------------------------------------------- Catalog list

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProductsScreen(
    vm: ProductsViewModel,
    appVm: AppViewModel,
    pricingVm: PricingViewModel,
    onOpen: (Long) -> Unit,
    onNew: () -> Unit,
    onBulkPrice: () -> Unit,
) {
    val products by vm.catalog.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    val selection by vm.selection.collectAsStateWithLifecycle()
    val categories by appVm.categories.collectAsStateWithLifecycle()
    val units by appVm.units.collectAsStateWithLifecycle()
    val settings by appVm.settings.collectAsStateWithLifecycle()
    val recent by vm.recentSearches.collectAsStateWithLifecycle()
    val fa = settings.languageTag.ifBlank { "fa" }.startsWith("fa")
    var sortMenu by remember { mutableStateOf(false) }
    var categoryMenu by remember { mutableStateOf(false) }
    var moveDialog by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val selecting = selection.isNotEmpty()

    Scaffold(
        floatingActionButton = {
            if (!selecting && appVm.can(Permission.EDIT_PRODUCTS)) {
                FloatingActionButton(
                    onClick = onNew,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) { Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.new_product)) }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = retailContentWindowInsets(embedded = true),
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            if (selecting) {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = Spacing.s), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = vm::clearSelection) { Icon(Icons.Outlined.Close, stringResource(R.string.cancel)) }
                        Text(stringResource(R.string.selected_count, selection.size), modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        IconButton(onClick = { vm.selectAll(products.map { it.id }) }) { Icon(Icons.Outlined.SelectAll, stringResource(R.string.select_all)) }
                        if (appVm.can(Permission.EDIT_PRODUCTS)) {
                            IconButton(onClick = { moveDialog = true }) { Icon(Icons.Outlined.Category, stringResource(R.string.change_category)) }
                        }
                        if (appVm.can(Permission.MANAGE_PRICES)) {
                            IconButton(onClick = {
                                pricingVm.prepareBulk(BulkRequest(scope = BulkScope(itemIds = selection, includeArchived = true)))
                                onBulkPrice()
                            }) { Icon(Icons.Outlined.PriceChange, stringResource(R.string.bulk_price_update)) }
                        }
                        if (appVm.can(Permission.DELETE_PRODUCTS)) {
                            val archivedView = filter.status == ItemStatus.ARCHIVED
                            IconButton(onClick = { vm.setArchived(selection, !archivedView) }) {
                                Icon(
                                    if (archivedView) Icons.Outlined.Unarchive else Icons.Outlined.Archive,
                                    stringResource(if (archivedView) R.string.restore else R.string.archive),
                                )
                            }
                            IconButton(onClick = { confirmDelete = true }) {
                                Icon(Icons.Outlined.Delete, stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
            Column(Modifier.padding(horizontal = Spacing.l, vertical = Spacing.s)) {
                SearchField(
                    query = filter.query,
                    onQueryChange = { q -> vm.setFilter { it.copy(query = q) } },
                    label = stringResource(R.string.search_products_hint),
                )
                if (filter.query.isEmpty() && recent.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s), modifier = Modifier.padding(top = Spacing.s)) {
                        items(recent) { q ->
                            RetailFilterChip(selected = false, label = q, onClick = { vm.setFilter { it.copy(query = q) } })
                        }
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s), modifier = Modifier.padding(top = Spacing.s)) {
                    item {
                        RetailFilterChip(
                            selected = filter.status == ItemStatus.ACTIVE,
                            label = stringResource(R.string.status_active),
                            onClick = { vm.setFilter { it.copy(status = ItemStatus.ACTIVE) } },
                        )
                    }
                    item {
                        RetailFilterChip(
                            selected = filter.status == ItemStatus.ARCHIVED,
                            label = stringResource(R.string.status_archived),
                            onClick = { vm.setFilter { it.copy(status = ItemStatus.ARCHIVED) } },
                        )
                    }
                    item {
                        RetailFilterChip(
                            selected = filter.lowStockOnly,
                            label = stringResource(R.string.low_stock),
                            onClick = { vm.setFilter { it.copy(lowStockOnly = !it.lowStockOnly) } },
                        )
                    }
                    item {
                        Box {
                            val current = categories.firstOrNull { it.id == filter.categoryId }
                            RetailFilterChip(
                                selected = current != null,
                                label = current?.let { if (fa) it.nameFa else it.nameEn } ?: stringResource(R.string.all_categories),
                                onClick = { categoryMenu = true },
                            )
                            DropdownMenu(expanded = categoryMenu, onDismissRequest = { categoryMenu = false }) {
                                DropdownMenuItem(text = { Text(stringResource(R.string.all_categories)) }, onClick = {
                                    vm.setFilter { it.copy(categoryId = null) }
                                    categoryMenu = false
                                })
                                categories.forEach { c ->
                                    DropdownMenuItem(text = { Text(if (fa) c.nameFa else c.nameEn) }, onClick = {
                                        vm.setFilter { it.copy(categoryId = c.id) }
                                        categoryMenu = false
                                    })
                                }
                            }
                        }
                    }
                    item {
                        Box {
                            IconButton(onClick = { sortMenu = true }) { Icon(Icons.Outlined.Sort, stringResource(R.string.sort)) }
                            DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                                listOf(
                                    ProductSort.TITLE to R.string.sort_title,
                                    ProductSort.PRICE_ASC to R.string.sort_price_asc,
                                    ProductSort.PRICE_DESC to R.string.sort_price_desc,
                                    ProductSort.STOCK_ASC to R.string.sort_stock_asc,
                                    ProductSort.RECENT to R.string.sort_recent,
                                ).forEach { (sort, label) ->
                                    DropdownMenuItem(
                                        text = { Text(stringResource(label), fontWeight = if (filter.sort == sort) FontWeight.Bold else FontWeight.Normal) },
                                        onClick = {
                                            vm.setFilter { it.copy(sort = sort) }
                                            sortMenu = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
                Text(
                    stringResource(R.string.products_count, products.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.xs),
                )
            }
            if (products.isEmpty()) {
                RetailEmptyState(
                    icon = Icons.Outlined.Inventory2,
                    title = stringResource(if (filter.query.isEmpty()) R.string.no_items else R.string.no_search_results),
                    subtitle = if (filter.query.isEmpty() && appVm.can(Permission.EDIT_PRODUCTS)) stringResource(R.string.no_items_hint) else null,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = Spacing.l, end = Spacing.l, bottom = 96.dp),
                ) {
                    items(products, key = { it.id }) { item ->
                        val selected = item.id in selection
                        val unitName = units.firstOrNull { it.id == item.unitId }?.let { if (fa) it.nameFa else it.nameEn }
                        val categoryName = categories.firstOrNull { it.id == item.categoryId }?.let { if (fa) it.nameFa else it.nameEn }
                        RetailCard(
                            Modifier.combinedClickable(
                                onClick = {
                                    if (selecting) vm.toggleSelection(item.id) else {
                                        if (filter.query.isNotEmpty()) vm.rememberSearch(filter.query)
                                        onOpen(item.id)
                                    }
                                },
                                onLongClick = { vm.toggleSelection(item.id) },
                            ),
                        ) {
                            ProductRow(item, unitName, categoryName, settings.currencyCode, selected)
                        }
                    }
                }
            }
        }
    }
    if (moveDialog) {
        var target by remember { mutableStateOf<Long?>(null) }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { moveDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(stringResource(R.string.change_category)) },
            text = {
                DropdownField(
                    label = stringResource(R.string.select_category),
                    options = listOf<Pair<Long?, String>>(null to stringResource(R.string.no_category)) +
                        categories.map { it.id to (if (fa) it.nameFa else it.nameEn) },
                    selected = target,
                    onSelect = { target = it },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.setCategory(selection, target)
                    moveDialog = false
                }) { Text(stringResource(R.string.apply)) }
            },
            dismissButton = { TextButton(onClick = { moveDialog = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.delete_products_title, selection.size),
            text = stringResource(R.string.delete_products_text),
            confirmLabel = stringResource(R.string.delete),
            destructive = true,
            onConfirm = {
                vm.delete(selection)
                confirmDelete = false
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun ProductRow(item: ItemEntity, unitName: String?, categoryName: String?, currency: String, selected: Boolean) {
    ListItem(
        headlineContent = { Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface) },
        supportingContent = {
            Column {
                MoneyText(Money.of(item.sellPrice), currency = currency, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.tertiary)
                Text(
                    listOfNotNull(
                        "${stringResource(R.string.stock)}: ${Money.input(item.stockQty)} ${stringResource(R.string.stock_pieces)}",
                        unitName,
                        categoryName,
                        item.sku?.takeIf { it.isNotBlank() },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), modifier = Modifier.padding(top = 2.dp)) {
                    if (item.status == ItemStatus.ARCHIVED) StatusChip(stringResource(R.string.status_archived), Tone.NEUTRAL)
                    if (item.lowStockThreshold > 0 && item.stockQty <= item.lowStockThreshold) StatusChip(stringResource(R.string.low_stock), Tone.WARNING)
                    if (item.pricingMode == PricingModes.MANUAL) StatusChip(stringResource(R.string.mode_manual), Tone.INFO)
                }
            }
        },
        leadingContent = {
            if (selected) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
            } else {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(10.dp))) { ItemThumb(item.imageUri) }
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        ),
    )
}

// ---------------------------------------------------------------- Detail

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductDetailScreen(
    id: Long,
    vm: ProductsViewModel,
    appVm: AppViewModel,
    pricingVm: PricingViewModel,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onOpen: (Long) -> Unit,
    onBulkPrice: () -> Unit,
) {
    val item by vm.observeItem(id).collectAsState(initial = null)
    val history by vm.observeHistory(id).collectAsState(initial = emptyList())
    val values by vm.observeAttributeValues(id).collectAsState(initial = emptyList())
    val definitions by vm.attributeDefinitions.collectAsStateWithLifecycle()
    val detail by vm.detail.collectAsStateWithLifecycle()
    val settings by appVm.settings.collectAsStateWithLifecycle()
    val categories by appVm.categories.collectAsStateWithLifecycle()
    val units by appVm.units.collectAsStateWithLifecycle()
    val fa = settings.languageTag.ifBlank { "fa" }.startsWith("fa")
    val currency = settings.currencyCode
    val showCosts = appVm.can(Permission.VIEW_COSTS)
    var confirmDelete by remember { mutableStateOf(false) }
    val copySuffix = stringResource(R.string.copy_suffix)

    LaunchedEffect(item) { item?.let { vm.loadDetail(it) } }

    Scaffold(
        topBar = {
            RetailTopAppBar(
                title = item?.title ?: stringResource(R.string.product),
                onBack = onBack,
                actions = {
                    val current = item
                    if (current != null && appVm.can(Permission.EDIT_PRODUCTS)) {
                        IconButton(onClick = { onEdit(current.id) }) { Icon(Icons.Outlined.Edit, stringResource(R.string.edit_item)) }
                        IconButton(onClick = { vm.duplicate(current.id, "${current.title} $copySuffix", onOpen) }) {
                            Icon(Icons.Outlined.ContentCopy, stringResource(R.string.duplicate))
                        }
                    }
                    if (current != null && appVm.can(Permission.DELETE_PRODUCTS)) {
                        val archived = current.status == ItemStatus.ARCHIVED
                        IconButton(onClick = { vm.setArchived(listOf(current.id), !archived) }) {
                            Icon(if (archived) Icons.Outlined.Unarchive else Icons.Outlined.Archive, stringResource(if (archived) R.string.restore else R.string.archive))
                        }
                        IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Outlined.Delete, stringResource(R.string.delete)) }
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = retailContentWindowInsets(),
    ) { pad ->
        val current = item
        if (current == null) {
            LoadingBox(Modifier.padding(pad))
            return@Scaffold
        }
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            item {
                val density = LocalDensity.current
                if (current.imageUri != null) {
                    ProductImage(
                        uri = current.imageUri,
                        maxSidePx = with(density) { 720.dp.roundToPx() },
                        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop,
                        contentDescription = current.title,
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), modifier = Modifier.padding(top = Spacing.s)) {
                    if (current.status == ItemStatus.ARCHIVED) StatusChip(stringResource(R.string.status_archived), Tone.NEUTRAL)
                    categories.firstOrNull { it.id == current.categoryId }?.let { StatusChip(if (fa) it.nameFa else it.nameEn, Tone.INFO) }
                    current.sku?.takeIf { it.isNotBlank() }?.let { StatusChip("${stringResource(R.string.sku)}: $it", Tone.NEUTRAL) }
                    current.barcode?.takeIf { it.isNotBlank() }?.let { StatusChip("${stringResource(R.string.barcode)}: $it", Tone.NEUTRAL) }
                    current.brand?.takeIf { it.isNotBlank() }?.let { StatusChip(it, Tone.NEUTRAL) }
                }
            }
            val d = detail?.takeIf { it.item.id == current.id }
            item {
                RetailCard {
                    Column(Modifier.padding(Spacing.l)) {
                        Text(stringResource(R.string.prices), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        KeyValueRow(stringResource(R.string.sell_price_base), Money.format(current.sellPrice) + " $currency", emphasize = true)
                        if (d != null) {
                            KeyValueRow(stringResource(R.string.cash_price), Money.format(d.storedSale.cashPrice) + " $currency")
                            KeyValueRow(stringResource(R.string.credit_price), Money.format(d.storedSale.creditPrice) + " $currency")
                            if (showCosts) {
                                KeyValueRow(
                                    stringResource(R.string.net_profit),
                                    Money.format(d.storedProfit.netProfit),
                                    valueColor = if (d.storedProfit.netProfit.signum() < 0) MaterialTheme.colorScheme.error else MaterialTheme.semantic.success,
                                )
                                KeyValueRow(stringResource(R.string.margin), d.storedProfit.marginPct?.let { Money.percent(it) } ?: "—")
                                KeyValueRow(stringResource(R.string.markup), d.storedProfit.markupPct?.let { Money.percent(it) } ?: "—")
                            }
                        }
                    }
                }
            }
            if (d != null && d.outdated && appVm.can(Permission.MANAGE_PRICES)) {
                item {
                    InfoBanner(
                        stringResource(R.string.price_outdated_detail, Money.format(d.result.finalPrice)),
                        Tone.WARNING,
                        actionLabel = stringResource(R.string.review),
                        onAction = {
                            pricingVm.prepareBulk(BulkRequest(scope = BulkScope(itemIds = setOf(current.id), includeArchived = true), operation = BulkOperation.Recalculate))
                            onBulkPrice()
                        },
                    )
                }
            }
            if (d != null && showCosts) {
                item { PriceBreakdownCard(d, currency, settings.taxPercent) }
            }
            item {
                RetailCard {
                    Column(Modifier.padding(Spacing.l)) {
                        Text(stringResource(R.string.stock), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        val unit = units.firstOrNull { it.id == current.unitId }?.let { if (fa) it.nameFa else it.nameEn }.orEmpty()
                        KeyValueRow(stringResource(R.string.stock), "${Money.input(current.stockQty)} ${stringResource(R.string.stock_pieces)}")
                        KeyValueRow(stringResource(R.string.unit), "$unit × ${Money.input(current.piecesPerUnit)}")
                        KeyValueRow(stringResource(R.string.low_stock_threshold), Money.input(current.lowStockThreshold))
                        current.maxStock?.let { KeyValueRow(stringResource(R.string.max_stock), Money.input(it)) }
                    }
                }
            }
            val attrs = definitions.filter { def -> values.any { it.attributeId == def.id } }
            if (attrs.isNotEmpty()) {
                item {
                    RetailCard {
                        Column(Modifier.padding(Spacing.l)) {
                            Text(stringResource(R.string.attributes), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                            attrs.forEach { def ->
                                val raw = values.first { it.attributeId == def.id }.value
                                val shown = when (def.type) {
                                    AttributeType.BOOLEAN -> stringResource(if (raw == "true") R.string.yes else R.string.no)
                                    else -> raw
                                }
                                KeyValueRow(if (fa) def.labelFa else def.labelEn, shown)
                            }
                        }
                    }
                }
            }
            val extra = listOfNotNull(
                current.supplier?.takeIf { it.isNotBlank() }?.let { R.string.supplier to it },
                current.tags?.takeIf { it.isNotBlank() }?.let { R.string.tags to it },
                current.description?.takeIf { it.isNotBlank() }?.let { R.string.description to it },
                current.notes?.takeIf { it.isNotBlank() }?.let { R.string.notes to it },
            )
            item {
                RetailCard {
                    Column(Modifier.padding(Spacing.l)) {
                        extra.forEach { (label, value) -> KeyValueRow(stringResource(label), value) }
                        KeyValueRow(stringResource(R.string.created_at), JalaliDate.fromEpochMs(current.createdAtEpochMs.takeIf { it > 0 } ?: current.updatedAtEpochMs).format())
                        KeyValueRow(stringResource(R.string.updated_at), JalaliDate.fromEpochMs(current.updatedAtEpochMs).format())
                    }
                }
            }
            item { SectionHeader(stringResource(R.string.price_history)) }
            if (history.isEmpty()) {
                item {
                    Text(stringResource(R.string.no_price_history), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }
            }
            items(history, key = { it.id }) { h ->
                val old = h.oldSellPrice
                ListItem(
                    headlineContent = {
                        Text(
                            (old?.let { Money.format(it) + " → " } ?: "") + Money.format(h.newSellPrice),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    supportingContent = {
                        Text(
                            "${JalaliDate.fromEpochMs(h.changedAtEpochMs).format()} · ${priceSourceLabel(h.source)}" + (h.actor?.let { " · ${actorLabel(it)}" } ?: ""),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    leadingContent = { Icon(Icons.Outlined.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                )
            }
        }
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.delete_products_title, 1),
            text = stringResource(R.string.delete_products_text),
            confirmLabel = stringResource(R.string.delete),
            destructive = true,
            onConfirm = {
                confirmDelete = false
                vm.delete(listOf(id), onBack)
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

/** Step-by-step explanation of how the stored price and counter price are derived. */
@Composable
fun PriceBreakdownCard(d: com.kalaban.farshidb2011.data.repository.ProductPrice, currency: String, taxPercent: Double) {
    val r = d.result
    val c = r.cost
    RetailCard {
        Column(Modifier.padding(Spacing.l)) {
            Text(stringResource(R.string.price_breakdown), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            KeyValueRow(stringResource(R.string.purchase_price_piece), Money.format(c.purchasePrice))
            if (c.ratio.compareTo(BigDecimal.ONE) != 0) {
                KeyValueRow(stringResource(R.string.dollar_ratio), "× " + c.ratio.setScale(4, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString())
                KeyValueRow(stringResource(R.string.adjusted_purchase), Money.format(c.adjustedPurchase))
            }
            if (c.extraCosts.signum() > 0) KeyValueRow(stringResource(R.string.extra_costs), "+ " + Money.format(c.extraCosts))
            if (c.overheadAmount.signum() > 0) KeyValueRow(stringResource(R.string.overhead), "+ " + Money.format(c.overheadAmount))
            KeyValueRow(stringResource(R.string.full_cost), Money.format(c.fullCost), emphasize = true)
            RetailDivider(Modifier.padding(vertical = Spacing.xs))
            KeyValueRow(stringResource(R.string.pricing_method), r.formulaName ?: methodLabel(r.method))
            KeyValueRow(stringResource(R.string.calculated_price), Money.format(r.rawPrice))
            if (r.roundingAdjustment.signum() != 0) KeyValueRow(stringResource(R.string.rounding_adjustment), Money.format(r.roundingAdjustment))
            r.bound?.let {
                KeyValueRow(stringResource(if (it == PriceBound.MIN) R.string.limited_by_min else R.string.limited_by_max), Money.format(r.finalPrice))
            }
            KeyValueRow(stringResource(R.string.computed_base_price), Money.format(r.finalPrice) + " $currency", emphasize = true)
            RetailDivider(Modifier.padding(vertical = Spacing.xs))
            val s = d.storedSale
            KeyValueRow(stringResource(R.string.sell_price_base), Money.format(s.basePrice))
            if (s.dollarChangeAmount.signum() != 0) KeyValueRow(stringResource(R.string.dollar_change_percent), "+ " + Money.format(s.dollarChangeAmount))
            if (s.profitAmount.signum() != 0) KeyValueRow(stringResource(R.string.profit_percent), "+ " + Money.format(s.profitAmount))
            KeyValueRow(stringResource(R.string.pre_tax_price), Money.format(s.preTax))
            if (s.taxAmount.signum() != 0) KeyValueRow("${stringResource(R.string.tax_percent)} (${Money.input(taxPercent)}%)", "+ " + Money.format(s.taxAmount))
            if (s.roundingAdjustment.signum() != 0) KeyValueRow(stringResource(R.string.rounding_adjustment), Money.format(s.roundingAdjustment))
            KeyValueRow(stringResource(R.string.cash_price), Money.format(s.cashPrice) + " $currency", emphasize = true)
            KeyValueRow(stringResource(R.string.break_even_price), Money.format(d.storedProfit.breakEvenPayable))
            if (r.warnings.isNotEmpty() || r.formulaError != null) {
                RetailDivider(Modifier.padding(vertical = Spacing.xs))
                r.warnings.forEach { w -> Text("• " + warningLabel(w), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.semantic.warning) }
                r.formulaError?.let { Text("• " + formulaErrorText(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
fun methodLabel(method: PriceMethod): String = stringResource(
    when (method) {
        PriceMethod.DOLLAR_MANAGER -> R.string.method_dollar_manager
        PriceMethod.FORMULA -> R.string.mode_formula
        PriceMethod.MARKUP -> R.string.mode_markup
        PriceMethod.MARGIN -> R.string.mode_margin
        PriceMethod.FIXED_PROFIT -> R.string.mode_fixed_profit
        PriceMethod.MANUAL -> R.string.mode_manual
    },
)

@Composable
fun warningLabel(w: PricingWarning): String = stringResource(
    when (w) {
        PricingWarning.INVALID_PURCHASE_RATE -> R.string.warn_invalid_rate
        PricingWarning.MISSING_COST -> R.string.warn_missing_cost
        PricingWarning.INVALID_VALUE -> R.string.warn_invalid_value
        PricingWarning.FORMULA_MISSING -> R.string.warn_formula_missing
        PricingWarning.FORMULA_INACTIVE -> R.string.warn_formula_inactive
        PricingWarning.FORMULA_ERROR -> R.string.warn_formula_error
        PricingWarning.CONDITION_ERROR -> R.string.warn_condition_error
        PricingWarning.BELOW_COST -> R.string.warn_below_cost
        PricingWarning.OUTSIDE_BOUNDS -> R.string.warn_outside_bounds
    },
)

// ---------------------------------------------------------------- Editor

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductEditorScreen(
    id: Long,
    vm: ProductsViewModel,
    appVm: AppViewModel,
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
) {
    val context = LocalContext.current
    val state by vm.editor.collectAsStateWithLifecycle()
    val preview by vm.preview.collectAsStateWithLifecycle()
    val units by appVm.units.collectAsStateWithLifecycle()
    val categories by appVm.categories.collectAsStateWithLifecycle()
    val settings by appVm.settings.collectAsStateWithLifecycle()
    val formulas by vm.formulas.collectAsStateWithLifecycle()
    val definitions by vm.attributeDefinitions.collectAsStateWithLifecycle()
    val fa = settings.languageTag.ifBlank { "fa" }.startsWith("fa")
    val showCosts = appVm.can(Permission.VIEW_COSTS)
    val currency = settings.currencyCode

    LaunchedEffect(id, units.isNotEmpty()) {
        if (state.loaded && state.id == id && id != 0L) return@LaunchedEffect
        if (id == 0L) vm.startNew(units.firstOrNull()?.id) else vm.startEdit(id)
    }

    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) vm.importImage(uri.toString())
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val pending = vm.editor.value.pendingCameraUri
        if (ok && pending != null) vm.importImage(pending)
    }
    fun launchCamera() {
        val dir = File(context.cacheDir, "item_images").apply { mkdirs() }
        val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.fileprovider", File(dir, "item_${System.currentTimeMillis()}.jpg"))
        vm.updateEditor { it.copy(pendingCameraUri = uri.toString()) }
        camera.launch(uri)
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera()
    }

    fun err(e: ProductError) = e in state.errors
    fun bad(key: String) = key in state.invalidNumbers
    val invalidNumber = stringResource(R.string.invalid_number)

    Scaffold(
        topBar = { RetailTopAppBar(title = stringResource(if (id == 0L) R.string.new_product else R.string.edit_item), onBack = onBack) },
        containerColor = MaterialTheme.colorScheme.background,
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
                            value = state.title,
                            onValueChange = { v -> vm.updateEditor { it.copy(title = v) } },
                            label = { Text(stringResource(R.string.title) + " *") },
                            isError = err(ProductError.TITLE_REQUIRED),
                            supportingText = if (err(ProductError.TITLE_REQUIRED)) stringResource(R.string.error_required) else null,
                        )
                        DropdownField(
                            label = stringResource(R.string.select_category),
                            options = listOf<Pair<Long?, String>>(null to stringResource(R.string.no_category)) +
                                categories.map { it.id to (if (fa) it.nameFa else it.nameEn) },
                            selected = state.categoryId,
                            onSelect = { v -> vm.updateEditor { it.copy(categoryId = v) } },
                        )
                        DropdownField(
                            label = stringResource(R.string.unit) + " *",
                            options = units.map<com.kalaban.farshidb2011.data.local.entity.UnitEntity, Pair<Long?, String>> { it.id to (if (fa) it.nameFa else it.nameEn) },
                            selected = state.unitId,
                            onSelect = { v -> vm.updateEditor { it.copy(unitId = v) } },
                            isError = err(ProductError.UNIT_REQUIRED),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                            RetailTextField(
                                value = state.sku,
                                onValueChange = { v -> vm.updateEditor { it.copy(sku = v) } },
                                label = { Text(stringResource(R.string.sku)) },
                                numericLtr = true,
                                modifier = Modifier.weight(1f),
                                isError = err(ProductError.SKU_DUPLICATE),
                                supportingText = if (err(ProductError.SKU_DUPLICATE)) stringResource(R.string.error_sku_duplicate) else null,
                            )
                            RetailTextField(
                                value = state.barcode,
                                onValueChange = { v -> vm.updateEditor { it.copy(barcode = v) } },
                                label = { Text(stringResource(R.string.barcode)) },
                                numericLtr = true,
                                modifier = Modifier.weight(1f),
                                isError = err(ProductError.BARCODE_DUPLICATE),
                                supportingText = if (err(ProductError.BARCODE_DUPLICATE)) stringResource(R.string.error_barcode_duplicate) else null,
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(64.dp).clip(RoundedCornerShape(12.dp))) { ItemThumb(state.imageUri) }
                            RetailOutlinedButton(
                                onClick = {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                        launchCamera()
                                    } else {
                                        cameraPermission.launch(Manifest.permission.CAMERA)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Outlined.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.size(Spacing.xs))
                                Text(stringResource(R.string.take_photo))
                            }
                            RetailOutlinedButton(onClick = { gallery.launch("image/*") }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.size(Spacing.xs))
                                Text(stringResource(R.string.pick_image))
                            }
                        }
                    }
                }
            }
            if (showCosts) {
                item {
                    RetailCard {
                        Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                            Text(stringResource(R.string.cost_section), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                                NumberField(
                                    state.pieces, { v -> vm.updateEditor { it.copy(pieces = v) } }, stringResource(R.string.pieces_per_unit),
                                    Modifier.weight(1f), error = bad("pieces") || err(ProductError.PIECES_INVALID),
                                )
                                NumberField(
                                    state.packPrice, { v -> vm.updateEditor { it.copy(packPrice = v) } }, stringResource(R.string.pack_total_price),
                                    Modifier.weight(1.4f), error = bad("packPrice"), supporting = if (bad("packPrice")) invalidNumber else null,
                                )
                            }
                            val pieces = Numbers.parseDecimal(state.pieces)?.takeIf { it.signum() > 0 }
                            val pack = Numbers.parseDecimal(state.packPrice)
                            if (pieces != null && pack != null && pieces.compareTo(BigDecimal.ONE) != 0) {
                                Text(
                                    "${stringResource(R.string.unit_piece_price)}: ${Money.format(pack.divide(pieces, Money.MC))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(stringResource(R.string.dollar_linked), color = MaterialTheme.colorScheme.onSurface)
                                    Text(stringResource(R.string.dollar_linked_help), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(checked = state.dollarLinked, onCheckedChange = { v -> vm.updateEditor { it.copy(dollarLinked = v) } }, colors = retailSwitchColors())
                            }
                        }
                    }
                }
                item {
                    ExpandableSection(
                        stringResource(R.string.extra_costs),
                        subtitle = stringResource(R.string.extra_costs_help),
                        initiallyExpanded = listOf(state.transport, state.packaging, state.other).any { it.isNotBlank() },
                    ) {
                        NumberField(state.transport, { v -> vm.updateEditor { it.copy(transport = v) } }, stringResource(R.string.transport_cost), error = bad("transport"))
                        NumberField(state.packaging, { v -> vm.updateEditor { it.copy(packaging = v) } }, stringResource(R.string.packaging_cost), error = bad("packaging"))
                        NumberField(state.other, { v -> vm.updateEditor { it.copy(other = v) } }, stringResource(R.string.other_cost), error = bad("other"))
                    }
                }
            }
            item {
                RetailCard {
                    Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        Text(stringResource(R.string.pricing_method), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        ChoiceChips(
                            options = listOf(
                                PricingModes.AUTO to stringResource(R.string.mode_auto),
                                PricingModes.FORMULA to stringResource(R.string.mode_formula),
                                PricingModes.MARKUP to stringResource(R.string.mode_markup),
                                PricingModes.MARGIN to stringResource(R.string.mode_margin),
                                PricingModes.FIXED_PROFIT to stringResource(R.string.mode_fixed_profit),
                                PricingModes.MANUAL to stringResource(R.string.mode_manual),
                            ),
                            selected = state.pricingMode,
                            onSelect = { v -> vm.updateEditor { it.copy(pricingMode = v) } },
                        )
                        Text(modeHelp(state.pricingMode), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        when (state.pricingMode) {
                            PricingModes.MARKUP, PricingModes.MARGIN, PricingModes.FIXED_PROFIT -> NumberField(
                                state.pricingValue,
                                { v -> vm.updateEditor { it.copy(pricingValue = v) } },
                                stringResource(
                                    when (state.pricingMode) {
                                        PricingModes.MARKUP -> R.string.markup_percent
                                        PricingModes.MARGIN -> R.string.margin_percent
                                        else -> R.string.fixed_profit_amount
                                    },
                                ),
                                error = bad("pricingValue") || err(ProductError.PRICING_VALUE_REQUIRED) || err(ProductError.MARGIN_INVALID),
                                supporting = when {
                                    err(ProductError.MARGIN_INVALID) -> stringResource(R.string.error_margin_range)
                                    err(ProductError.PRICING_VALUE_REQUIRED) -> stringResource(R.string.error_required)
                                    else -> null
                                },
                            )
                            PricingModes.FORMULA -> DropdownField(
                                label = stringResource(R.string.formula),
                                options = formulas.map<com.kalaban.farshidb2011.data.local.entity.PricingFormulaEntity, Pair<Long?, String>> {
                                    it.id to (it.name + if (!it.active) " (${stringResource(R.string.inactive)})" else "")
                                },
                                selected = state.formulaId,
                                onSelect = { v -> vm.updateEditor { it.copy(formulaId = v) } },
                                isError = err(ProductError.FORMULA_REQUIRED),
                            )
                            PricingModes.MANUAL -> NumberField(
                                state.manualPrice,
                                { v -> vm.updateEditor { it.copy(manualPrice = v) } },
                                stringResource(R.string.manual_price),
                                error = bad("manualPrice") || err(ProductError.MANUAL_PRICE_REQUIRED),
                            )
                        }
                    }
                }
            }
            item {
                ExpandableSection(
                    stringResource(R.string.price_bounds),
                    subtitle = stringResource(R.string.price_bounds_help),
                    initiallyExpanded = state.minPrice.isNotBlank() || state.maxPrice.isNotBlank(),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        NumberField(state.minPrice, { v -> vm.updateEditor { it.copy(minPrice = v) } }, stringResource(R.string.min_price), Modifier.weight(1f), error = bad("minPrice") || err(ProductError.MIN_ABOVE_MAX))
                        NumberField(state.maxPrice, { v -> vm.updateEditor { it.copy(maxPrice = v) } }, stringResource(R.string.max_price), Modifier.weight(1f), error = bad("maxPrice") || err(ProductError.MIN_ABOVE_MAX))
                    }
                    if (err(ProductError.MIN_ABOVE_MAX)) Text(stringResource(R.string.error_min_above_max), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
            preview?.let { p ->
                item {
                    Surface(color = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer, shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.fillMaxWidth().padding(Spacing.l)) {
                            Text(stringResource(R.string.price_preview), style = MaterialTheme.typography.titleSmall)
                            Text(
                                Money.format(p.result.finalPrice) + " $currency",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "${stringResource(R.string.cash_price)}: ${Money.format(p.sale.cashPrice)} · ${p.result.formulaName ?: methodLabel(p.result.method)}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            if (showCosts) {
                                Text(
                                    "${stringResource(R.string.full_cost)}: ${Money.format(p.result.cost.fullCost)} · ${stringResource(R.string.margin)}: ${p.profit.marginPct?.let { Money.percent(it) } ?: "—"}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            p.result.warnings.forEach { w ->
                                Text("• " + warningLabel(w), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
            item {
                RetailCard {
                    Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        Text(stringResource(R.string.stock), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        NumberField(state.stockUnits, { v -> vm.updateEditor { it.copy(stockUnits = v) } }, stringResource(R.string.stock_in_units), error = bad("stockUnits"))
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                            NumberField(state.lowStock, { v -> vm.updateEditor { it.copy(lowStock = v) } }, stringResource(R.string.low_stock_threshold), Modifier.weight(1f), error = bad("lowStock"))
                            NumberField(state.maxStock, { v -> vm.updateEditor { it.copy(maxStock = v) } }, stringResource(R.string.max_stock), Modifier.weight(1f), error = bad("maxStock"))
                        }
                    }
                }
            }
            val activeDefinitions = definitions.filter { it.active }
            if (activeDefinitions.isNotEmpty()) {
                item {
                    RetailCard {
                        Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                            Text(stringResource(R.string.attributes), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                            activeDefinitions.forEach { def ->
                                val label = (if (fa) def.labelFa else def.labelEn) + if (def.required) " *" else ""
                                val value = state.attributes[def.id].orEmpty()
                                val missing = def.required && value.isBlank() && err(ProductError.ATTRIBUTE_REQUIRED)
                                fun set(v: String) = vm.updateEditor { it.copy(attributes = it.attributes + (def.id to v)) }
                                when (def.type) {
                                    AttributeType.NUMBER -> NumberField(value, ::set, label, error = missing || err(ProductError.ATTRIBUTE_INVALID) && value.isNotBlank() && Numbers.parseDecimal(value) == null)
                                    AttributeType.BOOLEAN -> Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(label, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                                        Switch(checked = value == "true", onCheckedChange = { set(it.toString()) }, colors = retailSwitchColors())
                                    }
                                    AttributeType.CHOICE -> DropdownField(
                                        label = label,
                                        options = listOf("" to "—") + def.options.orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }.map { it to it },
                                        selected = value,
                                        onSelect = ::set,
                                        isError = missing,
                                    )
                                    else -> RetailTextField(value = value, onValueChange = ::set, label = { Text(label) }, isError = missing)
                                }
                            }
                        }
                    }
                }
            }
            item {
                ExpandableSection(stringResource(R.string.more_details), initiallyExpanded = state.brand.isNotBlank() || state.supplier.isNotBlank() || state.tags.isNotBlank()) {
                    RetailTextField(value = state.brand, onValueChange = { v -> vm.updateEditor { it.copy(brand = v) } }, label = { Text(stringResource(R.string.brand)) })
                    RetailTextField(value = state.supplier, onValueChange = { v -> vm.updateEditor { it.copy(supplier = v) } }, label = { Text(stringResource(R.string.supplier)) })
                    RetailTextField(
                        value = state.tags,
                        onValueChange = { v -> vm.updateEditor { it.copy(tags = v) } },
                        label = { Text(stringResource(R.string.tags)) },
                        supportingText = stringResource(R.string.tags_help),
                    )
                    RetailTextField(value = state.description, onValueChange = { v -> vm.updateEditor { it.copy(description = v) } }, label = { Text(stringResource(R.string.description)) }, singleLine = false, maxLines = 4)
                    RetailTextField(value = state.notes, onValueChange = { v -> vm.updateEditor { it.copy(notes = v) } }, label = { Text(stringResource(R.string.notes)) }, singleLine = false, maxLines = 4)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.status_archived), modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                        Switch(checked = state.archived, onCheckedChange = { v -> vm.updateEditor { it.copy(archived = v) } }, colors = retailSwitchColors())
                    }
                }
            }
            if (state.errors.isNotEmpty() || state.invalidNumbers.isNotEmpty()) {
                item {
                    InfoBanner(
                        (state.errors.map { productErrorText(it) } + if (state.invalidNumbers.isNotEmpty()) listOf(invalidNumber) else emptyList()).joinToString("\n"),
                        Tone.DANGER,
                    )
                }
            }
            item {
                RetailPrimaryButton(onClick = { vm.save(onSaved) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text(stringResource(R.string.save))
                }
                TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(48.dp))
            }
        }
    }
}

@Composable
private fun modeHelp(mode: String): String = stringResource(
    when (mode) {
        PricingModes.FORMULA -> R.string.mode_formula_help
        PricingModes.MARKUP -> R.string.mode_markup_help
        PricingModes.MARGIN -> R.string.mode_margin_help
        PricingModes.FIXED_PROFIT -> R.string.mode_fixed_profit_help
        PricingModes.MANUAL -> R.string.mode_manual_help
        else -> R.string.mode_auto_help
    },
)

@Composable
fun productErrorText(e: ProductError): String = stringResource(
    when (e) {
        ProductError.TITLE_REQUIRED -> R.string.error_title_required
        ProductError.UNIT_REQUIRED -> R.string.error_unit_required
        ProductError.NEGATIVE_VALUE -> R.string.error_negative
        ProductError.VALUE_TOO_LARGE -> R.string.error_too_large
        ProductError.PIECES_INVALID -> R.string.error_pieces
        ProductError.BARCODE_DUPLICATE -> R.string.error_barcode_duplicate
        ProductError.SKU_DUPLICATE -> R.string.error_sku_duplicate
        ProductError.PRICING_VALUE_REQUIRED -> R.string.error_pricing_value
        ProductError.MARGIN_INVALID -> R.string.error_margin_range
        ProductError.FORMULA_REQUIRED -> R.string.error_formula_required
        ProductError.MANUAL_PRICE_REQUIRED -> R.string.error_manual_price
        ProductError.MIN_ABOVE_MAX -> R.string.error_min_above_max
        ProductError.ATTRIBUTE_REQUIRED -> R.string.error_attribute_required
        ProductError.ATTRIBUTE_INVALID -> R.string.error_attribute_invalid
    },
)
