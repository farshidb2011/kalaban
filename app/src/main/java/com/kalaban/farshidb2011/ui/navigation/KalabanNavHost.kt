package com.kalaban.farshidb2011.ui.navigation

import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kalaban.farshidb2011.R
import com.kalaban.farshidb2011.ui.AppLocaleProvider
import com.kalaban.farshidb2011.ui.AppViewModel
import com.kalaban.farshidb2011.ui.components.RetailBottomTab
import com.kalaban.farshidb2011.ui.screens.AdminHub
import com.kalaban.farshidb2011.ui.screens.BackupScreen
import com.kalaban.farshidb2011.ui.screens.CreditsScreen
import com.kalaban.farshidb2011.ui.screens.DisplayPanel
import com.kalaban.farshidb2011.ui.screens.DollarHistoryScreen
import com.kalaban.farshidb2011.ui.screens.ItemsScreen
import com.kalaban.farshidb2011.ui.screens.LockScreen
import com.kalaban.farshidb2011.ui.screens.LowStockScreen
import com.kalaban.farshidb2011.ui.screens.NewSaleScreen
import com.kalaban.farshidb2011.ui.screens.RemindersScreen
import com.kalaban.farshidb2011.ui.screens.ReportsScreen
import com.kalaban.farshidb2011.ui.screens.SalesScreen
import com.kalaban.farshidb2011.ui.screens.SettingsScreen
import com.kalaban.farshidb2011.ui.screens.SimpleNameListScreen
import com.kalaban.farshidb2011.ui.theme.KalabanTheme

private data class BottomTab(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
)

@Composable
fun KalabanNavHost(vm: AppViewModel = viewModel(factory = AppViewModel.factory())) {
    val rootNav = rememberNavController()
    val unlocked by vm.unlocked.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val units by vm.units.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val language = settings.languageTag.ifBlank { "fa" }
    var pendingAfterUnlock by remember { mutableStateOf<String?>(null) }

    fun openLocked(route: String) {
        if (unlocked) {
            rootNav.navigate(route)
        } else {
            pendingAfterUnlock = route
            rootNav.navigate(Routes.LOCK)
        }
    }

    AppLocaleProvider(languageTag = language) {
        KalabanTheme(darkTheme = settings.displayDarkMode, dynamicColor = false) {
            Surface(
                color = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onBackground,
            ) {
                NavHost(
                    navController = rootNav,
                    startDestination = Routes.DISPLAY,
                ) {
                    composable(Routes.LOCK) {
                        LockScreen(vm) {
                            val next = pendingAfterUnlock ?: Routes.MAIN
                            pendingAfterUnlock = null
                            rootNav.navigate(next) {
                                popUpTo(Routes.LOCK) { inclusive = true }
                            }
                        }
                    }
                    composable(Routes.DISPLAY) {
                        DisplayPanel(
                            vm = vm,
                            onOpenAdmin = { openLocked(Routes.MAIN) },
                        )
                    }
                    composable(Routes.MAIN) {
                        RetailMainShell(
                            vm = vm,
                            onOpenDisplay = {
                                rootNav.navigate(Routes.DISPLAY) {
                                    popUpTo(Routes.DISPLAY) { inclusive = false }
                                    launchSingleTop = true
                                }
                            },
                            onOpenLocked = { openLocked(it) },
                            onOpen = { rootNav.navigate(it) },
                        )
                    }
                    composable(Routes.SETTINGS) {
                        SettingsScreen(vm) { rootNav.popBackStack() }
                    }
                    composable(Routes.ADMIN_MENU) {
                        AdminHub(
                            onBack = { rootNav.popBackStack() },
                            onNavigate = { route -> rootNav.navigate(route) },
                            onLock = {
                                vm.lockSession()
                                rootNav.navigate(Routes.DISPLAY) {
                                    popUpTo(Routes.DISPLAY) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                        )
                    }
                    composable(Routes.UNITS) {
                        val fa = language.startsWith("fa")
                        SimpleNameListScreen(
                            titleRes = R.string.units,
                            names = units.map {
                                it.id to if (fa) it.nameFa else it.nameEn
                            },
                            labelRes = R.string.unit_name,
                            onBack = { rootNav.popBackStack() },
                            onAdd = { vm.saveUnit(nameFa = it, nameEn = it) },
                            onUpdate = { id, name ->
                                val existing = units.firstOrNull { it.id == id }
                                if (fa) {
                                    vm.saveUnit(
                                        id = id,
                                        nameFa = name,
                                        nameEn = existing?.nameEn?.ifBlank { name } ?: name,
                                    )
                                } else {
                                    vm.saveUnit(
                                        id = id,
                                        nameFa = existing?.nameFa?.ifBlank { name } ?: name,
                                        nameEn = name,
                                    )
                                }
                            },
                            onDelete = { id ->
                                vm.deleteUnit(id)
                            },
                        )
                    }
                    composable(Routes.CATEGORIES) {
                        val fa = language.startsWith("fa")
                        SimpleNameListScreen(
                            titleRes = R.string.categories,
                            names = categories.map {
                                it.id to if (fa) it.nameFa else it.nameEn
                            },
                            labelRes = R.string.category_name,
                            onBack = { rootNav.popBackStack() },
                            onAdd = { vm.saveCategory(nameFa = it, nameEn = it) },
                            onUpdate = { id, name ->
                                val existing = categories.firstOrNull { it.id == id }
                                if (fa) {
                                    vm.saveCategory(
                                        id = id,
                                        nameFa = name,
                                        nameEn = existing?.nameEn?.ifBlank { name } ?: name,
                                    )
                                } else {
                                    vm.saveCategory(
                                        id = id,
                                        nameFa = existing?.nameFa?.ifBlank { name } ?: name,
                                        nameEn = name,
                                    )
                                }
                            },
                            onDelete = { id ->
                                vm.deleteCategory(id)
                            },
                        )
                    }
                    composable(Routes.NEW_SALE) { NewSaleScreen(vm) { rootNav.popBackStack() } }
                    composable(Routes.CREDITS) { CreditsScreen(vm) { rootNav.popBackStack() } }
                    composable(Routes.LOW_STOCK) { LowStockScreen(vm) { rootNav.popBackStack() } }
                    composable(Routes.REMINDERS) { RemindersScreen(vm) { rootNav.popBackStack() } }
                    composable(Routes.DOLLAR_HISTORY) { DollarHistoryScreen(vm) { rootNav.popBackStack() } }
                    composable(Routes.BACKUP) { BackupScreen(vm) { rootNav.popBackStack() } }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RetailMainShell(
    vm: AppViewModel,
    onOpenDisplay: () -> Unit,
    onOpenLocked: (String) -> Unit,
    onOpen: (String) -> Unit,
) {
    val tabNav = rememberNavController()
    val tabs = listOf(
        BottomTab(Routes.ITEMS, R.string.items, Icons.Outlined.Inventory2),
        BottomTab(Routes.SALES, R.string.sales, Icons.Outlined.ShoppingCart),
        BottomTab(Routes.REPORTS, R.string.reports, Icons.Outlined.BarChart),
    )
    val backStack by tabNav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: Routes.ITEMS
    var menuOpen by remember { mutableStateOf(false) }

    val titleRes = when (currentRoute) {
        Routes.SALES -> R.string.sales
        Routes.REPORTS -> R.string.reports
        else -> R.string.items
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
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Outlined.MoreHoriz, contentDescription = stringResource(R.string.admin_panel))
                    }
                    CompositionLocalProvider(
                        LocalContentColor provides MaterialTheme.colorScheme.onSurface,
                    ) {
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                            modifier = Modifier.widthIn(min = 280.dp),
                        ) {
                            val menuItems = listOf(
                                Triple(R.string.admin_panel, Icons.Outlined.AdminPanelSettings, Routes.ADMIN_MENU to true),
                                Triple(R.string.settings, Icons.Outlined.Settings, Routes.SETTINGS to true),
                                Triple(R.string.units, Icons.Outlined.Straighten, Routes.UNITS to true),
                                Triple(R.string.categories, Icons.Outlined.Category, Routes.CATEGORIES to true),
                                Triple(R.string.credits, Icons.Outlined.Payments, Routes.CREDITS to true),
                                Triple(R.string.backup, Icons.Outlined.Backup, Routes.BACKUP to true),
                                Triple(R.string.low_stock, Icons.Outlined.WarningAmber, Routes.LOW_STOCK to true),
                                Triple(R.string.reminders, Icons.Outlined.Notifications, Routes.REMINDERS to true),
                                Triple(R.string.dollar_history, Icons.Outlined.History, Routes.DOLLAR_HISTORY to true),
                            )
                            menuItems.forEachIndexed { index, (label, icon, routeLocked) ->
                                val (route, locked) = routeLocked
                                if (index == 2) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                }
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            stringResource(label),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                                    },
                                    modifier = Modifier.defaultMinSize(minHeight = 52.dp),
                                    onClick = {
                                        menuOpen = false
                                        if (locked) onOpenLocked(route) else onOpen(route)
                                    },
                                )
                            }
                        }
                    }
                },
            )
            NavHost(
                navController = tabNav,
                startDestination = Routes.ITEMS,
                modifier = Modifier.weight(1f),
            ) {
                composable(Routes.ITEMS) {
                    ItemsScreen(vm = vm, onBack = null, embedded = true)
                }
                composable(Routes.SALES) {
                    SalesScreen(
                        vm = vm,
                        onBack = null,
                        onNew = { onOpen(Routes.NEW_SALE) },
                        embedded = true,
                    )
                }
                composable(Routes.REPORTS) {
                    ReportsScreen(vm = vm, onBack = null, embedded = true)
                }
            }
            Surface(
                tonalElevation = 0.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    tabs.forEach { tab ->
                        RetailBottomTab(
                            selected = currentRoute == tab.route,
                            label = stringResource(tab.labelRes),
                            icon = tab.icon,
                            onClick = {
                                tabNav.navigate(tab.route) {
                                    popUpTo(tabNav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}
