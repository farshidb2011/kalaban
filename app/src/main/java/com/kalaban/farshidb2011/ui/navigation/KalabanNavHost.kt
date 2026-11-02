package com.kalaban.farshidb2011.ui.navigation

import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kalaban.farshidb2011.R
import com.kalaban.farshidb2011.core.Money
import com.kalaban.farshidb2011.data.repository.BulkOperation
import com.kalaban.farshidb2011.data.repository.BulkRequest
import com.kalaban.farshidb2011.domain.Permission
import com.kalaban.farshidb2011.ui.AdminViewModel
import com.kalaban.farshidb2011.ui.AppLocaleProvider
import com.kalaban.farshidb2011.ui.AppViewModel
import com.kalaban.farshidb2011.ui.ImportViewModel
import com.kalaban.farshidb2011.ui.PricingViewModel
import com.kalaban.farshidb2011.ui.ProductsViewModel
import com.kalaban.farshidb2011.ui.ReportsViewModel
import com.kalaban.farshidb2011.ui.UiMessages
import com.kalaban.farshidb2011.data.repository.AnalysisFlag
import com.kalaban.farshidb2011.ui.components.RetailBottomTab
import com.kalaban.farshidb2011.ui.screens.AboutScreen
import com.kalaban.farshidb2011.ui.screens.AttributesScreen
import com.kalaban.farshidb2011.ui.screens.AuditLogScreen
import com.kalaban.farshidb2011.ui.screens.BackupScreen
import com.kalaban.farshidb2011.ui.screens.BulkPriceScreen
import com.kalaban.farshidb2011.ui.screens.CreditsScreen
import com.kalaban.farshidb2011.ui.screens.DashboardScreen
import com.kalaban.farshidb2011.ui.screens.DiscountRulesScreen
import com.kalaban.farshidb2011.ui.screens.DisplayPanel
import com.kalaban.farshidb2011.ui.screens.DollarHistoryScreen
import com.kalaban.farshidb2011.ui.screens.FormulaEditorScreen
import com.kalaban.farshidb2011.ui.screens.FormulasScreen
import com.kalaban.farshidb2011.ui.screens.GeneralSettingsScreen
import com.kalaban.farshidb2011.ui.screens.HelpArticleScreen
import com.kalaban.farshidb2011.ui.screens.HelpCenterScreen
import com.kalaban.farshidb2011.ui.screens.ImportScreen
import com.kalaban.farshidb2011.ui.screens.LockScreen
import com.kalaban.farshidb2011.ui.screens.LowStockScreen
import com.kalaban.farshidb2011.ui.screens.ManagementScreen
import com.kalaban.farshidb2011.ui.screens.NewSaleScreen
import com.kalaban.farshidb2011.ui.screens.NotificationSettingsScreen
import com.kalaban.farshidb2011.ui.screens.PriceBatchScreen
import com.kalaban.farshidb2011.ui.screens.PriceBatchesScreen
import com.kalaban.farshidb2011.ui.screens.PricingRulesScreen
import com.kalaban.farshidb2011.ui.screens.ProductDetailScreen
import com.kalaban.farshidb2011.ui.screens.ProductEditorScreen
import com.kalaban.farshidb2011.ui.screens.ProductsScreen
import com.kalaban.farshidb2011.ui.screens.RemindersScreen
import com.kalaban.farshidb2011.ui.screens.ReportsHubScreen
import com.kalaban.farshidb2011.ui.screens.RolesScreen
import com.kalaban.farshidb2011.ui.screens.SalesScreen
import com.kalaban.farshidb2011.ui.screens.SecuritySettingsScreen
import com.kalaban.farshidb2011.ui.screens.SimpleNameListScreen
import com.kalaban.farshidb2011.ui.screens.UsersScreen
import com.kalaban.farshidb2011.ui.theme.KalabanTheme

private data class BottomTab(val route: String, val labelRes: Int, val icon: ImageVector, val visible: Boolean)

/** Changes when money formatting changes, forcing every amount on screen to re-render. */
private val LocalMoneyFormat = staticCompositionLocalOf { Money.scale to Money.grouping }

@Composable
fun KalabanNavHost(vm: AppViewModel = viewModel(factory = AppViewModel.factory())) {
    val products: ProductsViewModel = viewModel(factory = ProductsViewModel.factory())
    val pricing: PricingViewModel = viewModel(factory = PricingViewModel.factory())
    val admin: AdminViewModel = viewModel(factory = AdminViewModel.factory())
    val reports: ReportsViewModel = viewModel(factory = ReportsViewModel.factory())
    val importer: ImportViewModel = viewModel(factory = ImportViewModel.factory())

    val rootNav = rememberNavController()
    val unlocked by vm.unlocked.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val config by vm.config.collectAsStateWithLifecycle()
    val themeModePref by vm.themeMode.collectAsStateWithLifecycle()
    val units by vm.units.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val language = settings.languageTag.ifBlank { "fa" }
    var pendingAfterUnlock by remember { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }
    var reportsTab by rememberSaveable { mutableStateOf(0) }

    val effectiveThemeMode = themeModePref ?: if (settings.displayDarkMode) "dark" else "light"

    // A sign-out (manual, deactivated user, process death) always returns to the customer display.
    LaunchedEffect(unlocked) {
        if (!unlocked) {
            val current = rootNav.currentDestination?.route
            if (current != null && current != Routes.DISPLAY && current != Routes.LOCK) {
                rootNav.navigate(Routes.DISPLAY) {
                    popUpTo(Routes.DISPLAY) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    fun openLocked(route: String) {
        if (unlocked) {
            rootNav.navigate(route)
        } else {
            pendingAfterUnlock = route
            rootNav.navigate(Routes.LOCK)
        }
    }

    fun back() {
        rootNav.popBackStack()
    }

    fun openBulkRecalculation() {
        pricing.prepareBulk(BulkRequest(operation = BulkOperation.Recalculate))
        rootNav.navigate(Routes.BULK_PRICE)
    }

    AppLocaleProvider(languageTag = language) {
        KalabanTheme(themeMode = effectiveThemeMode, dynamicColor = false) {
            val resources = LocalResources.current
            LaunchedEffect(Unit) {
                UiMessages.messages.collect { message ->
                    snackbar.showSnackbar(resources.getString(message.res, *message.args.toTypedArray()))
                }
            }
            CompositionLocalProvider(LocalMoneyFormat provides (config.moneyDecimals to config.grouping)) {
                Box(Modifier.fillMaxSize()) {
                    Surface(
                        color = MaterialTheme.colorScheme.background,
                        contentColor = MaterialTheme.colorScheme.onBackground,
                    ) {
                        NavHost(navController = rootNav, startDestination = Routes.DISPLAY) {
                            composable(Routes.LOCK) {
                                LockScreen(
                                    vm = vm,
                                    onBack = ::back,
                                    onUnlocked = {
                                        val next = pendingAfterUnlock ?: Routes.MAIN
                                        pendingAfterUnlock = null
                                        rootNav.navigate(next) { popUpTo(Routes.LOCK) { inclusive = true } }
                                    },
                                )
                            }
                            composable(Routes.DISPLAY) {
                                DisplayPanel(vm = vm, onOpenAdmin = { openLocked(Routes.MAIN) })
                            }
                            composable(Routes.MAIN) {
                                RetailMainShell(
                                    vm = vm,
                                    products = products,
                                    pricing = pricing,
                                    admin = admin,
                                    reports = reports,
                                    reportsTab = reportsTab,
                                    onReportsTab = { reportsTab = it },
                                    rootNav = rootNav,
                                    onOpenDisplay = {
                                        rootNav.navigate(Routes.DISPLAY) {
                                            popUpTo(Routes.DISPLAY) { inclusive = false }
                                            launchSingleTop = true
                                        }
                                    },
                                    onSignOut = { vm.lockSession() },
                                )
                            }
                            composable(Routes.SETTINGS) { PricingRulesScreen(vm, admin, ::back, ::openBulkRecalculation) }
                            composable(Routes.GENERAL_SETTINGS) { GeneralSettingsScreen(vm, admin, ::back) }
                            composable(Routes.SECURITY) { SecuritySettingsScreen(vm, admin, ::back) { rootNav.navigate(it) } }
                            composable(Routes.NOTIFICATIONS) { NotificationSettingsScreen(admin, ::back) }
                            composable(Routes.USERS) { UsersScreen(admin, settings, ::back) }
                            composable(Routes.ROLES) { RolesScreen(admin, settings, ::back) }
                            composable(Routes.AUDIT) { AuditLogScreen(admin, vm, ::back) }
                            composable(Routes.ATTRIBUTES) { AttributesScreen(admin, settings, ::back) }
                            composable(Routes.UNITS) {
                                val fa = language.startsWith("fa")
                                var deleteErr by remember { mutableStateOf<String?>(null) }
                                val inUseMsg = stringResource(R.string.unit_in_use)
                                SimpleNameListScreen(
                                    titleRes = R.string.units,
                                    names = units.map { it.id to if (fa) it.nameFa else it.nameEn },
                                    labelRes = R.string.unit_name,
                                    externalError = deleteErr,
                                    onClearExternalError = { deleteErr = null },
                                    onBack = ::back,
                                    onAdd = { vm.saveUnit(nameFa = it, nameEn = it) },
                                    onUpdate = { id, name ->
                                        val existing = units.firstOrNull { it.id == id }
                                        if (fa) {
                                            vm.saveUnit(id = id, nameFa = name, nameEn = existing?.nameEn?.ifBlank { name } ?: name)
                                        } else {
                                            vm.saveUnit(id = id, nameFa = existing?.nameFa?.ifBlank { name } ?: name, nameEn = name)
                                        }
                                    },
                                    onDelete = { id ->
                                        deleteErr = null
                                        vm.deleteUnit(id) { deleteErr = inUseMsg }
                                    },
                                )
                            }
                            composable(Routes.CATEGORIES) {
                                val fa = language.startsWith("fa")
                                var deleteErr by remember { mutableStateOf<String?>(null) }
                                val inUseMsg = stringResource(R.string.category_in_use)
                                SimpleNameListScreen(
                                    titleRes = R.string.categories,
                                    names = categories.map { it.id to if (fa) it.nameFa else it.nameEn },
                                    labelRes = R.string.category_name,
                                    externalError = deleteErr,
                                    onClearExternalError = { deleteErr = null },
                                    onBack = ::back,
                                    onAdd = { vm.saveCategory(nameFa = it, nameEn = it) },
                                    onUpdate = { id, name ->
                                        val existing = categories.firstOrNull { it.id == id }
                                        if (fa) {
                                            vm.saveCategory(id = id, nameFa = name, nameEn = existing?.nameEn?.ifBlank { name } ?: name)
                                        } else {
                                            vm.saveCategory(id = id, nameFa = existing?.nameFa?.ifBlank { name } ?: name, nameEn = name)
                                        }
                                    },
                                    onDelete = { id ->
                                        deleteErr = null
                                        vm.deleteCategory(id) { deleteErr = inUseMsg }
                                    },
                                )
                            }
                            composable(Routes.NEW_SALE) { NewSaleScreen(vm, ::back) }
                            composable(Routes.CREDITS) { CreditsScreen(vm, ::back) }
                            composable(Routes.LOW_STOCK) { LowStockScreen(vm, ::back) }
                            composable(Routes.REMINDERS) { RemindersScreen(vm, ::back) }
                            composable(Routes.DOLLAR_HISTORY) { DollarHistoryScreen(vm, ::back) }
                            composable(Routes.BACKUP) { BackupScreen(vm, ::back) { rootNav.navigate(Routes.IMPORT) } }
                            composable(Routes.IMPORT) { ImportScreen(importer, vm, ::back) { rootNav.navigate(Routes.PRICE_BATCHES) } }
                            composable(Routes.PRODUCT_DETAIL, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                                val id = entry.arguments?.getLong("id") ?: 0L
                                ProductDetailScreen(
                                    id = id,
                                    vm = products,
                                    appVm = vm,
                                    pricingVm = pricing,
                                    onBack = ::back,
                                    onEdit = { rootNav.navigate(Routes.productEdit(it)) },
                                    onOpen = { rootNav.navigate(Routes.productDetail(it)) },
                                    onBulkPrice = { rootNav.navigate(Routes.BULK_PRICE) },
                                )
                            }
                            composable(Routes.PRODUCT_EDIT, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                                val id = entry.arguments?.getLong("id") ?: 0L
                                ProductEditorScreen(
                                    id = id,
                                    vm = products,
                                    appVm = vm,
                                    onBack = ::back,
                                    onSaved = { savedId ->
                                        rootNav.popBackStack()
                                        if (id == 0L) rootNav.navigate(Routes.productDetail(savedId))
                                    },
                                )
                            }
                            composable(Routes.FORMULAS) {
                                FormulasScreen(pricing, vm, ::back, { rootNav.navigate(Routes.formulaEdit(it)) }, ::openBulkRecalculation)
                            }
                            composable(Routes.FORMULA_EDIT, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                                FormulaEditorScreen(entry.arguments?.getLong("id") ?: 0L, pricing, vm, ::back)
                            }
                            composable(Routes.BULK_PRICE) { BulkPriceScreen(pricing, vm, ::back) { rootNav.navigate(Routes.PRICE_BATCHES) } }
                            composable(Routes.PRICE_BATCHES) { PriceBatchesScreen(pricing, vm, ::back) { rootNav.navigate(Routes.priceBatch(it)) } }
                            composable(Routes.PRICE_BATCH, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                                PriceBatchScreen(entry.arguments?.getLong("id") ?: 0L, pricing, vm, ::back)
                            }
                            composable(Routes.DISCOUNTS) { DiscountRulesScreen(pricing, vm, ::back) }
                            composable(Routes.HELP) { HelpCenterScreen(vm, ::back) { rootNav.navigate(Routes.helpArticle(it)) } }
                            composable(Routes.HELP_ARTICLE) { entry -> HelpArticleScreen(entry.arguments?.getString("id").orEmpty(), vm, ::back) }
                            composable(Routes.ABOUT) { AboutScreen(::back) }
                        }
                    }
                    SnackbarHost(
                        hostState = snackbar,
                        modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 72.dp),
                    ) { data ->
                        val scheme = MaterialTheme.colorScheme
                        // Explicit inverse pair so dark theme never shows pale text on a white plate.
                        CompositionLocalProvider(LocalContentColor provides scheme.inverseOnSurface) {
                            Snackbar(
                                snackbarData = data,
                                containerColor = scheme.inverseSurface,
                                contentColor = scheme.inverseOnSurface,
                                actionColor = scheme.inversePrimary,
                                dismissActionContentColor = scheme.inverseOnSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RetailMainShell(
    vm: AppViewModel,
    products: ProductsViewModel,
    pricing: PricingViewModel,
    admin: AdminViewModel,
    reports: ReportsViewModel,
    reportsTab: Int,
    onReportsTab: (Int) -> Unit,
    rootNav: NavHostController,
    onOpenDisplay: () -> Unit,
    onSignOut: () -> Unit,
) {
    val tabNav = rememberNavController()
    val user by vm.sessionUser.collectAsStateWithLifecycle()
    val tabs = remember(user) {
        listOf(
            BottomTab(Routes.DASHBOARD, R.string.dashboard, Icons.Outlined.Dashboard, true),
            BottomTab(Routes.ITEMS, R.string.items, Icons.Outlined.Inventory2, vm.can(Permission.VIEW_PRODUCTS)),
            BottomTab(Routes.SALES, R.string.sales, Icons.Outlined.ShoppingCart, vm.can(Permission.RECORD_SALES) || vm.can(Permission.MANAGE_CREDITS)),
            BottomTab(Routes.REPORTS, R.string.reports, Icons.Outlined.BarChart, vm.can(Permission.VIEW_REPORTS)),
            BottomTab(Routes.MANAGE, R.string.manage, Icons.Outlined.Tune, true),
        ).filter { it.visible }
    }
    val backStack by tabNav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: Routes.DASHBOARD
    val titleRes = tabs.firstOrNull { it.route == currentRoute }?.labelRes ?: R.string.dashboard

    fun selectTab(route: String) {
        tabNav.navigate(route) {
            popUpTo(tabNav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val activityResultOwner = checkNotNull(LocalActivityResultRegistryOwner.current) {
        "LocalActivityResultRegistryOwner missing; MainActivity must provide it"
    }
    CompositionLocalProvider(LocalActivityResultRegistryOwner provides activityResultOwner) {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text(stringResource(titleRes)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                actions = {
                    IconButton(onClick = onOpenDisplay) {
                        Icon(Icons.Outlined.Storefront, contentDescription = stringResource(R.string.back_to_display))
                    }
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = stringResource(R.string.sign_out))
                    }
                },
            )
            // Isolate body from TopAppBar's onPrimary LocalContentColor (white in light theme).
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
                NavHost(navController = tabNav, startDestination = Routes.DASHBOARD, modifier = Modifier.weight(1f)) {
                    composable(Routes.DASHBOARD) {
                        DashboardScreen(
                            appVm = vm,
                            reportsVm = reports,
                            pricingVm = pricing,
                            onNavigate = { rootNav.navigate(it) },
                            onOpenProducts = { lowStock ->
                                products.setFilter { it.copy(lowStockOnly = lowStock) }
                                selectTab(Routes.ITEMS)
                            },
                            onOpenAnalysis = {
                                reports.setAnalysisFilter { it.copy(flags = setOf(AnalysisFlag.NEGATIVE_PROFIT, AnalysisFlag.LOW_MARGIN)) }
                                onReportsTab(1)
                                selectTab(Routes.REPORTS)
                            },
                        )
                    }
                    composable(Routes.ITEMS) {
                        ProductsScreen(
                            vm = products,
                            appVm = vm,
                            pricingVm = pricing,
                            onOpen = { rootNav.navigate(Routes.productDetail(it)) },
                            onNew = { rootNav.navigate(Routes.productEdit(0)) },
                            onBulkPrice = { rootNav.navigate(Routes.BULK_PRICE) },
                        )
                    }
                    composable(Routes.SALES) {
                        SalesScreen(vm = vm, onBack = null, onNew = { rootNav.navigate(Routes.NEW_SALE) }, embedded = true)
                    }
                    composable(Routes.REPORTS) {
                        ReportsHubScreen(vm, reports, admin, reportsTab) { rootNav.navigate(Routes.productDetail(it)) }
                    }
                    composable(Routes.MANAGE) {
                        ManagementScreen(vm, onNavigate = { rootNav.navigate(it) }, onSignOut = onSignOut)
                    }
                }
            }
            Surface(
                tonalElevation = 0.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 4.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    tabs.forEach { tab ->
                        RetailBottomTab(
                            selected = currentRoute == tab.route,
                            label = stringResource(tab.labelRes),
                            icon = tab.icon,
                            onClick = { selectTab(tab.route) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}
